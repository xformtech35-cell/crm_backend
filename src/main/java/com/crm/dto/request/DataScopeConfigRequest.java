package com.crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataScopeConfigRequest {

    private Long roleIdFk;

    private Long userIdFk;

    @NotBlank(message = "Module name is required")
    private String moduleName;

    @NotBlank(message = "Scope mode is required")
    private String scopeMode; // "ALL_DATA", "TEAM_DATA", "OWN_DATA_ONLY"
}
