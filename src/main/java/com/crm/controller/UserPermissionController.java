package com.crm.controller;

import com.crm.dto.response.ApiResponse;
import com.crm.entity.User;
import com.crm.entity.UserPermission;
import com.crm.service.UserPermissionService;
import com.crm.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user-permissions")
@RequiredArgsConstructor
public class UserPermissionController {

    private final UserPermissionService userPermissionService;
    private final AuthUtil authUtil;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserPermissions(
            @PathVariable Long userId, Authentication auth) {
        User currentUser = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("User permissions fetched",
                userPermissionService.getUserPermissions(userId, currentUser)));
    }

    @GetMapping("/{userId}/overrides")
    public ResponseEntity<ApiResponse<List<UserPermission>>> getOverrides(
            @PathVariable Long userId, Authentication auth) {
        User currentUser = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("User overrides fetched",
                userPermissionService.getActiveOverrides(userId, currentUser)));
    }

    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<String>>> saveUserPermissions(
            @PathVariable Long userId, @RequestBody Map<String, List<String>> body, Authentication auth) {
        User currentUser = authUtil.getCurrentUser(auth);
        List<String> permissions = body.get("permissions");
        return ResponseEntity.ok(ApiResponse.success("User permissions saved",
                userPermissionService.saveUserPermissions(userId, permissions, currentUser)));
    }

    @PostMapping("/{userId}/override")
    public ResponseEntity<ApiResponse<UserPermission>> createOverride(
            @PathVariable Long userId, @RequestBody Map<String, String> body, Authentication auth) {
        User currentUser = authUtil.getCurrentUser(auth);
        String permission = body.get("permission");
        String expiresAtStr = body.get("expiresAt");
        LocalDateTime expiresAt = (expiresAtStr != null && !expiresAtStr.isBlank())
            ? LocalDateTime.parse(expiresAtStr) : null;
        return ResponseEntity.ok(ApiResponse.success("Override created",
                userPermissionService.createScopedOverride(userId, permission, expiresAt, currentUser)));
    }

    @DeleteMapping("/override/{overrideId}")
    public ResponseEntity<ApiResponse<Void>> deleteOverride(
            @PathVariable Long overrideId, Authentication auth) {
        User currentUser = authUtil.getCurrentUser(auth);
        userPermissionService.deleteOverride(overrideId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Override deleted", null));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> resetUserPermissionsToDefault(
            @PathVariable Long userId, Authentication auth) {
        User currentUser = authUtil.getCurrentUser(auth);
        userPermissionService.resetUserPermissionsToDefault(userId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("User permissions reset to role default", null));
    }
}