package com.crm.dto.request;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TaskTimeLogRequest {
    private Long taskId;
    private Long userId;
    private String note;
}
