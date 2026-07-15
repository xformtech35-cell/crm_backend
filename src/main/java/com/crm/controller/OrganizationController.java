package com.crm.controller;

import com.crm.dto.request.OrganizationRequest;
import com.crm.dto.response.ApiResponse;
import com.crm.entity.Organization;
import com.crm.entity.User;
import com.crm.service.OrganizationService;
import com.crm.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Organization>>> getAll(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Organizations fetched",
                organizationService.getAllOrganizations(user.getUserid(), user.getRole())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Organization>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Organization fetched", organizationService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Organization>> create(@Valid @RequestBody OrganizationRequest request, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Organization created", organizationService.create(request, user.getUserid())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Organization>> update(@PathVariable Long id, @Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Organization updated", organizationService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        organizationService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Organization deleted", null));
    }
}
