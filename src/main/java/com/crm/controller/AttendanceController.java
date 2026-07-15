package com.crm.controller;

import com.crm.dto.request.AttendanceRequest;
import com.crm.dto.response.ApiResponse;
import com.crm.entity.Attendance;
import com.crm.entity.User;
import com.crm.service.AttendanceService;
import com.crm.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AuthUtil authUtil;

    @PostMapping("/punch-in")
    public ResponseEntity<ApiResponse<Attendance>> punchIn(@RequestBody AttendanceRequest request, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        Long userIdFk = authUtil.isAdmin(user.getRole()) ? user.getUserid() : null;
        Attendance attendance = attendanceService.punchIn(user.getUserid(), userIdFk, request.getLocation(), request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Punched in", attendance));
    }

    @PostMapping("/punch-out")
    public ResponseEntity<ApiResponse<Attendance>> punchOut(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Punched out", attendanceService.punchOut(user.getUserid())));
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<Attendance>> getToday(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Today's attendance", attendanceService.getTodayAttendance(user.getUserid())));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Attendance>>> getHistory(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        if (authUtil.isSuperAdmin(user.getRole())) {
            return ResponseEntity.ok(ApiResponse.success("Attendance history", attendanceService.getAll()));
        }
        if (authUtil.isAdmin(user.getRole())) {
            return ResponseEntity.ok(ApiResponse.success("Attendance history", attendanceService.getAllHistoryByUserIdFk(user.getUserid())));
        }
        return ResponseEntity.ok(ApiResponse.success("Attendance history", attendanceService.getHistoryByUser(user.getUserid())));
    }
}
