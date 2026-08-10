package com.crm.service;

import com.crm.dto.request.RoleRequest;
import com.crm.entity.DataScopeConfig;
import com.crm.entity.Permission;
import com.crm.entity.Role;
import com.crm.entity.User;
import com.crm.exception.BadRequestException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.DataScopeConfigRepository;
import com.crm.repository.PermissionRepository;
import com.crm.repository.RoleRepository;
import com.crm.repository.TeamMemberRepository;
import com.crm.repository.UserRepository;
import com.crm.util.AuthUtil;
import com.crm.entity.TeamMember;
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
    private final DataScopeConfigRepository dataScopeConfigRepository;
    private final AuthUtil authUtil;
    private final AuditLogService auditLogService;

    // ─────────────────────────────────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────────────────────────────────

    public List<Role> getAllRoles(User currentUser, Long companyAdminIdParam) {
        Long companyAdminId = companyAdminIdParam;
        if (companyAdminId == null && !authUtil.isSuperAdmin(currentUser.getRole())) {
            companyAdminId = getCompanyAdminId(currentUser);
        }

        if (companyAdminId != null) {
            List<Role> companyRoles = roleRepository.findByUserIdFk(companyAdminId);

            boolean hasAdmin = companyRoles.stream().anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getRoleName()));
            if (!hasAdmin) {
                Role newAdmin = roleRepository.save(Role.builder()
                        .roleName("ADMIN").userIdFk(companyAdminId)
                        .isSystemRole(true).roleLevel(1).build());
                companyRoles.add(newAdmin);
            }

            boolean hasTeamLead = companyRoles.stream().anyMatch(r ->
                "Team Lead".equalsIgnoreCase(r.getRoleName()) || "TEAM_LEAD".equalsIgnoreCase(r.getRoleName()));
            if (!hasTeamLead) {
                Role newTeamLead = roleRepository.save(Role.builder()
                        .roleName("Team Lead").userIdFk(companyAdminId)
                        .isSystemRole(true).roleLevel(2).build());
                companyRoles.add(newTeamLead);
            }

            return companyRoles;
        }
        return roleRepository.findAll();
    }

    private Long getCompanyAdminId(User user) {
        if (authUtil.isSuperAdmin(user.getRole())) return null;
        if (authUtil.isAdmin(user.getRole())) return user.getUserid();
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

    public long getUserCountByRole(Long roleId) {
        return teamMemberRepository.countByTeamMemberRole(roleId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────────────────────────────────────

    public Role create(RoleRequest req, User currentUser) {
        String reqRole = req.getRoleName();
        // Prevent non-super-admins from creating ADMIN/SUPER_ADMIN roles
        if ("ADMIN".equalsIgnoreCase(reqRole) || "SUPER_ADMIN".equalsIgnoreCase(reqRole)
                || "SUPER ADMIN".equalsIgnoreCase(reqRole)) {
            if (!authUtil.isSuperAdmin(currentUser.getRole())) {
                throw new AccessDeniedException("Only Super Admins can create Admin or Super Admin roles");
            }
        }

        Long ownerId = authUtil.isSuperAdmin(currentUser.getRole()) ? null : currentUser.getUserid();
        boolean exists;
        if (ownerId == null) {
            exists = roleRepository.findByRoleName(req.getRoleName())
                    .map(r -> r.getUserIdFk() == null).orElse(false);
        } else {
            exists = roleRepository.existsByRoleNameAndUserIdFk(req.getRoleName(), ownerId);
        }
        if (exists) throw new BadRequestException("Role already exists: " + req.getRoleName());

        Role saved = roleRepository.save(Role.builder()
                .roleName(req.getRoleName())
                .userIdFk(ownerId)
                .isSystemRole(false)
                .build());

        Long companyAdminId = authUtil.getCompanyAdminId(currentUser);
        auditLogService.log(currentUser.getUserid(), "ROLE_CREATE", "Role",
                saved.getRoleId(), null, saved, companyAdminId);
        return saved;
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
            // Block renaming system roles (ADMIN, Team Lead) for non-super-admins
            if (Boolean.TRUE.equals(role.getIsSystemRole())) {
                throw new AccessDeniedException("System roles cannot be renamed. Edit permissions instead.");
            }
        }

        String oldName = role.getRoleName();
        role.setRoleName(req.getRoleName());
        Role saved = roleRepository.save(role);

        Long companyAdminId = authUtil.getCompanyAdminId(currentUser);
        auditLogService.log(currentUser.getUserid(), "ROLE_UPDATE", "Role",
                id, oldName, req.getRoleName(), companyAdminId);
        return saved;
    }

    @Transactional
    public void delete(Long id, User currentUser) {
        Role role = getById(id);

        // System roles cannot be deleted (replaces string-literal ADMIN check)
        if (Boolean.TRUE.equals(role.getIsSystemRole())) {
            if (!authUtil.isSuperAdmin(currentUser.getRole())) {
                throw new AccessDeniedException("System roles (ADMIN, Team Lead) cannot be deleted");
            }
        }

        if (!authUtil.isSuperAdmin(currentUser.getRole())) {
            Long companyAdminId = getCompanyAdminId(currentUser);
            if (role.getUserIdFk() != null && !role.getUserIdFk().equals(companyAdminId)) {
                throw new AccessDeniedException("Access denied: You do not own this role");
            }
        }

        Long companyAdminId = authUtil.getCompanyAdminId(currentUser);
        auditLogService.log(currentUser.getUserid(), "ROLE_DELETE", "Role",
                id, role.getRoleName(), null, companyAdminId);

        permissionRepository.deleteByRoleIdFk(id);
        roleRepository.delete(role);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Clone Role (Part 2)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public Role cloneRole(Long sourceRoleId, String newName, User currentUser) {
        Role source = getById(sourceRoleId);
        Long ownerId = authUtil.isSuperAdmin(currentUser.getRole()) ? source.getUserIdFk() : currentUser.getUserid();

        if (roleRepository.existsByRoleNameAndUserIdFk(newName, ownerId)) {
            throw new BadRequestException("A role with name '" + newName + "' already exists");
        }

        // Create the new role
        Role cloned = roleRepository.save(Role.builder()
                .roleName(newName)
                .userIdFk(ownerId)
                .isSystemRole(false)
                .clonedFromRoleId(sourceRoleId)
                .build());

        // Copy all permissions from source
        List<Permission> sourcePerms = permissionRepository.findByRoleIdFk(sourceRoleId);
        List<Permission> clonedPerms = sourcePerms.stream()
                .map(p -> Permission.builder().roleIdFk(cloned.getRoleId()).grpPerm(p.getGrpPerm()).build())
                .collect(Collectors.toList());
        permissionRepository.saveAll(clonedPerms);

        // Copy all scope configs from source
        List<DataScopeConfig> sourceScopes = dataScopeConfigRepository.findByRoleIdFk(sourceRoleId);
        List<DataScopeConfig> clonedScopes = sourceScopes.stream()
                .map(s -> DataScopeConfig.builder()
                        .roleIdFk(cloned.getRoleId())
                        .companyAdminIdFk(s.getCompanyAdminIdFk())
                        .moduleName(s.getModuleName())
                        .scopeMode(s.getScopeMode())
                        .build())
                .collect(Collectors.toList());
        dataScopeConfigRepository.saveAll(clonedScopes);

        Long companyAdminId = authUtil.getCompanyAdminId(currentUser);
        auditLogService.log(currentUser.getUserid(), "ROLE_CLONE", "Role",
                cloned.getRoleId(), "cloned from role#" + sourceRoleId + " (" + source.getRoleName() + ")",
                newName, companyAdminId);

        return cloned;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Permissions
    // ─────────────────────────────────────────────────────────────────────────

    public List<Permission> getPermissions(Long roleId, User currentUser) {
        getById(roleId); // verify exists
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
            boolean isSuperAdminRole = "SUPER_ADMIN".equalsIgnoreCase(roleName) || "SUPER ADMIN".equalsIgnoreCase(roleName);
            if (isSuperAdminRole) {
                throw new AccessDeniedException("Only Super Admins can manage permissions for Super Admin roles");
            }
            if (role.getUserIdFk() != null && !role.getUserIdFk().equals(currentUser.getUserid())) {
                throw new AccessDeniedException("Access denied: You do not own this role");
            }
        }

        List<Permission> oldPerms = permissionRepository.findByRoleIdFk(roleId);
        List<String> oldPermNames = oldPerms.stream().map(Permission::getGrpPerm).collect(Collectors.toList());

        permissionRepository.deleteByRoleIdFk(roleId);
        List<String> filtered = permissionNames == null ? new ArrayList<>()
                : permissionNames.stream().filter(p -> p != null && !p.isBlank()).collect(Collectors.toList());
        List<Permission> perms = filtered.stream()
                .map(p -> Permission.builder().roleIdFk(roleId).grpPerm(p).build())
                .collect(Collectors.toList());
        List<Permission> saved = permissionRepository.saveAll(perms);

        // BIDIRECTIONAL SYNC: If this is the company ADMIN role, sync integrationsAccess
        if ("ADMIN".equalsIgnoreCase(role.getRoleName()) && Boolean.TRUE.equals(role.getIsSystemRole())
                && role.getUserIdFk() == null) {
            boolean integrationsEnabled = filtered.contains("integrations.view");
            List<User> adminUsers = userRepository.findByRole("ADMIN");
            for (User adminUser : adminUsers) {
                if (adminUser.isIntegrationsAccess() != integrationsEnabled) {
                    adminUser.setIntegrationsAccess(integrationsEnabled);
                    userRepository.save(adminUser);
                }
            }
        }

        Long companyAdminId = authUtil.getCompanyAdminId(currentUser);
        auditLogService.log(currentUser.getUserid(), "PERMISSION_CHANGE", "Role",
                roleId, oldPermNames, filtered, companyAdminId);

        return saved;
    }

    @Transactional
    public void syncIntegrationsPermissionFromCompanyAccess() {
        Role adminRole = roleRepository.findByRoleName("ADMIN")
                .filter(r -> r.getUserIdFk() == null).orElse(null);
        if (adminRole == null) return;

        boolean anyEnabled = userRepository.findByRole("ADMIN").stream().anyMatch(User::isIntegrationsAccess);
        List<Permission> existingPerms = permissionRepository.findByRoleIdFk(adminRole.getRoleId());
        boolean hasIntegrationsView = existingPerms.stream().anyMatch(p -> "integrations.view".equals(p.getGrpPerm()));
        boolean hasIntegrationsEdit = existingPerms.stream().anyMatch(p -> "integrations.edit".equals(p.getGrpPerm()));

        if (anyEnabled && !hasIntegrationsView) {
            List<Permission> toAdd = new ArrayList<>();
            toAdd.add(Permission.builder().roleIdFk(adminRole.getRoleId()).grpPerm("integrations.view").build());
            toAdd.add(Permission.builder().roleIdFk(adminRole.getRoleId()).grpPerm("integrations.edit").build());
            permissionRepository.saveAll(toAdd);
        } else if (!anyEnabled && hasIntegrationsView) {
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
            // Block deleting permissions from system roles for non-super-admins
            if (Boolean.TRUE.equals(role.getIsSystemRole()) && "ADMIN".equalsIgnoreCase(role.getRoleName())) {
                throw new AccessDeniedException("Only Super Admins can delete permissions of Admin roles");
            }
            if (role.getUserIdFk() != null && !role.getUserIdFk().equals(currentUser.getUserid())) {
                throw new AccessDeniedException("Access denied: You do not own this role");
            }
        }

        permissionRepository.delete(perm);
    }

    @Transactional
    public void copyTemplateRolesToCompany(User newCompanyAdmin) {
        // Create company-specific ADMIN role as a system role
        roleRepository.save(Role.builder()
                .roleName("ADMIN")
                .userIdFk(newCompanyAdmin.getUserid())
                .isSystemRole(true)
                .roleLevel(1)
                .build());

        // Create company-specific Team Lead role as a system role
        roleRepository.save(Role.builder()
                .roleName("Team Lead")
                .userIdFk(newCompanyAdmin.getUserid())
                .isSystemRole(true)
                .roleLevel(2)
                .build());
    }
}
