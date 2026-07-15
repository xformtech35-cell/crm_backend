package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crm_xformsales_permission")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long permissionId;

    @Column(name = "role_id_fk")
    private Long roleIdFk;

    @Column(name = "grp_perm")
    private String grpPerm;
}
