package com.crm.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "crm_leadgroups_master")
@Data
public class LeadGroupsMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupName; // optional

    private Boolean active = true;

    @Column(name = "user_id_fk")
    private Long userIdFk;
}