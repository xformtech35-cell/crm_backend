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

    /** True for system-seeded roles (ADMIN, Team Lead) that cannot be deleted.
     *  Nullable — NULL treated as false for backward compat with existing rows. */
    @Column(name = "is_system_role", nullable = true)
    private Boolean isSystemRole;

    /** Display sort order: 1=ADMIN, 2=Team Lead, 3=Team Member, null=custom. */
    @Column(name = "role_level", nullable = true)
    private Integer roleLevel;

    /** If this role was cloned, points to the source role_id. Null otherwise. */
    @Column(name = "cloned_from_role_id", nullable = true)
    private Long clonedFromRoleId;
}
