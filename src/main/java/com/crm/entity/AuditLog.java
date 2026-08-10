package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Audit trail for permission, scope, role, and team-assignment changes.
 * Table: crm_audit_log
 */
@Entity
@Table(name = "crm_audit_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "actor_user_id", nullable = true)
    private Long actorUserId;

    @Column(name = "action_type", length = 50, nullable = true)
    private String actionType;

    @Column(name = "entity_type", length = 50, nullable = true)
    private String entityType;

    @Column(name = "entity_id", nullable = true)
    private Long entityId;

    @Column(name = "old_value", columnDefinition = "TEXT", nullable = true)
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT", nullable = true)
    private String newValue;

    @Column(name = "timestamp", nullable = true)
    private LocalDateTime timestamp;

    @Column(name = "company_admin_id_fk", nullable = true)
    private Long companyAdminIdFk;
}
