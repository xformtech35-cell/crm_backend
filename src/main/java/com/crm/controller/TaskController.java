package com.crm.controller;

import com.crm.dto.request.TaskRequest;
import com.crm.dto.response.ApiResponse;
import com.crm.entity.Task;
import com.crm.entity.User;
import com.crm.service.TaskService;
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
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Task>>> getAll(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Tasks fetched",
                taskService.getAllTasks(user.getUserid(), user.getRole())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Task>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Task fetched", taskService.getById(id)));
    }

    @GetMapping("/by-team/{teamId}")
    public ResponseEntity<ApiResponse<List<Task>>> getByTeam(@PathVariable Long teamId) {
        return ResponseEntity.ok(ApiResponse.success("Tasks fetched by team", taskService.getByTeam(teamId)));
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<Task>> create(
            @Valid @RequestPart("task") TaskRequest request,
            @RequestPart(value = "taskDoc", required = false) MultipartFile doc,
            Authentication auth) throws IOException {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Task created", taskService.create(request, user.getUserid(), doc)));
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<Task>> update(
            @PathVariable Long id,
            @Valid @RequestPart("task") TaskRequest request,
            @RequestPart(value = "taskDoc", required = false) MultipartFile doc) throws IOException {
        return ResponseEntity.ok(ApiResponse.success("Task updated", taskService.update(id, request, doc)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Task deleted", null));
    }
}
