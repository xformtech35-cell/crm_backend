package com.crm.controller;

import com.crm.dto.response.ApiResponse;
import com.crm.entity.AuditLog;
import com.crm.entity.User;
import com.crm.service.AuditLogService;
import com.crm.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit-log")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLog(
            @RequestParam(required = false) Long companyAdminId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        User currentUser = authUtil.getCurrentUser(auth);
        if (!authUtil.isAnyAdmin(currentUser.getRole())) {
            throw new AccessDeniedException("Access denied: Admin or Super Admin required");
        }
        if (!authUtil.isSuperAdmin(currentUser.getRole())) {
            companyAdminId = authUtil.getCompanyAdminId(currentUser);
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> result = auditLogService.getAuditLog(companyAdminId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Audit log fetched", result));
    }
}