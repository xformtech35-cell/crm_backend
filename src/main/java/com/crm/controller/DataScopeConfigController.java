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
        return ResponseEntity.ok(ApiResponse.success("Data scope configurations fetched", dataScopeConfigService.getAllConfigs(companyAdminId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DataScopeConfig>> saveOne(@Valid @RequestBody DataScopeConfigRequest request, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        return ResponseEntity.ok(ApiResponse.success("Data scope configuration saved", dataScopeConfigService.saveConfig(request, companyAdminId)));
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<DataScopeConfig>>> saveBatch(@Valid @RequestBody List<DataScopeConfigRequest> requests, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        return ResponseEntity.ok(ApiResponse.success("Data scope configurations updated", dataScopeConfigService.saveBatchConfigs(requests, companyAdminId)));
    }
}
