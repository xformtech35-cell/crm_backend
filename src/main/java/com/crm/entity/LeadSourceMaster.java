package com.crm.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "crm_leadsource_master")
@Data
public class LeadSourceMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sourceName;

    private String description;   // optional – for UI

    private Boolean active = true;

    @Column(name = "user_id_fk")
    private Long userIdFk;
}