package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crm_xformsales_team")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "team_name")
    private String teamName;

    @Column(name = "user_id_fk")
    private Long userIdFk;
}
