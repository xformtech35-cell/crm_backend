package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_calendar_notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id_fk", nullable = false)
    private Long eventIdFk;

    @Column(name = "user_id_fk", nullable = false)
    private Long userIdFk;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "PENDING"; // PENDING, SENT, READ, DISMISSED, SNOOZED

    @Column(name = "channel", length = 50)
    @Builder.Default
    private String channel = "IN_APP"; // IN_APP, WEBSOCKET, EMAIL

    @Column(name = "title")
    private String title;

    @Column(name = "message")
    private String message;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
    }
}
