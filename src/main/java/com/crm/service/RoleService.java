package com.crm.service;

import com.crm.dto.request.RoleRequest;
import com.crm.entity.Permission;
import com.crm.entity.Role;
import com.crm.entity.User;
import com.crm.exception.BadRequestException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.PermissionRepository;
import com.crm.repository.RoleRepository;
import com.crm.repository.UserRepository;
import com.crm.util.AuthUtil;
import com.crm.entity.TeamMember;
import com.crm.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final AuthUtil authUtil;

    public List<Role> getAllRoles(User currentUser) {
        if (authUtil.isSuperAdmin(currentUser.getRole())) {
            return roleRepository.findAll();
        }
        
        Long companyAdminId = getCompanyAdminId(currentUser);
        if (companyAdminId == null) {
            return roleRepository.findAll();
        }
        
        List<Role> companyRoles = roleRepository.findByUserIdFk(companyAdminId);
        
        List<Role> templateRoles = roleRepository.findByUserIdFk(null);
        List<Role> coreSystemRoles = templateRoles.stream()
                .filter(r -> {
                    String name = r.getRoleName();
                    return "ADMIN".equalsIgnoreCase(name) || "Team Lead".equalsIgnoreCase(name) || "TEAM_LEAD".equalsIgnoreCase(name);
                })
                .collect(Collectors.toList());
                
        companyRoles.addAll(coreSystemRoles);
        return companyRoles;
    }

    private Long getCompanyAdminId(User user) {
        if (authUtil.isSuperAdmin(user.getRole())) {
            return null;
        }
        if (authUtil.isAdmin(user.getRole())) {
            return user.getUserid();
        }
        return teamMemberRepository.findByTeamMemberEmail(user.getUserEmail())
                .map(TeamMember::getUserIdFk)
                .orElse(user.getUserid());
    }

    public Role getById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
    }

    public Role getById(Long id, User currentUser) {
        Role role = getById(id);
        if (!authUtil.isAnyAdmin(currentUser.getRole())) {
            throw new AccessDeniedException("Access denied");
        }
        return role;
    }

    public Role create(RoleRequest req, User currentUser) {
        String reqRole = req.getRoleName();
        if ("ADMIN".equalsIgnoreCase(reqRole) || "SUPER_ADMIN".equalsIgnoreCase(reqRole) || "SUPER ADMIN".equalsIgnoreCase(reqRole)) {
            if (!authUtil.isSuperAdmin(currentUser.getRole())) {
                throw new AccessDeniedException("Only Super Admins can create Admin or Super Admin roles");
            }
        }
        
        Long ownerId = authUtil.isSuperAdmin(currentUser.getRole()) ? null : currentUser.getUserid();
        boolean exists;
        if (ownerId == null) {
            exists = roleRepository.findByRoleName(req.getRoleName())
                    .map(r -> r.getUserIdFk() == null)
                    .orElse(false);
        } else {
            exists = roleRepository.existsByRoleNameAndUserIdFk(req.getRoleName(), ownerId);
        }
        if (exists) {
            throw new BadRequestException("Role already exists: " + req.getRoleName());
        }
        
        return roleRepository.save(Role.builder()
                .roleName(req.getRoleName())
                .userIdFk(ownerId)
                .build());
    }

    public Role update(Long id, RoleRequest req, User currentUser) {
        Role role = getById(id);
        
        if (!authUtil.isSuperAdmin(currentUser.getRole())) {
            if (role.getUserIdFk() == null) {
                throw new AccessDeniedException("Only Super Admins can modify system roles");
            }
            if (!role.getUserIdFk().equals(currentUser.getUserid())) {
                throw new AccessDeniedException("Access denied: You do not own this role");
            }
        }
        
        String oldName = role.getRoleName();
        String newName = req.getRoleName();

        boolean targetIsAdminOrSuper = "ADMIN".equalsIgnoreCase(oldName) || "SUPER_ADMIN".equalsIgnoreCase(oldName) || "SUPER ADMIN".equalsIgnoreCase(oldName);
        boolean requestIsAdminOrSuper = "ADMIN".equalsIgnoreCase(newName) || "SUPER_ADMIN".equalsIgnoreCase(newName) || "SUPER ADMIN".equalsIgnoreCase(newName);

        if (targetIsAdminOrSuper || requestIsAdminOrSuper) {
            if (!authUtil.isSuperAdmin(currentUser.getRole())) {
                throw new AccessDeniedException("Only Super Admins can modify Admin or Super Admin roles");
            }
        }

        role.setRoleName(req.getRoleName());
        return roleRepository.save(role);
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        Role role = getById(id);
        
        if (!authUtil.isSuperAdmin(currentUser.getRole())) {
            if (role.getUserIdFk() == null) {
                throw new AccessDeniedException("Only Super Admins can delete system roles");
            }
            if (!role.getUserIdFk().equals(currentUser.getUserid())) {
                throw new AccessDeniedException("Access denied: You do not own this role");
            }
        }
        
        String roleName = role.getRoleName();
        boolean targetIsAdminOrSuper = "ADMIN".equalsIgnoreCase(roleName) || "SUPER_ADMIN".equalsIgnoreCase(roleName) || "SUPER ADMIN".equalsIgnoreCase(roleName);

        if (targetIsAdminOrSuper) {
            if (!authUtil.isSuperAdmin(currentUser.getRole())) {
                throw new AccessDeniedException("Only Super Admins can delete Admin or Super Admin roles");
            }
        }
        permissionRepository.deleteByRoleIdFk(id);
        roleRepository.delete(role);
    }

    public List<Permission> getPermissions(Long roleId, User currentUser) {
        Role role = getById(roleId);
        if (!authUtil.isAnyAdmin(currentUser.getRole())) {
            throw new AccessDeniedException("Access denied");
        }
        return permissionRepository.findByRoleIdFk(roleId);
    }

    // MODIFIED: Saves permissions and bidirectionally syncs integrationsAccess on admin users
    @Transactional
    public List<Permission> savePermissions(Long roleId, List<String> permissionNames, User currentUser) {
        Role role = getById(roleId);
        
        if (!authUtil.isSuperAdmin(currentUser.getRole())) {
            String roleName = role.getRoleName();
            boolean isSuperAdminRole = "SUPER_ADMIN".equalsIgnoreCase(roleName) || "SUPER ADMIN".equalsIgnoreCase(roleName);
            
            // Only block SUPER_ADMIN and SUPER ADMIN - Allow ADMIN role editing
            if (isSuperAdminRole) {
                throw new AccessDeniedException("Only Super Admins can manage permissions for Super Admin roles");
            }
            
            // Check ownership for custom roles
            if (role.getUserIdFk() != null && !role.getUserIdFk().equals(currentUser.getUserid())) {
                throw new AccessDeniedException("Access denied: You do not own this role");
            }
        }

        permissionRepository.deleteByRoleIdFk(roleId);
        List<String> filtered = permissionNames == null ? new ArrayList<>() :
                permissionNames.stream().filter(p -> p != null && !p.isBlank()).collect(Collectors.toList());
        List<Permission> perms = filtered.stream()
                .map(p -> Permission.builder().roleIdFk(roleId).grpPerm(p).build())
                .collect(Collectors.toList());
        List<Permission> saved = permissionRepository.saveAll(perms);

        // BIDIRECTIONAL SYNC: If this is the global ADMIN role, sync integrationsAccess on all admin users
        if ("ADMIN".equalsIgnoreCase(role.getRoleName()) && role.getUserIdFk() == null) {
            boolean integrationsEnabled = filtered.contains("integrations.view");
            List<User> adminUsers = userRepository.findByRole("ADMIN");
            for (User adminUser : adminUsers) {
                if (adminUser.isIntegrationsAccess() != integrationsEnabled) {
                    adminUser.setIntegrationsAccess(integrationsEnabled);
                    userRepository.save(adminUser);
                }
            }
        }

        return saved;
    }

    /**
     * Called by SuperAdminController when a company's integrationsAccess is toggled.
     * Syncs the global ADMIN role's integrations.view / integrations.edit permissions
     * to match whether ANY admin user still has integrationsAccess = true.
     */
    @Transactional
    public void syncIntegrationsPermissionFromCompanyAccess() {
        // Find the global ADMIN role (userIdFk == null)
        Role adminRole = roleRepository.findByRoleName("ADMIN")
                .filter(r -> r.getUserIdFk() == null)
                .orElse(null);
        if (adminRole == null) return;

        // Check if any admin user has integrations access enabled
        boolean anyEnabled = userRepository.findByRole("ADMIN")
                .stream().anyMatch(User::isIntegrationsAccess);

        List<Permission> existingPerms = permissionRepository.findByRoleIdFk(adminRole.getRoleId());
        boolean hasIntegrationsView = existingPerms.stream()
                .anyMatch(p -> "integrations.view".equals(p.getGrpPerm()));
        boolean hasIntegrationsEdit = existingPerms.stream()
                .anyMatch(p -> "integrations.edit".equals(p.getGrpPerm()));

        if (anyEnabled && !hasIntegrationsView) {
            // Add integrations permissions to ADMIN role
            List<Permission> toAdd = new ArrayList<>();
            toAdd.add(Permission.builder().roleIdFk(adminRole.getRoleId()).grpPerm("integrations.view").build());
            toAdd.add(Permission.builder().roleIdFk(adminRole.getRoleId()).grpPerm("integrations.edit").build());
            permissionRepository.saveAll(toAdd);
        } else if (!anyEnabled && hasIntegrationsView) {
            // Remove integrations permissions from ADMIN role
            existingPerms.stream()
                    .filter(p -> "integrations.view".equals(p.getGrpPerm()) || "integrations.edit".equals(p.getGrpPerm()))
                    .forEach(permissionRepository::delete);
        }
    }

    public void deletePermission(Long permissionId, User currentUser) {
        Permission perm = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", permissionId));
        Role role = getById(perm.getRoleIdFk());
        
        if (!authUtil.isSuperAdmin(currentUser.getRole())) {
            if (role.getUserIdFk() != null && !role.getUserIdFk().equals(currentUser.getUserid())) {
                throw new AccessDeniedException("Access denied: You do not own this role");
            }
        }
        
        String roleName = role.getRoleName();
        boolean targetIsAdminOrSuper = "ADMIN".equalsIgnoreCase(roleName) || "SUPER_ADMIN".equalsIgnoreCase(roleName) || "SUPER ADMIN".equalsIgnoreCase(roleName);

        if (targetIsAdminOrSuper) {
            if (!authUtil.isSuperAdmin(currentUser.getRole())) {
                throw new AccessDeniedException("Only Super Admins can delete permissions of Admin or Super Admin roles");
            }
        }
        permissionRepository.delete(perm);
    }

    @Transactional
    public void copyTemplateRolesToCompany(User newCompanyAdmin) {
        List<Role> templateRoles = roleRepository.findByUserIdFk(null);
        for (Role templateRole : templateRoles) {
            String name = templateRole.getRoleName();
            if ("SUPER_ADMIN".equalsIgnoreCase(name) || "ADMIN".equalsIgnoreCase(name) || "SUPER ADMIN".equalsIgnoreCase(name)) {
                continue;
            }
            
            Role newRole = roleRepository.save(Role.builder()
                    .roleName(name)
                    .userIdFk(newCompanyAdmin.getUserid())
                    .build());
            
            List<Permission> templatePermissions = permissionRepository.findByRoleIdFk(templateRole.getRoleId());
            List<Permission> newPermissions = templatePermissions.stream()
                    .map(p -> Permission.builder()
                            .roleIdFk(newRole.getRoleId())
                            .grpPerm(p.getGrpPerm())
                            .build())
                    .collect(Collectors.toList());
            permissionRepository.saveAll(newPermissions);
        }
    }
}