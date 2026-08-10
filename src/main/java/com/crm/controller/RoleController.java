package com.crm.controller;

import com.crm.dto.request.RoleRequest;
import com.crm.dto.response.ApiResponse;
import com.crm.entity.Permission;
import com.crm.entity.Role;
import com.crm.entity.User;
import com.crm.service.RoleService;
import com.crm.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Role>>> getAll(
            @RequestParam(required = false) Long companyAdminId, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Roles fetched", roleService.getAllRoles(user, companyAdminId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Role>> getById(@PathVariable Long id, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Role fetched", roleService.getById(id, user)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Role>> create(@Valid @RequestBody RoleRequest request, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Role created", roleService.create(request, user)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Role>> update(@PathVariable Long id, @Valid @RequestBody RoleRequest request, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Role updated", roleService.update(id, request, user)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        roleService.delete(id, user);
        return ResponseEntity.ok(ApiResponse.success("Role deleted", null));
    }

    @GetMapping("/{id}/permissions")
    public ResponseEntity<ApiResponse<List<Permission>>> getPermissions(@PathVariable Long id, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Permissions fetched", roleService.getPermissions(id, user)));
    }

    @PostMapping("/{id}/permissions")
    public ResponseEntity<ApiResponse<List<Permission>>> savePermissions(
            @PathVariable Long id, @RequestBody Map<String, List<String>> body, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Permissions saved",
                roleService.savePermissions(id, body.get("permissions"), user)));
    }

    @DeleteMapping("/permissions/{permissionId}")
    public ResponseEntity<ApiResponse<Void>> deletePermission(@PathVariable Long permissionId, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        roleService.deletePermission(permissionId, user);
        return ResponseEntity.ok(ApiResponse.success("Permission deleted", null));
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<ApiResponse<Role>> cloneRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        String newName = body.get("roleName");
        if (newName == null || newName.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("roleName is required"));
        }
        return ResponseEntity.ok(ApiResponse.success("Role cloned", roleService.cloneRole(id, newName, user)));
    }

    @GetMapping("/{id}/user-count")
    public ResponseEntity<ApiResponse<Long>> getUserCount(@PathVariable Long id, Authentication auth) {
        authUtil.getCurrentUser(auth); // auth check
        return ResponseEntity.ok(ApiResponse.success("User count fetched", roleService.getUserCountByRole(id)));
    }
}
