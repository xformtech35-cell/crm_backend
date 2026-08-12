package com.crm.dto.request;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Data
public class CalendarEventRequest {
    private String title;
    private String description;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private Boolean isAllDay;
    private String eventType; // REMINDER, MEETING, FOLLOW_UP, TASK
    private String priority;  // LOW, MEDIUM, HIGH, URGENT
    private String category;  // SALES, DEMO, FOLLOW_UP, INTERNAL, CLIENT_CALL
    private String location;
    private String meetingLink;

    private Boolean reminderEnabled;
    private Integer reminderMinutes; // 0, 5, 10, 15, 30, 60, 1440

    private String recurrenceType; // NONE, DAILY, WEEKLY, MONTHLY
    private Integer recurrenceInterval;
    private LocalDate recurrenceEndDate;

    private Long assignedTo;
    private Long leadIdFk;
    private Long contactIdFk;
    private Long opportunityIdFk;
    private Long taskIdFk;

    private List<Long> attendeeUserIds;
}
