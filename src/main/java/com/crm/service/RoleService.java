package com.crm.service;

import com.crm.dto.request.RoleRequest;
import com.crm.entity.Permission;
import com.crm.entity.Role;
import com.crm.entity.User;
import com.crm.exception.BadRequestException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.PermissionRepository;
import com.crm.repository.RoleRepository;
import com.crm.util.AuthUtil;
import com.crm.entity.TeamMember;
import com.crm.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AuthUtil authUtil;

    public List<Role> getAllRoles(User currentUser) {
        if (authUtil.isSuperAdmin(currentUser.getRole())) {
            return roleRepository.findAll();
        }
        
        Long companyAdminId = getCompanyAdminId(currentUser);
        if (companyAdminId == null) {
            return roleRepository.findAll();
        }
        
        // 1. Fetch only the company's custom roles
        List<Role> companyRoles = roleRepository.findByUserIdFk(companyAdminId);
        
        // 2. Return the company's roles plus the core system roles (where userIdFk is null)
        List<Role> templateRoles = roleRepository.findByUserIdFk(null);
        List<Role> coreSystemRoles = templateRoles.stream()
                .filter(r -> {
                    String name = r.getRoleName();
                    return "ADMIN".equalsIgnoreCase(name);
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

    @Transactional
    public List<Permission> savePermissions(Long roleId, List<String> permissionNames, User currentUser) {
        Role role = getById(roleId);
        
        if (!authUtil.isSuperAdmin(currentUser.getRole())) {
            String roleName = role.getRoleName();
            boolean targetIsAdminOrSuper = "ADMIN".equalsIgnoreCase(roleName) || "SUPER_ADMIN".equalsIgnoreCase(roleName) || "SUPER ADMIN".equalsIgnoreCase(roleName);

            if (targetIsAdminOrSuper) {
                throw new AccessDeniedException("Only Super Admins can manage permissions for Admin or Super Admin roles");
            }
            if (role.getUserIdFk() != null && !role.getUserIdFk().equals(currentUser.getUserid())) {
                throw new AccessDeniedException("Access denied: You do not own this role");
            }
        }

        permissionRepository.deleteByRoleIdFk(roleId);
        List<Permission> perms = permissionNames.stream()
                .filter(p -> p != null && !p.isBlank())
                .map(p -> Permission.builder().roleIdFk(roleId).grpPerm(p).build())
                .collect(Collectors.toList());
        return permissionRepository.saveAll(perms);
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
