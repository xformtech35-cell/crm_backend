package com.crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TaskRequest {
    @NotBlank(message = "Task Name is required")
    private String taskName;

    @NotNull(message = "Assignee is required")
    private Long taskAssignedMember;

    private Long taskAssignedTeam;
    private String taskAssign;
    private Long taskAssignedTo;
    private String taskStartDate;
    private String taskCompletedDate;

    @NotBlank(message = "Due Date is required")
    private String taskDueDate;

    @NotBlank(message = "Related Lead or Project reference is required")
    private String taskRelatedTo;

    private String taskDescription;
    private String taskPriority;
    private Integer taskPercentageCompleted;

    @NotBlank(message = "Task Type is required")
    private String taskType;

    private String taskPhone;
    private String taskEmail;
    private Long taskProjectId;
    private String taskExpectedCompletion;
    private String taskPeriod;
    private Integer taskTimeSpentMinutes;
}

