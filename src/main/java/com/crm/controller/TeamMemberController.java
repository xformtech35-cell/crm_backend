package com.crm.controller;

import com.crm.dto.request.TeamMemberRequest;
import com.crm.dto.response.ApiResponse;
import com.crm.entity.TeamMember;
import com.crm.entity.User;
import com.crm.service.TeamMemberService;
import com.crm.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team-members")
@RequiredArgsConstructor
public class TeamMemberController {

    private final TeamMemberService teamMemberService;
    private final AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TeamMember>>> getAll(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Team members fetched",
                teamMemberService.getAllTeamMembers(user.getUserid(), user.getRole())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamMember>> getById(@PathVariable Long id, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Team member fetched", teamMemberService.getById(id, user)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TeamMember>> create(@Valid @RequestBody TeamMemberRequest request, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Team member created", teamMemberService.create(request, user)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamMember>> update(@PathVariable Long id, @Valid @RequestBody TeamMemberRequest request, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Team member updated", teamMemberService.update(id, request, user)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        teamMemberService.delete(id, user);
        return ResponseEntity.ok(ApiResponse.success("Team member deleted", null));
    }
}
