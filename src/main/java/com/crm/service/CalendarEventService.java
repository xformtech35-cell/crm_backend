package com.crm.service;

import com.crm.dto.request.CalendarEventRequest;
import com.crm.entity.*;
import com.crm.repository.*;
import com.crm.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;
    private final CalendarAttendeeRepository calendarAttendeeRepository;
    private final CalendarNotificationRepository calendarNotificationRepository;
    private final UserRepository userRepository;
    private final AuthUtil authUtil;

    @Transactional
    public CalendarEvent createEvent(CalendarEventRequest req, User currentUser, Long companyAdminId) {
        if (req.getTitle() == null || req.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required for calendar event");
        }
        if (req.getStartDatetime() != null && req.getEndDatetime() != null) {
            if (req.getEndDatetime().isBefore(req.getStartDatetime())) {
                throw new IllegalArgumentException("End time cannot be before start time");
            }
        }

        CalendarEvent event = CalendarEvent.builder()
                .userIdFk(companyAdminId)
                .title(req.getTitle().trim())
                .description(req.getDescription())
                .startDatetime(req.getStartDatetime() != null ? req.getStartDatetime() : LocalDateTime.now())
                .endDatetime(req.getEndDatetime() != null ? req.getEndDatetime() : LocalDateTime.now().plusHours(1))
                .isAllDay(req.getIsAllDay() != null ? req.getIsAllDay() : false)
                .eventType(req.getEventType() != null ? req.getEventType() : "REMINDER")
                .status("UPCOMING")
                .priority(req.getPriority() != null ? req.getPriority() : "MEDIUM")
                .category(req.getCategory() != null ? req.getCategory() : "SALES")
                .location(req.getLocation())
                .meetingLink(req.getMeetingLink())
                .reminderEnabled(req.getReminderEnabled() != null ? req.getReminderEnabled() : true)
                .reminderMinutes(req.getReminderMinutes() != null ? req.getReminderMinutes() : 15)
                .recurrenceType(req.getRecurrenceType() != null ? req.getRecurrenceType() : "NONE")
                .recurrenceInterval(req.getRecurrenceInterval() != null ? req.getRecurrenceInterval() : 1)
                .recurrenceEndDate(req.getRecurrenceEndDate())
                .createdBy(currentUser.getUserid())
                .assignedTo(req.getAssignedTo() != null ? req.getAssignedTo() : currentUser.getUserid())
                .leadIdFk(req.getLeadIdFk())
                .contactIdFk(req.getContactIdFk())
                .opportunityIdFk(req.getOpportunityIdFk())
                .taskIdFk(req.getTaskIdFk())
                .build();

        CalendarEvent saved = calendarEventRepository.save(event);

        // Attendees
        if (req.getAttendeeUserIds() != null && !req.getAttendeeUserIds().isEmpty()) {
            for (Long uid : req.getAttendeeUserIds()) {
                CalendarAttendee attendee = CalendarAttendee.builder()
                        .eventIdFk(saved.getId())
                        .userIdFk(uid)
                        .rsvpStatus(uid.equals(currentUser.getUserid()) ? "ACCEPTED" : "PENDING")
                        .build();
                calendarAttendeeRepository.save(attendee);
            }
        }

        // Schedule Notification
        scheduleNotificationForEvent(saved);

        return saved;
    }

    public List<Map<String, Object>> getEventsInRange(LocalDateTime start, LocalDateTime end, User currentUser, Long companyAdminId) {
        List<CalendarEvent> events;
        if (authUtil.isSuperAdmin(currentUser.getRole())) {
            events = calendarEventRepository.findAllEventsInRange(start, end);
        } else {
            events = calendarEventRepository.findEventsInRange(companyAdminId, start, end);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (CalendarEvent e : events) {
            Map<String, Object> map = buildEventDto(e);
            result.add(map);
        }
        return result;
    }

    public Map<String, Object> getEventById(Long id, User currentUser, Long companyAdminId) {
        CalendarEvent event = calendarEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Calendar event not found with ID: " + id));

        if (!authUtil.isSuperAdmin(currentUser.getRole()) && !event.getUserIdFk().equals(companyAdminId)) {
            throw new SecurityException("Access denied to calendar event");
        }

        Map<String, Object> dto = buildEventDto(event);

        // Fetch Attendees
        List<CalendarAttendee> attendees = calendarAttendeeRepository.findByEventIdFk(id);
        List<Map<String, Object>> attendeeList = new ArrayList<>();
        for (CalendarAttendee a : attendees) {
            Map<String, Object> attMap = new HashMap<>();
            attMap.put("id", a.getId());
            attMap.put("userId", a.getUserIdFk());
            attMap.put("rsvpStatus", a.getRsvpStatus());
            userRepository.findById(a.getUserIdFk()).ifPresent(u -> {
                attMap.put("username", u.getUsername());
                attMap.put("userEmail", u.getUserEmail());
            });
            attendeeList.add(attMap);
        }
        dto.put("attendees", attendeeList);

        return dto;
    }

    @Transactional
    public CalendarEvent updateEvent(Long id, CalendarEventRequest req, User currentUser, Long companyAdminId) {
        CalendarEvent event = calendarEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Calendar event not found with ID: " + id));

        if (!authUtil.isSuperAdmin(currentUser.getRole()) && !event.getUserIdFk().equals(companyAdminId)) {
            throw new SecurityException("Access denied to modify calendar event");
        }

        if (req.getTitle() != null && !req.getTitle().trim().isEmpty()) {
            event.setTitle(req.getTitle().trim());
        }
        if (req.getDescription() != null) event.setDescription(req.getDescription());
        if (req.getStartDatetime() != null) event.setStartDatetime(req.getStartDatetime());
        if (req.getEndDatetime() != null) event.setEndDatetime(req.getEndDatetime());
        if (req.getIsAllDay() != null) event.setIsAllDay(req.getIsAllDay());
        if (req.getEventType() != null) event.setEventType(req.getEventType());
        if (req.getPriority() != null) event.setPriority(req.getPriority());
        if (req.getCategory() != null) event.setCategory(req.getCategory());
        if (req.getLocation() != null) event.setLocation(req.getLocation());
        if (req.getMeetingLink() != null) event.setMeetingLink(req.getMeetingLink());
        if (req.getReminderEnabled() != null) event.setReminderEnabled(req.getReminderEnabled());
        if (req.getReminderMinutes() != null) event.setReminderMinutes(req.getReminderMinutes());

        if (req.getAssignedTo() != null) event.setAssignedTo(req.getAssignedTo());
        if (req.getLeadIdFk() != null) event.setLeadIdFk(req.getLeadIdFk());
        if (req.getContactIdFk() != null) event.setContactIdFk(req.getContactIdFk());
        if (req.getOpportunityIdFk() != null) event.setOpportunityIdFk(req.getOpportunityIdFk());
        if (req.getTaskIdFk() != null) event.setTaskIdFk(req.getTaskIdFk());

        CalendarEvent updated = calendarEventRepository.save(event);

        if (req.getAttendeeUserIds() != null) {
            calendarAttendeeRepository.deleteByEventIdFk(id);
            for (Long uid : req.getAttendeeUserIds()) {
                CalendarAttendee attendee = CalendarAttendee.builder()
                        .eventIdFk(id)
                        .userIdFk(uid)
                        .rsvpStatus(uid.equals(currentUser.getUserid()) ? "ACCEPTED" : "PENDING")
                        .build();
                calendarAttendeeRepository.save(attendee);
            }
        }

        scheduleNotificationForEvent(updated);
        return updated;
    }

    @Transactional
    public void deleteEvent(Long id, Boolean deleteSeries, User currentUser, Long companyAdminId) {
        CalendarEvent event = calendarEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Calendar event not found"));

        if (!authUtil.isSuperAdmin(currentUser.getRole()) && !event.getUserIdFk().equals(companyAdminId)) {
            throw new SecurityException("Access denied to delete calendar event");
        }

        if (Boolean.TRUE.equals(deleteSeries) && event.getRecurrenceParentId() != null) {
            List<CalendarEvent> series = calendarEventRepository.findByRecurrenceParentId(event.getRecurrenceParentId());
            for (CalendarEvent se : series) {
                se.setStatus("CANCELLED");
                se.setCancelledAt(LocalDateTime.now());
                se.setIsDeleted(true);
                se.setDeletedAt(LocalDateTime.now());
                calendarEventRepository.save(se);
            }
        } else {
            event.setStatus("CANCELLED");
            event.setCancelledAt(LocalDateTime.now());
            event.setIsDeleted(true);
            event.setDeletedAt(LocalDateTime.now());
            calendarEventRepository.save(event);
        }
    }


    @Transactional
    public CalendarEvent snoozeEvent(Long id, int minutes, User currentUser) {
        CalendarEvent event = calendarEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Calendar event not found"));

        LocalDateTime snoozedTime = LocalDateTime.now().plusMinutes(minutes);
        event.setStatus("SNOOZED");
        event.setSnoozedUntil(snoozedTime);
        CalendarEvent saved = calendarEventRepository.save(event);

        // Schedule new notification for snoozed time
        CalendarNotification notif = CalendarNotification.builder()
                .eventIdFk(id)
                .userIdFk(currentUser.getUserid())
                .scheduledAt(snoozedTime)
                .status("PENDING")
                .channel("IN_APP")
                .title("Snoozed Reminder: " + event.getTitle())
                .message("Snoozed for " + minutes + " minutes. " + (event.getDescription() != null ? event.getDescription() : ""))
                .build();
        calendarNotificationRepository.save(notif);

        return saved;
    }

    @Transactional
    public CalendarEvent completeEvent(Long id, User currentUser) {
        CalendarEvent event = calendarEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Calendar event not found"));

        event.setStatus("COMPLETED");
        event.setCompletedAt(LocalDateTime.now());
        return calendarEventRepository.save(event);
    }

    @Transactional
    public Map<String, Object> updateRsvp(Long eventId, String rsvpStatus, User currentUser) {
        CalendarAttendee attendee = calendarAttendeeRepository.findByEventIdFkAndUserIdFk(eventId, currentUser.getUserid())
                .orElseGet(() -> CalendarAttendee.builder()
                        .eventIdFk(eventId)
                        .userIdFk(currentUser.getUserid())
                        .build());

        attendee.setRsvpStatus(rsvpStatus.toUpperCase());
        calendarAttendeeRepository.save(attendee);

        Map<String, Object> res = new HashMap<>();
        res.put("eventId", eventId);
        res.put("userId", currentUser.getUserid());
        res.put("rsvpStatus", attendee.getRsvpStatus());
        return res;
    }

    private void scheduleNotificationForEvent(CalendarEvent event) {
        if (Boolean.TRUE.equals(event.getReminderEnabled()) && event.getReminderMinutes() != null) {
            LocalDateTime scheduledAt = event.getStartDatetime().minusMinutes(event.getReminderMinutes());
            if (scheduledAt.isBefore(LocalDateTime.now())) {
                scheduledAt = LocalDateTime.now(); // Trigger immediately if past due
            }

            CalendarNotification notif = CalendarNotification.builder()
                    .eventIdFk(event.getId())
                    .userIdFk(event.getAssignedTo() != null ? event.getAssignedTo() : event.getUserIdFk())
                    .scheduledAt(scheduledAt)
                    .status("PENDING")
                    .channel("IN_APP")
                    .title("Calendar Reminder: " + event.getTitle())
                    .message("Starts in " + event.getReminderMinutes() + " mins. " + (event.getDescription() != null ? event.getDescription() : ""))
                    .build();
            calendarNotificationRepository.save(notif);
        }
    }

    private Map<String, Object> buildEventDto(CalendarEvent e) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", e.getId());
        map.put("title", e.getTitle());
        map.put("description", e.getDescription());
        map.put("startDatetime", e.getStartDatetime());
        map.put("endDatetime", e.getEndDatetime());
        map.put("isAllDay", e.getIsAllDay());
        map.put("eventType", e.getEventType());
        map.put("status", e.getStatus());
        map.put("priority", e.getPriority());
        map.put("category", e.getCategory());
        map.put("location", e.getLocation());
        map.put("meetingLink", e.getMeetingLink());
        map.put("reminderEnabled", e.getReminderEnabled());
        map.put("reminderMinutes", e.getReminderMinutes());
        map.put("recurrenceType", e.getRecurrenceType());
        map.put("assignedTo", e.getAssignedTo());
        map.put("leadIdFk", e.getLeadIdFk());
        map.put("contactIdFk", e.getContactIdFk());
        map.put("opportunityIdFk", e.getOpportunityIdFk());
        map.put("taskIdFk", e.getTaskIdFk());
        map.put("snoozedUntil", e.getSnoozedUntil());
        map.put("completedAt", e.getCompletedAt());
        return map;
    }
}
