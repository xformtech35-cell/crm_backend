package com.crm.controller;

import com.crm.dto.response.ApiResponse;
import com.crm.service.CalendarService;
import com.crm.entity.User;
import com.crm.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;
    private final AuthUtil authUtil;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllEvents(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        return ResponseEntity.ok(ApiResponse.success("Calendar events fetched",
                calendarService.getAllCalendarEvents(companyAdminId, user.getRole())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        String dateStr = date != null ? date.toString() : LocalDate.now().toString();
        return ResponseEntity.ok(ApiResponse.success("Calendar events fetched",
                calendarService.getCalendarEvents(dateStr, companyAdminId, user.getRole())));
    }

    @GetMapping("/{date}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEventsByDate(@PathVariable String date, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        return ResponseEntity.ok(ApiResponse.success("Calendar events fetched",
                calendarService.getCalendarEvents(date, companyAdminId, user.getRole())));
    }
}
