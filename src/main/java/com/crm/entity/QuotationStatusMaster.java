package com.crm.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "crm_quotation_status_master")
@Data
public class QuotationStatusMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "status_name")
    private String statusName;

    private String description;

    private Boolean active = true;

    @Column(name = "user_id_fk")
    private Long userIdFk;
}
