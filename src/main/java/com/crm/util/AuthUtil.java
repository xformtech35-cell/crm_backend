package com.crm.util;

import com.crm.entity.User;
import com.crm.entity.TeamMember;
import com.crm.entity.Team;
import com.crm.entity.CreateTeam;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.UserRepository;
import com.crm.repository.RoleRepository;
import com.crm.repository.TeamMemberRepository;
import com.crm.repository.TeamRepository;
import com.crm.repository.CreateTeamRepository;
import com.crm.repository.DataScopeConfigRepository;
import com.crm.entity.DataScopeConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthUtil {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final CreateTeamRepository createTeamRepository;
    private final DataScopeConfigRepository dataScopeConfigRepository;
    private final HttpServletRequest request;

    public User getCurrentUser(Authentication auth) {
        if (auth == null) {
            throw new org.springframework.security.access.AccessDeniedException("Access Denied: User is not authenticated");
        }
        User user = userRepository.findByUserEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", auth.getName()));

        if (isSuperAdmin(user.getRole())) {
            String companyIdStr = request.getHeader("X-Company-Id");
            if (companyIdStr != null && !companyIdStr.trim().isEmpty()) {
                try {
                    Long companyId = Long.parseLong(companyIdStr.trim());
                    return userRepository.findById(companyId)
                            .orElse(user);
                } catch (Exception e) {
                    // Fallback to standard super admin user if parsing/finding fails
                }
            }
        }
        return user;
    }

    public Long getCompanyAdminId(User user) {
        if (user == null) return null;
        String companyIdStr = request.getHeader("X-Company-Id");
        if (companyIdStr != null && !companyIdStr.trim().isEmpty()) {
            try {
                return Long.parseLong(companyIdStr.trim());
            } catch (Exception ignored) {}
        }
        if (isSuperAdmin(user.getRole())) {
            return null;
        }
        if (isAdmin(user.getRole())) {
            return user.getUserid();
        }
        return teamMemberRepository.findByTeamMemberEmail(user.getUserEmail())
                .map(TeamMember::getUserIdFk)
                .orElse(user.getUserid());
    }

    public Long getSelectedTeamMemberId() {
        String header = request.getHeader("X-Team-Member-Id");
        if (header != null && !header.trim().isEmpty()) {
            try {
                return Long.parseLong(header.trim());
            } catch (Exception e) {
                // ignore
            }
        }
        return null;
    }

    public String resolveRoleName(String roleField) {

        if (roleField == null) return "";
        if (roleField.equalsIgnoreCase("SUPER_ADMIN") || roleField.equalsIgnoreCase("SUPER ADMIN")) {
            return "SUPER_ADMIN";
        }
        if (roleField.equalsIgnoreCase("ADMIN")) {
            return "ADMIN";
        }
        if (roleField.equalsIgnoreCase("TEAM_LEAD") || roleField.equalsIgnoreCase("TEAM LEAD") || roleField.equalsIgnoreCase("Team Lead")) {
            return "TEAM_LEAD";
        }
        try {
            Long roleId = Long.parseLong(roleField);
            return roleRepository.findById(roleId)
                    .map(r -> {
                        String name = r.getRoleName().toUpperCase();
                        if (name.equals("TEAM LEAD") || name.equals("TEAM_LEAD") || name.equals("TEAM LEADER")) {
                            return "TEAM_LEAD";
                        }
                        return name;
                    })
                    .orElse(roleField.toUpperCase());
        } catch (NumberFormatException e) {
            return roleField.toUpperCase();
        }
    }

    public boolean isSuperAdmin(String role) {
        String roleName = resolveRoleName(role);
        return "SUPER_ADMIN".equals(roleName) || "SUPER ADMIN".equals(roleName);
    }

    public boolean isAdmin(String role) {
        String roleName = resolveRoleName(role);
        return "ADMIN".equals(roleName);
    }

    public boolean isTeamLead(String role) {
        String roleName = resolveRoleName(role);
        return "TEAM_LEAD".equals(roleName) || "TEAM LEAD".equals(roleName);
    }

    public boolean isAnyAdmin(String role) {
        return isSuperAdmin(role) || isAdmin(role);
    }

    public List<Team> getLedTeamsForUser(User user) {
        List<Team> ledTeams = new ArrayList<>();
        if (user == null) return ledTeams;

        Optional<TeamMember> selfTm = teamMemberRepository.findByTeamMemberEmail(user.getUserEmail());
        Long selfTmId = selfTm.map(TeamMember::getTeamMemberId).orElse(null);

        if (selfTmId != null) {
            ledTeams.addAll(teamRepository.findByTeamLeadId(selfTmId));
        }
        if (user.getUserid() != null) {
            List<Team> teamsByUserId = teamRepository.findByTeamLeadId(user.getUserid());
            for (Team t : teamsByUserId) {
                if (!ledTeams.contains(t)) ledTeams.add(t);
            }
        }

        if (selfTmId != null) {
            List<CreateTeam> memberAssignments = createTeamRepository.findByTeamMemberIdFk(selfTmId);
            for (CreateTeam ct : memberAssignments) {
                if (ct.getTeamIdFk() != null) {
                    teamRepository.findById(ct.getTeamIdFk()).ifPresent(t -> {
                        if (!ledTeams.contains(t)) {
                            ledTeams.add(t);
                        }
                    });
                }
            }
        }
        return ledTeams;
    }

    public List<Long> getTeamLeadTeamIds(User user) {
        List<Long> teamIds = new ArrayList<>();
        List<Team> ledTeams = getLedTeamsForUser(user);
        for (Team t : ledTeams) {
            if (t.getTeamId() != null && !teamIds.contains(t.getTeamId())) {
                teamIds.add(t.getTeamId());
            }
        }
        return teamIds;
    }

    public List<Long> getTeamLeadMemberUserIds(User user) {
        List<Long> result = new ArrayList<>();
        if (user == null) return result;
        if (user.getUserid() != null) result.add(user.getUserid());

        Optional<TeamMember> selfTm = teamMemberRepository.findByTeamMemberEmail(user.getUserEmail());
        if (selfTm.isPresent() && selfTm.get().getTeamMemberId() != null) {
            if (!result.contains(selfTm.get().getTeamMemberId())) {
                result.add(selfTm.get().getTeamMemberId());
            }
        }

        List<Team> ledTeams = getLedTeamsForUser(user);
        for (Team team : ledTeams) {
            if (team.getTeamLeadId() != null) {
                if (!result.contains(team.getTeamLeadId())) {
                    result.add(team.getTeamLeadId());
                }
                teamMemberRepository.findById(team.getTeamLeadId()).ifPresent(tm -> {
                    if (tm.getTeamMemberEmail() != null && !tm.getTeamMemberEmail().isBlank()) {
                        userRepository.findByUserEmail(tm.getTeamMemberEmail()).ifPresent(u -> {
                            if (u.getUserid() != null && !result.contains(u.getUserid())) {
                                result.add(u.getUserid());
                            }
                        });
                    }
                });
                userRepository.findById(team.getTeamLeadId()).ifPresent(u -> {
                    if (u.getUserid() != null && !result.contains(u.getUserid())) {
                        result.add(u.getUserid());
                    }
                });
            }

            List<CreateTeam> assignments = createTeamRepository.findByTeamIdFk(team.getTeamId());
            for (CreateTeam ct : assignments) {
                if (ct.getTeamMemberIdFk() != null) {
                    if (!result.contains(ct.getTeamMemberIdFk())) {
                        result.add(ct.getTeamMemberIdFk());
                    }
                    teamMemberRepository.findById(ct.getTeamMemberIdFk()).ifPresent(tm -> {
                        if (tm.getTeamMemberEmail() != null && !tm.getTeamMemberEmail().isBlank()) {
                            userRepository.findByUserEmail(tm.getTeamMemberEmail()).ifPresent(u -> {
                                if (u.getUserid() != null && !result.contains(u.getUserid())) {
                                    result.add(u.getUserid());
                                }
                            });
                        }
                    });
                }
            }
        }
        return result;
    }

    public List<String> getTeamLeadMemberEmails(User user) {
        List<String> emails = new ArrayList<>();
        if (user == null) return emails;
        if (user.getUserEmail() != null && !user.getUserEmail().isBlank()) {
            emails.add(user.getUserEmail().trim().toLowerCase());
        }

        List<Team> ledTeams = getLedTeamsForUser(user);
        for (Team team : ledTeams) {
            if (team.getTeamLeadId() != null) {
                teamMemberRepository.findById(team.getTeamLeadId()).ifPresent(tm -> {
                    if (tm.getTeamMemberEmail() != null && !tm.getTeamMemberEmail().isBlank()) {
                        String email = tm.getTeamMemberEmail().trim().toLowerCase();
                        if (!emails.contains(email)) emails.add(email);
                    }
                });
                userRepository.findById(team.getTeamLeadId()).ifPresent(u -> {
                    if (u.getUserEmail() != null && !u.getUserEmail().isBlank()) {
                        String email = u.getUserEmail().trim().toLowerCase();
                        if (!emails.contains(email)) emails.add(email);
                    }
                });
            }

            List<CreateTeam> assignments = createTeamRepository.findByTeamIdFk(team.getTeamId());
            for (CreateTeam ct : assignments) {
                if (ct.getTeamMemberIdFk() != null) {
                    teamMemberRepository.findById(ct.getTeamMemberIdFk()).ifPresent(tm -> {
                        if (tm.getTeamMemberEmail() != null && !tm.getTeamMemberEmail().isBlank()) {
                            String email = tm.getTeamMemberEmail().trim().toLowerCase();
                            if (!emails.contains(email)) {
                                emails.add(email);
                            }
                        }
                    });
                }
            }
        }
        return emails;
    }

    public String resolveDataScopeMode(User user, String moduleName) {
        if (user == null) return "OWN_DATA_ONLY";
        // Super Admin always has full access — unconditional
        if (isSuperAdmin(user.getRole())) return "ALL_DATA";

        String normalizedModule = moduleName.toUpperCase();
        Long companyAdminId = getCompanyAdminId(user);

        // 1. Check User-specific override
        if (companyAdminId != null) {
            Optional<DataScopeConfig> userConfig = dataScopeConfigRepository.findByCompanyAdminIdFkAndUserIdFkAndModuleName(companyAdminId, user.getUserid(), normalizedModule);
            if (userConfig.isPresent()) {
                return userConfig.get().getScopeMode();
            }
        } else {
            Optional<DataScopeConfig> userConfig = dataScopeConfigRepository.findByUserIdFkAndModuleName(user.getUserid(), normalizedModule);
            if (userConfig.isPresent()) {
                return userConfig.get().getScopeMode();
            }
        }

        // 2. Check Role-specific configuration
        String roleStr = user.getRole();
        if (roleStr != null) {
            try {
                Long roleId = Long.parseLong(roleStr);
                Optional<DataScopeConfig> roleConfig = companyAdminId != null
                        ? dataScopeConfigRepository.findByCompanyAdminIdFkAndRoleIdFkAndModuleName(companyAdminId, roleId, normalizedModule)
                        : dataScopeConfigRepository.findByRoleIdFkAndModuleName(roleId, normalizedModule);
                if (roleConfig.isPresent()) {
                    return roleConfig.get().getScopeMode();
                }
            } catch (NumberFormatException e) {
                // Find role by name within this company
                Optional<com.crm.entity.Role> roleObj = (companyAdminId != null ? roleRepository.findByUserIdFk(companyAdminId) : roleRepository.findAll())
                        .stream()
                        .filter(r -> r.getRoleName() != null && r.getRoleName().equalsIgnoreCase(roleStr))
                        .findFirst();
                if (roleObj.isPresent()) {
                    Long rId = roleObj.get().getRoleId();
                    Optional<DataScopeConfig> roleConfig = companyAdminId != null
                            ? dataScopeConfigRepository.findByCompanyAdminIdFkAndRoleIdFkAndModuleName(companyAdminId, rId, normalizedModule)
                            : dataScopeConfigRepository.findByRoleIdFkAndModuleName(rId, normalizedModule);
                    if (roleConfig.isPresent()) {
                        return roleConfig.get().getScopeMode();
                    }
                }
            }
        }

        // 2b. Check TeamMember.teamMemberRole if user.getRole() didn't match a config
        Optional<TeamMember> tm = teamMemberRepository.findByTeamMemberEmail(user.getUserEmail());
        if (tm.isPresent() && tm.get().getTeamMemberRole() != null) {
            Long tmRoleId = tm.get().getTeamMemberRole();
            Optional<DataScopeConfig> roleConfig = companyAdminId != null
                    ? dataScopeConfigRepository.findByCompanyAdminIdFkAndRoleIdFkAndModuleName(companyAdminId, tmRoleId, normalizedModule)
                    : dataScopeConfigRepository.findByRoleIdFkAndModuleName(tmRoleId, normalizedModule);
            if (roleConfig.isPresent()) {
                return roleConfig.get().getScopeMode();
            }
        }

        // 3. Fallback defaults
        if (isAdmin(user.getRole())) {
            return "ALL_DATA";
        }
        if (isTeamLead(user.getRole())) {
            return "TEAM_DATA";
        }
        return "OWN_DATA_ONLY";
    }
}

