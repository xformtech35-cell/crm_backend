package com.crm.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "crm_leadstatus_master")
@Data
public class LeadStatusMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String statusName;

    private String description;

    private Boolean active = true;

    @Column(name = "user_id_fk")
    private Long userIdFk;
}
