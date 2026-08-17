package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "crm_calendar_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id_fk", nullable = false)
    private Long userIdFk; // Tenant / Company Admin ID

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startDatetime;

    @Column(name = "end_datetime", nullable = false)
    private LocalDateTime endDatetime;

    @Column(name = "is_all_day")
    @Builder.Default
    private Boolean isAllDay = false;

    @Column(name = "event_type", length = 50)
    @Builder.Default
    private String eventType = "REMINDER"; // REMINDER, MEETING, FOLLOW_UP, TASK

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "UPCOMING"; // UPCOMING, REMINDER_SENT, DUE, COMPLETED, CANCELLED, OVERDUE, SNOOZED

    @Column(name = "priority", length = 50)
    @Builder.Default
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH, URGENT

    @Column(name = "category", length = 50)
    @Builder.Default
    private String category = "SALES"; // SALES, DEMO, FOLLOW_UP, INTERNAL, CLIENT_CALL

    @Column(name = "location")
    private String location;

    @Column(name = "meeting_link", length = 500)
    private String meetingLink;

    @Column(name = "reminder_enabled")
    @Builder.Default
    private Boolean reminderEnabled = true;

    @Column(name = "reminder_minutes")
    @Builder.Default
    private Integer reminderMinutes = 15; // 0, 5, 10, 15, 30, 60, 1440

    @Column(name = "recurrence_type", length = 50)
    @Builder.Default
    private String recurrenceType = "NONE"; // NONE, DAILY, WEEKLY, MONTHLY

    @Column(name = "recurrence_interval")
    @Builder.Default
    private Integer recurrenceInterval = 1;

    @Column(name = "recurrence_end_date")
    private LocalDate recurrenceEndDate;

    @Column(name = "recurrence_parent_id")
    private Long recurrenceParentId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "lead_id_fk")
    private Long leadIdFk;

    @Column(name = "contact_id_fk")
    private Long contactIdFk;

    @Column(name = "opportunity_id_fk")
    private Long opportunityIdFk;

    @Column(name = "task_id_fk")
    private Long taskIdFk;

    @Column(name = "snoozed_until")
    private LocalDateTime snoozedUntil;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = "UPCOMING";
        if (this.priority == null) this.priority = "MEDIUM";
        if (this.eventType == null) this.eventType = "REMINDER";
        if (this.isDeleted == null) this.isDeleted = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

