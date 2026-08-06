package com.crm.controller;

import com.crm.dto.request.TeamRequest;
import com.crm.dto.response.ApiResponse;
import com.crm.entity.Team;
import com.crm.service.TeamService;
import com.crm.entity.User;
import com.crm.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Team>>> getAll(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        return ResponseEntity.ok(ApiResponse.success("Teams fetched", teamService.getAllTeams(user, companyAdminId, user.getRole())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Team>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Team fetched", teamService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Team>> create(@Valid @RequestBody TeamRequest request, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        return ResponseEntity.ok(ApiResponse.success("Team created", teamService.create(request, companyAdminId, user.getRole())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Team>> update(@PathVariable Long id, @Valid @RequestBody TeamRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Team updated", teamService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        teamService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Team deleted", null));
    }
}
