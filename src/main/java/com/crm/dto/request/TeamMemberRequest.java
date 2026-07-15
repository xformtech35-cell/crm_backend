package com.crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TeamMemberRequest {
    @NotBlank(message = "Name is required")
    private String teamMemberName;
    private String teamMemberMobile;
    @NotBlank(message = "Email is required")
    private String teamMemberEmail;
    private Long teamMemberRole;
    private String password;
}
