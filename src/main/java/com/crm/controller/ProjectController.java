package com.crm.controller;

import com.crm.dto.request.ProjectRequest;
import com.crm.dto.response.ApiResponse;
import com.crm.entity.Project;
import com.crm.entity.User;
import com.crm.service.ProjectService;
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
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Project>>> getAll(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        return ResponseEntity.ok(ApiResponse.success("Projects fetched",
                projectService.getAllProjects(companyAdminId, user.getRole())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Project>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Project fetched", projectService.getById(id)));
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<Project>> create(
            @Valid @RequestPart("project") ProjectRequest request,
            @RequestPart(value = "projectDoc", required = false) MultipartFile doc,
            Authentication auth) throws IOException {
        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        return ResponseEntity.ok(ApiResponse.success("Project created", projectService.create(request, companyAdminId, doc)));
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<Project>> update(
            @PathVariable Long id,
            @Valid @RequestPart("project") ProjectRequest request,
            @RequestPart(value = "projectDoc", required = false) MultipartFile doc) throws IOException {
        return ResponseEntity.ok(ApiResponse.success("Project updated", projectService.update(id, request, doc)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Project deleted", null));
    }
}
