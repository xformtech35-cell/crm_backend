package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_integration_config")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IntegrationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // e.g. "INDIAMART", "WHATSAPP", "TRADEINDIA"

    @Column(name = "user_id_fk", nullable = false)
    private Long userIdFk; // The Admin / Company owning this config

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "api_url")
    private String apiUrl;

    @Column(name = "additional_config", columnDefinition = "TEXT")
    private String additionalConfig; // For storing custom parameters as JSON/String

    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime;

    @Column(name = "sync_status")
    private String syncStatus; // SUCCESS, FAILURE

    @Column(name = "leads_pulled")
    private Integer leadsPulled;

    @Column(name = "auto_assign_user_id")
    private Long autoAssignUserId; // Automatically assign incoming leads to this team member
}
