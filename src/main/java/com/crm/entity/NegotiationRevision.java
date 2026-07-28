package com.crm.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
    
    // OneToMany mapping - A NegotiationRevision can have multiple Documents
    @OneToMany(mappedBy = "negotiationRevision", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Document> documents = new ArrayList<>();
    
    
    
}