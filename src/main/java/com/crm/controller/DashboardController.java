package com.crm.controller;

import com.crm.dto.response.ApiResponse;
import com.crm.dto.response.DashboardResponse;
import com.crm.entity.User;
import com.crm.service.DashboardService;
import com.crm.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Dashboard data fetched", dashboardService.getDashboardStats(user)));
    }
}
