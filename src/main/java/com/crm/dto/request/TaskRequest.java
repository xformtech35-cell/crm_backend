package com.crm.dto.request;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TaskRequest {
    private String taskName;
    private Long taskAssignedMember;
    private Long taskAssignedTeam;
    private String taskAssign;
    private Long taskAssignedTo;
    private String taskStartDate;
    private String taskCompletedDate;
    private String taskDueDate;
    private String taskRelatedTo;
    private String taskDescription;
    private String taskPriority;
    private Integer taskPercentageCompleted;
    // New fields
    private String taskType;
    private String taskPhone;
    private String taskEmail;
    private Long taskProjectId;
    private String taskExpectedCompletion;
    private String taskPeriod;
    private Integer taskTimeSpentMinutes;
}
