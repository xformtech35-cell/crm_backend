package com.crm.dto.request;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AttendanceRequest {
    private Long userId;
    private String location;
    private String status;
}
