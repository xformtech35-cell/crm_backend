package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_user_permission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_permission_id")
    private Long userPermissionId;

    @Column(name = "user_id_fk", nullable = false)
    private Long userIdFk;

    @Column(name = "grp_perm", nullable = false)
    private String grpPerm;

    /** NULL = permanent override. When set and < NOW(), this override is ignored.
     *  Existing rows have NULL — treated as permanent (Part 0.6). */
    @Column(name = "expires_at", nullable = true)
    private LocalDateTime expiresAt;

    /** Which admin user granted this override. NULL for pre-existing rows. */
    @Column(name = "created_by_fk", nullable = true)
    private Long createdByFk;

    /** When this override was created. NULL for pre-existing rows (Part 0.6). */
    @Column(name = "created_at", nullable = true)
    private LocalDateTime createdAt;
}

