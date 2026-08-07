package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crm_xformsales_team")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@org.hibernate.annotations.SQLDelete(sql = "UPDATE crm_xformsales_team SET is_deleted = true, deleted_at = NOW() WHERE team_id = ?")
@org.hibernate.annotations.SQLRestriction("(is_deleted = false OR is_deleted IS NULL)")
public class Team {

    @Builder.Default
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "team_name")
    private String teamName;

    @Column(name = "user_id_fk")
    private Long userIdFk;

    @Column(name = "team_lead_id_fk")
    private Long teamLeadId;
}
