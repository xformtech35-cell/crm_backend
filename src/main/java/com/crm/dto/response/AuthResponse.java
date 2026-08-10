package com.crm.dto.response;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String token;
    private String tokenType;
    private Long userId;
    private String username;
    private String userEmail;
    private String role;
    private String companyName;
    private String teamLeadName;

    private List<String> permissions;
    private boolean integrationsAccess;
    private Long teamId; // Nullable — null for Admin/Super Admin
}
