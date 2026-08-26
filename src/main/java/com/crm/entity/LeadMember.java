package com.crm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_xformsales_lead_member", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"lead_id_fk", "team_member_id_fk"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lead_id_fk", nullable = false)
    private Long leadIdFk;

    @Column(name = "team_member_id_fk", nullable = false)
    private Long teamMemberIdFk;

    @Column(name = "is_primary")
    @Builder.Default
    private Integer isPrimary = 1;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
