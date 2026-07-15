package com.crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RoleRequest {
    @NotBlank(message = "Role name is required")
    private String roleName;
}
