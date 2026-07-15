package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crm_xformsales_create_team")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "create_team_id")
    private Long createTeamId;

    @Column(name = "team_id_fk")
    private Long teamIdFk;

    @Column(name = "team_member_id_fk")
    private Long teamMemberIdFk;

    @Column(name = "role_id_fk")
    private Long roleIdFk;

    @Column(name = "user_id_fk")
    private Long userIdFk;
}
