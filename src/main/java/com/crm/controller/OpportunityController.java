package com.crm.controller;

import com.crm.dto.request.OpportunityRequest;
import com.crm.dto.response.ApiResponse;
import com.crm.entity.Opportunity;
import com.crm.entity.User;
import com.crm.service.OpportunityService;
import com.crm.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/opportunities")
@RequiredArgsConstructor
public class OpportunityController {

    private final OpportunityService opportunityService;
    private final AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Opportunity>>> getAll(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Opportunities fetched",
                opportunityService.getAllOpportunities(user.getUserid(), user.getRole())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Opportunity>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Opportunity fetched", opportunityService.getById(id)));
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<Opportunity>> create(
            @Valid @RequestPart("opportunity") OpportunityRequest request,
            @RequestPart(value = "oppDoc", required = false) MultipartFile doc,
            Authentication auth) throws IOException {
        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        return ResponseEntity.ok(ApiResponse.success("Opportunity created", opportunityService.create(request, companyAdminId, doc)));
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<Opportunity>> update(
            @PathVariable Long id,
            @Valid @RequestPart("opportunity") OpportunityRequest request,
            @RequestPart(value = "oppDoc", required = false) MultipartFile doc) throws IOException {
        return ResponseEntity.ok(ApiResponse.success("Opportunity updated", opportunityService.update(id, request, doc)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        opportunityService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Opportunity deleted", null));
    }
}
