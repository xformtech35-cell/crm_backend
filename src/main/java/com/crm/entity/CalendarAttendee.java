package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_calendar_attendees")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarAttendee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id_fk", nullable = false)
    private Long eventIdFk;

    @Column(name = "user_id_fk", nullable = false)
    private Long userIdFk;

    @Column(name = "rsvp_status", length = 50)
    @Builder.Default
    private String rsvpStatus = "PENDING"; // PENDING, ACCEPTED, DECLINED, TENTATIVE

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = LocalDateTime.now();
    }
}
