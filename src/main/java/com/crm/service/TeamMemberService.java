package com.crm.service;

import com.crm.dto.request.TeamMemberRequest;
import com.crm.entity.TeamMember;
import com.crm.entity.User;
import com.crm.exception.BadRequestException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.PermissionRepository;
import com.crm.repository.RoleRepository;
import com.crm.repository.TeamMemberRepository;
import com.crm.repository.UserPermissionRepository;
import com.crm.repository.UserRepository;
import com.crm.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TeamMemberService {

    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthUtil authUtil;

    public List<TeamMember> getAllTeamMembers(Long userId, String role) {
        List<TeamMember> members;
        if (authUtil.isSuperAdmin(role)) {
            members = teamMemberRepository.findAll();
        } else {
            User user = userRepository.findById(userId).orElse(null);
            String scopeMode = authUtil.resolveDataScopeMode(user, "TEAM_MEMBERS");

            if ("ALL_DATA".equals(scopeMode)) {
                Long companyId = authUtil.getCompanyAdminId(user);
                members = teamMemberRepository.findByUserIdFk(companyId != null ? companyId : userId);
            } else if ("TEAM_DATA".equals(scopeMode)) {
                List<String> teammateEmails = authUtil.getTeamLeadMemberEmails(user);
                Long companyId = authUtil.getCompanyAdminId(user);
                List<TeamMember> companyMembers = teamMemberRepository.findByUserIdFk(companyId != null ? companyId : userId);
                
                members = companyMembers.stream()
                        .filter(tm -> {
                            if (tm.getTeamMemberEmail() == null) return false;
                            String email = tm.getTeamMemberEmail().trim().toLowerCase();
                            // Include assigned team members OR unassigned company members so Team Lead can assign them
                            boolean isTeammate = teammateEmails.contains(email);
                            boolean isUnassigned = (tm.getTeamIdFk() == null);
                            return isTeammate || isUnassigned;
                        })
                        .collect(java.util.stream.Collectors.toList());
            } else {
                // OWN_DATA_ONLY
                if (user != null && user.getUserEmail() != null) {
                    members = teamMemberRepository.findByTeamMemberEmail(user.getUserEmail())
                            .map(tm -> new java.util.ArrayList<>(List.of(tm)))
                            .orElseGet(java.util.ArrayList::new);
                } else {
                    members = new java.util.ArrayList<>();
                }
            }
        }
        for (TeamMember member : members) {
            populateUserFields(member);
        }

        // Ensure Company Admin user associated with this team is included in the list for visibility
        if (role != null && !authUtil.isSuperAdmin(role)) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                Long companyAdminId = authUtil.getCompanyAdminId(user);
                if (companyAdminId != null) {
                    Optional<User> adminOpt = userRepository.findById(companyAdminId);
                    if (adminOpt.isPresent()) {
                        User adminUser = adminOpt.get();
                        boolean adminInList = members.stream().anyMatch(m ->
                            (m.getTeamMemberEmail() != null && m.getTeamMemberEmail().equalsIgnoreCase(adminUser.getUserEmail()))
                        );
                        if (!adminInList && adminUser.getUserEmail() != null) {
                            TeamMember adminTm = TeamMember.builder()
                                    .teamMemberId(-adminUser.getUserid())
                                    .teamMemberName(adminUser.getUsername() != null && !adminUser.getUsername().isBlank() ? adminUser.getUsername() : "Company Admin")
                                    .teamMemberEmail(adminUser.getUserEmail())
                                    .teamMemberMobile("")
                                    .userIdFk(adminUser.getUserid())
                                    .build();
                            members.add(0, adminTm);
                        }
                    }
                }
            }
        }

        return members;
    }

    public TeamMember getById(Long id) {
        TeamMember member = teamMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TeamMember", "id", id));
        populateUserFields(member);
        return member;
    }

    public TeamMember getById(Long id, User currentUser) {
        TeamMember member = getById(id);
        validateTeamMemberAccess(member, currentUser);
        return member;
    }

    private boolean isRoleAdminOrSuperAdmin(Long roleId) {
        if (roleId == null) return false;
        return roleRepository.findById(roleId)
                .map(r -> "ADMIN".equalsIgnoreCase(r.getRoleName()) || "SUPER_ADMIN".equalsIgnoreCase(r.getRoleName()) || "SUPER ADMIN".equalsIgnoreCase(r.getRoleName()))
                .orElse(false);
    }

    private boolean isRoleAdminOrSuperAdminOrTeamLead(Long roleId) {
        if (roleId == null) return false;
        return roleRepository.findById(roleId)
                .map(r -> {
                    String name = r.getRoleName();
                    return "ADMIN".equalsIgnoreCase(name) || "SUPER_ADMIN".equalsIgnoreCase(name) || "SUPER ADMIN".equalsIgnoreCase(name) || "TEAM LEAD".equalsIgnoreCase(name) || "TEAM_LEAD".equalsIgnoreCase(name);
                })
                .orElse(false);
    }

    /**
     * Check if user has 'users.create' permission via the Roles & Permissions matrix.
     * First checks user-level overrides, then falls back to role-level permissions.
     */
    private boolean hasUsersCreatePermission(User user) {
        try {
            // User-level override takes priority
            if (userPermissionRepository.existsByUserIdFk(user.getUserid())) {
                return userPermissionRepository.findByUserIdFk(user.getUserid())
                        .stream().anyMatch(p -> "users.create".equals(p.getGrpPerm()));
            }
            // Role-level permission check
            Long roleId = Long.parseLong(user.getRole());
            return permissionRepository.existsByRoleIdFkAndGrpPerm(roleId, "users.create");
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public TeamMember create(TeamMemberRequest req, User currentUser) {
        String currentRole = currentUser.getRole();
        boolean isManager = authUtil.isAnyAdmin(currentRole) || authUtil.isTeamLead(currentRole);

        // Fallback: honour the Roles & Permissions matrix — if user has
        // 'users.create' permission they should be allowed to create members.
        if (!isManager) {
            isManager = hasUsersCreatePermission(currentUser);
        }

        if (!isManager) {
            throw new AccessDeniedException("Access denied");
        }

        if (authUtil.isTeamLead(currentRole)) {
            if (isRoleAdminOrSuperAdminOrTeamLead(req.getTeamMemberRole())) {
                throw new AccessDeniedException("Team Leads cannot create Admin, Super Admin, or Team Lead members");
            }
        } else if (authUtil.isAdmin(currentRole)) {
            if (isRoleAdminOrSuperAdmin(req.getTeamMemberRole())) {
                throw new AccessDeniedException("Admins cannot create Admin or Super Admin members");
            }
        }

        if (userRepository.existsByUserEmail(req.getTeamMemberEmail())) {
            throw new BadRequestException("Email is already registered: " + req.getTeamMemberEmail());
        }

        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new BadRequestException("Password is required for new team members");
        }

        // Determine company admin ID
        Long companyAdminId = authUtil.getCompanyAdminId(currentUser);

        // Also create a User account so the team member can login
        User user = User.builder()
                .username(req.getTeamMemberEmail())
                .userEmail(req.getTeamMemberEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getTeamMemberRole() != null ? req.getTeamMemberRole().toString() : "member")
                .createdDate(LocalDate.now())
                .build();
        userRepository.save(user);

        TeamMember member = TeamMember.builder()
                .teamMemberName(req.getTeamMemberName())
                .teamMemberRole(req.getTeamMemberRole())
                .teamMemberMobile(req.getTeamMemberMobile())
                .teamMemberEmail(req.getTeamMemberEmail())
                .userIdFk(companyAdminId)
                .build();
        TeamMember saved = teamMemberRepository.save(member);
        populateUserFields(saved);
        return saved;
    }

    @Transactional
    public TeamMember update(Long id, TeamMemberRequest req, User currentUser) {
        String currentRole = currentUser.getRole();
        boolean isManager = authUtil.isAnyAdmin(currentRole) || authUtil.isTeamLead(currentRole);
        if (!isManager) {
            throw new AccessDeniedException("Access denied");
        }

        TeamMember member = getById(id);
        validateTeamMemberAccess(member, currentUser);
        
        User targetUser = userRepository.findByUserEmail(member.getTeamMemberEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", member.getTeamMemberEmail()));

        if (authUtil.isTeamLead(currentRole)) {
            if (authUtil.isAnyAdmin(targetUser.getRole()) || authUtil.isTeamLead(targetUser.getRole())) {
                throw new AccessDeniedException("Team Leads cannot edit Admin, Super Admin, or Team Lead members");
            }
            if (isRoleAdminOrSuperAdminOrTeamLead(req.getTeamMemberRole())) {
                throw new AccessDeniedException("Team Leads cannot assign Admin, Super Admin, or Team Lead roles");
            }
        } else if (authUtil.isAdmin(currentRole)) {
            if (authUtil.isAnyAdmin(targetUser.getRole())) {
                throw new AccessDeniedException("Admins cannot edit Admin or Super Admin members");
            }
            if (isRoleAdminOrSuperAdmin(req.getTeamMemberRole())) {
                throw new AccessDeniedException("Admins cannot assign Admin or Super Admin roles");
            }
        }

        if (!member.getTeamMemberEmail().equalsIgnoreCase(req.getTeamMemberEmail())) {
            if (userRepository.existsByUserEmail(req.getTeamMemberEmail())) {
                throw new BadRequestException("Email is already registered: " + req.getTeamMemberEmail());
            }
            targetUser.setUserEmail(req.getTeamMemberEmail());
            targetUser.setUsername(req.getTeamMemberEmail());
        }
        if (req.getTeamMemberRole() != null) {
            targetUser.setRole(req.getTeamMemberRole().toString());
        }
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            targetUser.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        userRepository.save(targetUser);

        member.setTeamMemberName(req.getTeamMemberName());
        member.setTeamMemberRole(req.getTeamMemberRole());
        member.setTeamMemberMobile(req.getTeamMemberMobile());
        member.setTeamMemberEmail(req.getTeamMemberEmail());
        TeamMember saved = teamMemberRepository.save(member);
        populateUserFields(saved);
        return saved;
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        String currentRole = currentUser.getRole();
        boolean isManager = authUtil.isAnyAdmin(currentRole) || authUtil.isTeamLead(currentRole);
        if (!isManager) {
            throw new AccessDeniedException("Access denied");
        }

        TeamMember member = getById(id);
        validateTeamMemberAccess(member, currentUser);
        
        User targetUser = userRepository.findByUserEmail(member.getTeamMemberEmail()).orElse(null);

        if (authUtil.isTeamLead(currentRole)) {
            if (targetUser != null && (authUtil.isAnyAdmin(targetUser.getRole()) || authUtil.isTeamLead(targetUser.getRole()))) {
                throw new AccessDeniedException("Team Leads cannot delete Admin, Super Admin, or Team Lead members");
            }
        } else if (authUtil.isAdmin(currentRole)) {
            if (targetUser != null && authUtil.isAnyAdmin(targetUser.getRole())) {
                throw new AccessDeniedException("Admins cannot delete Admin or Super Admin members");
            }
        }

        if (targetUser != null) {
            userRepository.delete(targetUser);
        }
        teamMemberRepository.delete(member);
    }

    private void validateTeamMemberAccess(TeamMember member, User currentUser) {
        if (authUtil.isSuperAdmin(currentUser.getRole())) return;

        Long companyAdminId = authUtil.getCompanyAdminId(currentUser);
        if (member.getUserIdFk() == null || (!member.getUserIdFk().equals(currentUser.getUserid()) && !member.getUserIdFk().equals(companyAdminId))) {
            throw new AccessDeniedException("Access denied to this team member");
        }

        // 1. Prevent editing or deleting Senior Admin accounts by non-super-admins
        User targetUser = userRepository.findByUserEmail(member.getTeamMemberEmail()).orElse(null);
        if (targetUser != null && authUtil.isAnyAdmin(targetUser.getRole()) && !authUtil.isSuperAdmin(currentUser.getRole())) {
            if (!targetUser.getUserid().equals(currentUser.getUserid())) {
                throw new AccessDeniedException("Cannot modify or manage senior Admin accounts");
            }
        }

        // 2. Team Leads can only manage Team Members belonging to their team(s)
        if (authUtil.isTeamLead(currentUser.getRole())) {
            if (targetUser != null && (authUtil.isAnyAdmin(targetUser.getRole()) || authUtil.isTeamLead(targetUser.getRole()))) {
                if (!targetUser.getUserid().equals(currentUser.getUserid())) {
                    throw new AccessDeniedException("Team Leads cannot modify or manage Admin or other Team Lead accounts");
                }
            }
            List<String> teammateEmails = authUtil.getTeamLeadMemberEmails(currentUser);
            if (!teammateEmails.contains(member.getTeamMemberEmail().trim().toLowerCase())) {
                throw new AccessDeniedException("Team Leads can only manage team members assigned to their team");
            }
        }

        // 3. Regular Team Members cannot edit or delete Team Leads or Admins
        if (!authUtil.isAnyAdmin(currentUser.getRole()) && !authUtil.isTeamLead(currentUser.getRole())) {
            if (!member.getTeamMemberEmail().equalsIgnoreCase(currentUser.getUserEmail())) {
                throw new AccessDeniedException("Team Members can only manage their own account");
            }
        }
    }

    private void populateUserFields(TeamMember member) {
        if (member != null && member.getTeamMemberEmail() != null) {
            userRepository.findByUserEmail(member.getTeamMemberEmail()).ifPresent(user -> {
                member.setUserid(user.getUserid());
                member.setUsername(user.getUsername());
                member.setUserEmail(user.getUserEmail());
            });
        }
    }
}
