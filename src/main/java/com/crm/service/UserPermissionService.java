package com.crm.service;

import com.crm.entity.User;
import com.crm.entity.UserPermission;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.UserPermissionRepository;
import com.crm.repository.UserRepository;
import com.crm.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserPermissionService {

    private final UserPermissionRepository userPermissionRepository;
    private final UserRepository userRepository;
    private final AuthUtil authUtil;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Map<String, Object> getUserPermissions(Long targetUserId, User currentUser) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));
        validateUserPermissionAccess(targetUser, currentUser);

        boolean hasCustom = userPermissionRepository.existsByUserIdFk(targetUserId);
        List<UserPermission> userPerms = userPermissionRepository.findByUserIdFk(targetUserId);
        LocalDateTime now = LocalDateTime.now();
        List<String> perms = userPerms.stream()
                .filter(p -> p.getExpiresAt() == null || p.getExpiresAt().isAfter(now))
                .map(UserPermission::getGrpPerm)
                .filter(p -> !"__NONE__".equals(p))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("userId", targetUserId);
        result.put("hasCustomPermissions", hasCustom);
        result.put("permissions", perms);
        return result;
    }

    @Transactional(readOnly = true)
    public List<UserPermission> getActiveOverrides(Long targetUserId, User currentUser) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));
        validateUserPermissionAccess(targetUser, currentUser);
        return userPermissionRepository.findByUserIdFk(targetUserId).stream()
                .filter(p -> !"__NONE__".equals(p.getGrpPerm()))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<String> saveUserPermissions(Long targetUserId, List<String> permissions, User currentUser) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));
        validateUserPermissionAccess(targetUser, currentUser);

        List<String> oldPerms = userPermissionRepository.findByUserIdFk(targetUserId).stream()
                .map(UserPermission::getGrpPerm).collect(Collectors.toList());

        userPermissionRepository.deleteByUserIdFk(targetUserId);
        List<String> filtered = permissions == null ? new ArrayList<>()
                : permissions.stream().filter(p -> p != null && !p.isBlank() && !"__NONE__".equals(p))
                             .distinct().collect(Collectors.toList());

        List<UserPermission> toSave;
        if (filtered.isEmpty()) {
            toSave = List.of(UserPermission.builder().userIdFk(targetUserId).grpPerm("__NONE__").build());
        } else {
            toSave = filtered.stream()
                    .map(p -> UserPermission.builder()
                            .userIdFk(targetUserId).grpPerm(p)
                            .createdByFk(currentUser.getUserid())
                            .createdAt(LocalDateTime.now())
                            .build())
                    .collect(Collectors.toList());
        }
        userPermissionRepository.saveAll(toSave);

        Long companyAdminId = authUtil.getCompanyAdminId(currentUser);
        auditLogService.log(currentUser.getUserid(), "PERMISSION_CHANGE", "UserPermission",
                targetUserId, oldPerms, filtered, companyAdminId);
        return filtered;
    }

    @Transactional
    public UserPermission createScopedOverride(Long targetUserId, String permission,
                                               LocalDateTime expiresAt, User currentUser) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));
        validateUserPermissionAccess(targetUser, currentUser);

        UserPermission override = UserPermission.builder()
                .userIdFk(targetUserId)
                .grpPerm(permission)
                .expiresAt(expiresAt)
                .createdByFk(currentUser.getUserid())
                .createdAt(LocalDateTime.now())
                .build();
        UserPermission saved = userPermissionRepository.save(override);

        Long companyAdminId = authUtil.getCompanyAdminId(currentUser);
        auditLogService.log(currentUser.getUserid(), "USER_OVERRIDE_CREATE", "UserPermission",
                saved.getUserPermissionId(), null,
                Map.of("userId", targetUserId, "permission", permission,
                        "expiresAt", String.valueOf(expiresAt)),
                companyAdminId);
        return saved;
    }

    @Transactional
    public void deleteOverride(Long overrideId, User currentUser) {
        UserPermission perm = userPermissionRepository.findById(overrideId)
                .orElseThrow(() -> new ResourceNotFoundException("UserPermission", "id", overrideId));
        User targetUser = userRepository.findById(perm.getUserIdFk())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", perm.getUserIdFk()));
        validateUserPermissionAccess(targetUser, currentUser);

        Long companyAdminId = authUtil.getCompanyAdminId(currentUser);
        auditLogService.log(currentUser.getUserid(), "USER_OVERRIDE_DELETE", "UserPermission",
                overrideId, perm.getGrpPerm(), null, companyAdminId);
        userPermissionRepository.delete(perm);
    }

    @Transactional
    public void resetUserPermissionsToDefault(Long targetUserId, User currentUser) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));
        validateUserPermissionAccess(targetUser, currentUser);
        userPermissionRepository.deleteByUserIdFk(targetUserId);
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeExpired() {
        List<UserPermission> all = userPermissionRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        List<UserPermission> expired = all.stream()
                .filter(p -> p.getExpiresAt() != null && p.getExpiresAt().isBefore(now))
                .collect(Collectors.toList());
        if (!expired.isEmpty()) {
            userPermissionRepository.deleteAll(expired);
        }
    }

    private void validateUserPermissionAccess(User targetUser, User currentUser) {
        if (authUtil.isSuperAdmin(currentUser.getRole())) return;
        Long companyAdminId = authUtil.getCompanyAdminId(currentUser);
        Long targetCompanyAdminId = authUtil.getCompanyAdminId(targetUser);
        if (companyAdminId != null && !companyAdminId.equals(targetCompanyAdminId)) {
            throw new AccessDeniedException("Access denied: Cannot manage permissions for users in another company");
        }
    }
}