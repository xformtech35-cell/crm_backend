package com.crm.dto.request;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TaskRequest {
    private String taskName;
    private Long taskAssignedMember;
    private Long taskAssignedTeam;
    private String taskAssign;
    private Long taskAssignedTo;
    private LocalDate taskStartDate;
    private LocalDate taskCompletedDate;
    private LocalDate taskDueDate;
    private String taskRelatedTo;
    private String taskDescription;
    private String taskPriority;
    private Integer taskPercentageCompleted;
    // New fields
    private String taskType;
    private String taskPhone;
    private String taskEmail;
    private Long taskProjectId;
    private LocalDate taskExpectedCompletion;
    private String taskPeriod;
    private Integer taskTimeSpentMinutes;
}
