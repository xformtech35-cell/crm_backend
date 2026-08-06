package com.crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TeamRequest {
    @NotBlank(message = "Team name is required")
    private String teamName;

    private Long teamLeadId;
}
