package com.crm.util;
import com.crm.entity.User;
import com.crm.entity.TeamMember;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.UserRepository;
import com.crm.repository.RoleRepository;
import com.crm.repository.TeamMemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthUtil {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final HttpServletRequest request;

    public User getCurrentUser(Authentication auth) {
        if (auth == null) {
            throw new org.springframework.security.access.AccessDeniedException("Access Denied: User is not authenticated");
        }
        User user = userRepository.findByUserEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", auth.getName()));

        if (isSuperAdmin(user.getRole())) {
            String companyIdStr = request.getHeader("X-Company-Id");
            String requestUri = request.getRequestURI();
            if (companyIdStr != null && !companyIdStr.trim().isEmpty() && !requestUri.contains("/superadmin")) {
                try {
                    Long companyId = Long.parseLong(companyIdStr.trim());
                    return userRepository.findById(companyId)
                            .orElseThrow(() -> new ResourceNotFoundException("Company Admin User", "id", companyId));
                } catch (Exception e) {
                    // Fallback to standard super admin user if parsing/finding fails
                }
            }
        }
        return user;
    }

    public Long getCompanyAdminId(User user) {
        if (user == null) return null;
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
        try {
            Long roleId = Long.parseLong(roleField);
            return roleRepository.findById(roleId)
                    .map(r -> r.getRoleName().toUpperCase())
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

    public boolean isAnyAdmin(String role) {
        return isSuperAdmin(role) || isAdmin(role);
    }
}

