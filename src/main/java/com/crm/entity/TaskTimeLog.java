package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_task_time_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaskTimeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "time_log_id")
    private Long timeLogId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "user_id_fk")
    private Long userIdFk;
}
