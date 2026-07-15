package com.crm.controller;

import com.crm.dto.request.CreateTeamRequest;
import com.crm.dto.response.ApiResponse;
import com.crm.entity.CreateTeam;
import com.crm.service.CreateTeamService;
import com.crm.entity.User;
import com.crm.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/create-team")
@RequiredArgsConstructor
public class CreateTeamController {

    private final CreateTeamService createTeamService;
    private final AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CreateTeam>>> getAll(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        return ResponseEntity.ok(ApiResponse.success("Create team entries fetched", createTeamService.getAll(companyAdminId, user.getRole())));
    }

    @GetMapping("/by-team/{teamId}")
    public ResponseEntity<ApiResponse<List<CreateTeam>>> getByTeamId(@PathVariable Long teamId) {
        return ResponseEntity.ok(ApiResponse.success("Team members fetched", createTeamService.getByTeamId(teamId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CreateTeam>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Entry fetched", createTeamService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateTeam>> create(@Valid @RequestBody CreateTeamRequest request, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        return ResponseEntity.ok(ApiResponse.success("Team assignment created", createTeamService.create(request, companyAdminId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CreateTeam>> update(@PathVariable Long id, @Valid @RequestBody CreateTeamRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Team assignment updated", createTeamService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        createTeamService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Team assignment deleted", null));
    }
}
