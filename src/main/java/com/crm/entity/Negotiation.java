package com.crm.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "crm_negotiation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Negotiation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long leadIdFk;

    private String negotiationName;
    private String negotiationTitle;

    private String quotationNo;
    private String quotationRevision;

    private BigDecimal quotationAmount;

    private String negotiationStatus;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    private Long userIdFk;
    
    
// ========== NEW DOCUMENT FIELDS ==========
    
    @Column(name = "document", length = 255)
    private String document;  // Stores the filename only (e.g., "64ef6496-3a8c-4d1e-a3d6-a3d99bcf7f41.jpg")

    @Column(name = "document_url", length = 500)
    private String documentUrl;  // Stores the full URL to access the document

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;
}