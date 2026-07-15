package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "crm_xformsales_task")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "task_name")
    private String taskName;

    @Column(name = "task_assigned_member")
    private Long taskAssignedMember;

    @Column(name = "task_assigned_team")
    private Long taskAssignedTeam;

    @Column(name = "task_assign")
    private String taskAssign;

    @Column(name = "task_assigned_to")
    private Long taskAssignedTo;

    @Column(name = "task_start_date")
    private LocalDate taskStartDate;

    @Column(name = "task_completed_date")
    private LocalDate taskCompletedDate;

    @Column(name = "task_due_date")
    private LocalDate taskDueDate;

    @Column(name = "task_related_to")
    private String taskRelatedTo;

    @Column(name = "task_description", columnDefinition = "TEXT")
    private String taskDescription;

    @Column(name = "task_priority")
    private String taskPriority;

    @Column(name = "task_percentage_completed")
    private Integer taskPercentageCompleted;

    @Column(name = "task_doc")
    private String taskDoc;

    @Column(name = "user_id_fk")
    private Long userIdFk;

    // ─── New fields ───
    @Column(name = "task_type")
    private String taskType;

    @Column(name = "task_phone")
    private String taskPhone;

    @Column(name = "task_email")
    private String taskEmail;

    @Column(name = "task_project_id")
    private Long taskProjectId;

    @Column(name = "task_created_by")
    private String taskCreatedBy;

    @Column(name = "task_expected_completion")
    private LocalDate taskExpectedCompletion;

    @Column(name = "task_period")
    private String taskPeriod;

    @Column(name = "task_time_spent_minutes")
    private Integer taskTimeSpentMinutes;
}
