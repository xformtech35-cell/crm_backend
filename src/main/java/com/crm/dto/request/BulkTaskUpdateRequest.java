package com.crm.dto.request;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BulkTaskUpdateRequest {
    private List<Long> taskIds;
    private Long taskAssignedMember;
    private Long taskAssignedTeam;
    private String taskDueDate;
    private String taskPriority;
    private String taskAssign;
}
