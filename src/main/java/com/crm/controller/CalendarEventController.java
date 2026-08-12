package com.crm.controller;

import com.crm.dto.request.CalendarEventRequest;
import com.crm.dto.response.ApiResponse;
import com.crm.entity.CalendarEvent;
import com.crm.entity.CalendarNotification;
import com.crm.entity.User;
import com.crm.repository.CalendarNotificationRepository;
import com.crm.service.CalendarEventService;
import com.crm.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar/events")
@RequiredArgsConstructor
public class CalendarEventController {

    private final CalendarEventService calendarEventService;
    private final CalendarNotificationRepository calendarNotificationRepository;
    private final AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Authentication auth) {

        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);

        LocalDateTime s = start != null ? start : LocalDateTime.now().minusDays(30);
        LocalDateTime e = end != null ? end : LocalDateTime.now().plusDays(90);

        List<Map<String, Object>> events = calendarEventService.getEventsInRange(s, e, user, companyAdminId);
        return ResponseEntity.ok(ApiResponse.success("Events fetched successfully", events));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEventById(@PathVariable Long id, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);

        Map<String, Object> dto = calendarEventService.getEventById(id, user, companyAdminId);
        return ResponseEntity.ok(ApiResponse.success("Event details fetched", dto));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CalendarEvent>> createEvent(@RequestBody CalendarEventRequest req, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);

        CalendarEvent created = calendarEventService.createEvent(req, user, companyAdminId);
        return ResponseEntity.ok(ApiResponse.success("Event created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CalendarEvent>> updateEvent(
            @PathVariable Long id,
            @RequestBody CalendarEventRequest req,
            Authentication auth) {

        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);

        CalendarEvent updated = calendarEventService.updateEvent(id, req, user, companyAdminId);
        return ResponseEntity.ok(ApiResponse.success("Event updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteEvent(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") Boolean deleteSeries,
            Authentication auth) {

        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);

        calendarEventService.deleteEvent(id, deleteSeries, user, companyAdminId);
        return ResponseEntity.ok(ApiResponse.success("Event deleted successfully", "Deleted"));
    }

    @PostMapping("/{id}/snooze")
    public ResponseEntity<ApiResponse<CalendarEvent>> snoozeEvent(
            @PathVariable Long id,
            @RequestParam(defaultValue = "15") int minutes,
            Authentication auth) {

        User user = authUtil.getCurrentUser(auth);
        CalendarEvent snoozed = calendarEventService.snoozeEvent(id, minutes, user);
        return ResponseEntity.ok(ApiResponse.success("Reminder snoozed for " + minutes + " minutes", snoozed));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<CalendarEvent>> completeEvent(@PathVariable Long id, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        CalendarEvent completed = calendarEventService.completeEvent(id, user);
        return ResponseEntity.ok(ApiResponse.success("Reminder completed", completed));
    }

    @PostMapping("/{id}/rsvp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateRsvp(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication auth) {

        User user = authUtil.getCurrentUser(auth);
        Map<String, Object> res = calendarEventService.updateRsvp(id, status, user);
        return ResponseEntity.ok(ApiResponse.success("RSVP updated to " + status, res));
    }

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<List<CalendarNotification>>> getNotifications(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        List<CalendarNotification> notifs = calendarNotificationRepository.findByUserIdFkOrderByScheduledAtDesc(user.getUserid());
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched", notifs));
    }

    @PostMapping("/notifications/{notifId}/read")
    public ResponseEntity<ApiResponse<String>> markNotificationRead(@PathVariable Long notifId, Authentication auth) {
        calendarNotificationRepository.findById(notifId).ifPresent(n -> {
            n.setStatus("READ");
            n.setReadAt(LocalDateTime.now());
            calendarNotificationRepository.save(n);
        });
        return ResponseEntity.ok(ApiResponse.success("Notification marked read", "Success"));
    }
}
