package com.crm.controller;

import com.crm.dto.request.DataScopeConfigRequest;
import com.crm.dto.response.ApiResponse;
import com.crm.entity.DataScopeConfig;
import com.crm.entity.User;
import com.crm.service.DataScopeConfigService;
import com.crm.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data-scope-configs")
@RequiredArgsConstructor
public class DataScopeConfigController {

    private final DataScopeConfigService dataScopeConfigService;
    private final AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DataScopeConfig>>> getAll(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        return ResponseEntity.ok(ApiResponse.success("Data scope configurations fetched",
                dataScopeConfigService.getAllConfigs(companyAdminId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DataScopeConfig>> saveOne(
            @Valid @RequestBody DataScopeConfigRequest request, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        return ResponseEntity.ok(ApiResponse.success("Data scope configuration saved",
                dataScopeConfigService.saveConfig(request, companyAdminId, user.getUserid())));
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<DataScopeConfig>>> saveBatch(
            @Valid @RequestBody List<DataScopeConfigRequest> requests, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        return ResponseEntity.ok(ApiResponse.success("Data scope configurations updated",
                dataScopeConfigService.saveBatchConfigs(requests, companyAdminId, user.getUserid())));
    }

    /**
     * Bulk-set all modules for a role to the same scope mode.
     * Used by the "Set All Modules" button in Data Access Config UI.
     * Body: { "roleId": 123, "scopeMode": "TEAM_DATA" }
     */
    @PostMapping("/set-all")
    public ResponseEntity<ApiResponse<List<DataScopeConfig>>> setAllModules(
            @RequestBody Map<String, String> body, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        Long roleId = Long.parseLong(body.get("roleId"));
        String scopeMode = body.get("scopeMode");
        return ResponseEntity.ok(ApiResponse.success("All modules updated",
                dataScopeConfigService.saveAllModulesForRole(roleId, scopeMode, companyAdminId, user.getUserid())));
    }

    /**
     * Get scope overrides for a specific user (user-level override rows in crm_data_scope_config).
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<DataScopeConfig>>> getUserConfigs(
            @PathVariable Long userId, Authentication auth) {
        authUtil.getCurrentUser(auth);
        List<DataScopeConfig> configs = dataScopeConfigService.getAllConfigs(null)
                .stream().filter(c -> userId.equals(c.getUserIdFk())).toList();
        return ResponseEntity.ok(ApiResponse.success("User scope configs fetched", configs));
    }
}
