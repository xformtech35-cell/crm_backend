package com.crm.service;

import com.crm.dto.request.ChangePasswordRequest;
import com.crm.dto.request.LoginRequest;
import com.crm.dto.response.ApiResponse;
import com.crm.dto.response.AuthResponse;
import com.crm.entity.Permission;
import com.crm.entity.Role;
import com.crm.entity.User;
import com.crm.exception.BadRequestException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.PermissionRepository;
import com.crm.repository.RoleRepository;
import com.crm.repository.UserRepository;
import com.crm.repository.TeamMemberRepository;
import com.crm.entity.TeamMember;
import com.crm.security.JwtTokenProvider;
import com.crm.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final com.crm.repository.UserPermissionRepository userPermissionRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final com.crm.repository.CreateTeamRepository createTeamRepository;
    private final com.crm.repository.TeamRepository teamRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final AuthUtil authUtil;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUserEmail(), request.getPassword())
        );

        User user = userRepository.findByUserEmail(request.getUserEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getUserEmail()));

        String token = tokenProvider.generateToken(user.getUserEmail(), user.getUserid(), user.getRole());

        List<String> permissions = getPermissionsForUser(user);
        String resolvedRole = authUtil.resolveRoleName(user.getRole());

        boolean integrationsAccess = false;
        String userRole = user.getRole();
        String companyName = "XForm CRM";
        String teamLeadName = "Not Assigned";
        Long teamId = null;

        if (authUtil.isSuperAdmin(userRole)) {
            integrationsAccess = true;
            companyName = "Super Admin HQ";
            teamLeadName = "System Super Admin";
        } else if (authUtil.isAdmin(userRole)) {
            integrationsAccess = user.isIntegrationsAccess();
            companyName = user.getUsername() != null && !user.getUsername().isBlank() ? user.getUsername() : user.getUserEmail();
            teamLeadName = "Company Administrator";
        } else {
            Optional<TeamMember> tmOpt = teamMemberRepository.findByTeamMemberEmail(user.getUserEmail());
            if (tmOpt.isPresent()) {
                TeamMember tm = tmOpt.get();
                if (tm.getUserIdFk() != null) {
                    Optional<User> adminOpt = userRepository.findById(tm.getUserIdFk());
                    if (adminOpt.isPresent()) {
                        User adminUser = adminOpt.get();
                        integrationsAccess = adminUser.isIntegrationsAccess();
                        companyName = adminUser.getUsername() != null && !adminUser.getUsername().isBlank()
                                ? adminUser.getUsername() : adminUser.getUserEmail();
                    }
                }

                // Resolve teamId and teamLeadName from team membership
                try {
                    List<com.crm.entity.CreateTeam> ctList = createTeamRepository.findByTeamMemberIdFk(tm.getTeamMemberId());
                    if (ctList != null && !ctList.isEmpty()) {
                        for (com.crm.entity.CreateTeam ct : ctList) {
                            if (ct.getTeamIdFk() != null) {
                                teamId = ct.getTeamIdFk(); // cache teamId for JWT claim
                                Optional<com.crm.entity.Team> teamOpt = teamRepository.findById(ct.getTeamIdFk());
                                if (teamOpt.isPresent() && teamOpt.get().getTeamLeadId() != null) {
                                    Long leadId = teamOpt.get().getTeamLeadId();
                                    Optional<TeamMember> leadOpt = teamMemberRepository.findById(leadId);
                                    if (leadOpt.isPresent()) {
                                        teamLeadName = leadOpt.get().getTeamMemberName();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        return AuthResponse.builder()
        .token(token)
        .tokenType("Bearer")
        .userId(user.getUserid())
        .username(user.getUsername())
        .userEmail(user.getUserEmail())
        .role(resolvedRole)
        .companyName(companyName)
        .teamLeadName(teamLeadName)
        .permissions(permissions)
        .integrationsAccess(integrationsAccess)
        .teamId(teamId)
        .build();
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        String resetPassword = UUID.randomUUID().toString().substring(0, 8);
        user.setPassword(passwordEncoder.encode(resetPassword));
        userRepository.save(user);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("CRM - Password Reset");
            message.setText("Your new password is: " + resetPassword + "\nPlease change it after login.");
            mailSender.send(message);
        } catch (Exception ignored) {
            // mail failure should not block the operation; user can contact admin
        }
    }

    private List<String> getPermissionsForUser(User user) {
        String roleField = user.getRole();
        if (roleField == null) return List.of();

        if (authUtil.isSuperAdmin(roleField)) {
            return List.of(
                "dashboard.view", "roles.view", "settings.view",
                "leads.view", "leads.create", "leads.edit", "leads.delete", "leads.import",
                "opportunities.view", "opportunities.create", "opportunities.edit", "opportunities.delete",
                "projects.view", "projects.create", "projects.edit", "projects.delete",
                "tasks.view", "tasks.create", "tasks.edit", "tasks.delete",
                "contacts.view", "contacts.create", "contacts.edit", "contacts.delete",
                "organizations.view", "organizations.create", "organizations.edit", "organizations.delete",
                "teams.view", "teams.create", "teams.edit", "teams.delete",
                "users.view", "users.create", "users.edit", "users.delete",
                "reports.view", "calendar.view", "calendar.create", "calendar.edit", "calendar.delete",
                "attendance.view", "attendance.edit", "integrations.view", "integrations.edit",
                "companies.view", "companies.create", "companies.edit", "companies.delete", "audit.view",
                "activities.view", "emails.view", "analytics.view", "automation.view",
                "trash.view", "trash.restore", "trash.delete", "data_access.view", "data_access.edit"
            );
        }

        // 1. User-specific custom permission overrides check (skip expired rows)
        if (userPermissionRepository != null && userPermissionRepository.existsByUserIdFk(user.getUserid())) {
            LocalDateTime now = LocalDateTime.now();
            List<com.crm.entity.UserPermission> userPerms = userPermissionRepository.findByUserIdFk(user.getUserid());
            List<String> activePerms = userPerms.stream()
                    .filter(p -> p.getExpiresAt() == null || p.getExpiresAt().isAfter(now))
                    .map(com.crm.entity.UserPermission::getGrpPerm)
                    .filter(p -> !"__NONE__".equals(p))
                    .collect(Collectors.toList());
            if (!activePerms.isEmpty() || userPerms.stream().anyMatch(p -> "__NONE__".equals(p.getGrpPerm()))) {
                return activePerms;
            }
        }

        // 2. Role permission fallback for Company Admin
        if (authUtil.isAdmin(roleField)) {
            Optional<Role> companyAdminRole = roleRepository.findByUserIdFk(user.getUserid())
                    .stream()
                    .filter(r -> "ADMIN".equalsIgnoreCase(r.getRoleName()))
                    .findFirst();

            if (companyAdminRole.isPresent()) {
                return permissionRepository.findByRoleIdFk(companyAdminRole.get().getRoleId())
                        .stream().map(Permission::getGrpPerm).collect(Collectors.toList());
            }

            return List.of();
        }

        // 3. Check TeamMember record for assigned teamMemberRole
        if (teamMemberRepository != null) {
            Optional<com.crm.entity.TeamMember> tmOpt = teamMemberRepository.findByTeamMemberEmail(user.getUserEmail());
            if (tmOpt.isPresent() && tmOpt.get().getTeamMemberRole() != null) {
                Long tmRoleId = tmOpt.get().getTeamMemberRole();
                return permissionRepository.findByRoleIdFk(tmRoleId)
                        .stream().map(Permission::getGrpPerm).collect(Collectors.toList());
            }
        }

        return getPermissionsByRoleField(roleField, user);
    }

    private List<String> getPermissionsByRoleField(String roleField, User user) {
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        Optional<Role> roleOpt = Optional.empty();
        try {
            Long roleId = Long.parseLong(roleField);
            roleOpt = roleRepository.findById(roleId);
        } catch (NumberFormatException e) {
            if (companyAdminId != null) {
                roleOpt = roleRepository.findByUserIdFk(companyAdminId).stream()
                        .filter(r -> roleField.equalsIgnoreCase(r.getRoleName()))
                        .findFirst();
            }
            if (roleOpt.isEmpty()) {
                roleOpt = roleRepository.findByRoleName(roleField);
            }
        }

        return roleOpt.map(role -> permissionRepository.findByRoleIdFk(role.getRoleId())
                        .stream().map(Permission::getGrpPerm).collect(Collectors.toList()))
                .orElse(List.of());
    }
}
