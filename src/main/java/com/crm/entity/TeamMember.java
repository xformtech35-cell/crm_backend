package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crm_xformsales_team_member")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@org.hibernate.annotations.SQLDelete(sql = "UPDATE crm_xformsales_team_member SET is_deleted = true, deleted_at = NOW() WHERE team_member_id = ?")
@org.hibernate.annotations.SQLRestriction("is_deleted = false")
public class TeamMember {

    @Builder.Default
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_member_id")
    private Long teamMemberId;

    @Column(name = "team_member_name")
    private String teamMemberName;

    @Column(name = "team_member_role")
    private Long teamMemberRole;

    @Column(name = "team_member_mobile")
    private String teamMemberMobile;

    @Column(name = "team_member_email")
    private String teamMemberEmail;

    @Column(name = "user_id_fk")
    private Long userIdFk;

    @Transient
    private Long userid;

    @Transient
    private String username;

    @Transient
    private String userEmail;
}
