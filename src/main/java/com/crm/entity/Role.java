package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crm_xformsales_role")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "role_name")
    private String roleName;

    @Column(name = "user_id_fk")
    private Long userIdFk; // The Admin / Company owning this role
}
