package com.crm.controller;

import com.crm.dto.request.TaskTimeLogRequest;
import com.crm.dto.response.ApiResponse;
import com.crm.entity.TaskTimeLog;
import com.crm.entity.User;
import com.crm.service.TaskTimeLogService;
import com.crm.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/task-time")
@RequiredArgsConstructor
public class TaskTimeLogController {

    private final TaskTimeLogService taskTimeLogService;
    private final AuthUtil authUtil;

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<TaskTimeLog>> startTimer(@RequestBody TaskTimeLogRequest request, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        Long userIdFk = authUtil.isAdmin(user.getRole()) ? user.getUserid() : null; // Usually determined by user's admin link, assuming null for now if not admin, or fetch from TeamMember
        TaskTimeLog log = taskTimeLogService.startTimer(request.getTaskId(), user.getUserid(), userIdFk, request.getNote());
        return ResponseEntity.ok(ApiResponse.success("Timer started", log));
    }

    @PostMapping("/stop/{logId}")
    public ResponseEntity<ApiResponse<TaskTimeLog>> stopTimer(@PathVariable Long logId) {
        return ResponseEntity.ok(ApiResponse.success("Timer stopped", taskTimeLogService.stopTimer(logId)));
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<ApiResponse<List<TaskTimeLog>>> getTaskLogs(@PathVariable Long taskId) {
        return ResponseEntity.ok(ApiResponse.success("Task logs", taskTimeLogService.getLogsByTaskId(taskId)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<TaskTimeLog>>> getUserLogs(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("User logs", taskTimeLogService.getLogsByUserId(userId)));
    }
}
