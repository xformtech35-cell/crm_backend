package com.crm.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "crm_negotiation_revision")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NegotiationRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "negotiation_id")
    private Long negotiationId;

    @Column(name = "lead_id_fk")
    private Long leadIdFk;

    @Column(name = "quotation_no")
    private String quotationNo;

    @Column(name = "quotation_revision")
    private String quotationRevision;

    @Column(name = "quotation_amount")
    private BigDecimal quotationAmount;

    @Column(name = "negotiation_status")
    private String negotiationStatus;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "enquiry_description", columnDefinition = "TEXT")
    private String enquiryDescription;

    @Column(name = "quotation_date")
    private LocalDate quotationDate;

    @Column(name = "user_id_fk")
    private Long userIdFk;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;
}