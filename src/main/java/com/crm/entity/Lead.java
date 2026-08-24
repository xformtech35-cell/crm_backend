package com.crm.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "crm_xformsales_lead")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@org.hibernate.annotations.SQLDelete(sql = "UPDATE crm_xformsales_lead SET is_deleted = true, deleted_at = NOW() WHERE lead_id = ?")
@org.hibernate.annotations.SQLRestriction("(is_deleted = false OR is_deleted IS NULL)")
public class Lead {

    @Builder.Default
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lead_id")
    private Long leadId;

    @Column(name = "lead_first_name")
    private String leadFirstName;

    @Column(name = "lead_last_name")
    private String leadLastName;

    @Column(name = "lead_title")
    private String leadTitle;

    @Column(name = "lead_address", length = 500)
    private String leadAddress;

    @Column(name = "lead_city")
    private String leadCity;

    @Column(name = "lead_state")
    private String leadState;

    @Column(name = "lead_country")
    private String leadCountry;

    @Column(name = "lead_mobile_no")
    private String leadMobileNo;

    @Column(name = "lead_phone_no")
    private String leadPhoneNo;

    @Column(name = "lead_email")
    private String leadEmail;

    @Column(name = "lead_organisation_name")
    private String leadOrganisationName;

    @Column(name = "lead_website")
    private String leadWebsite;

    @Column(name = "lead_industry")
    private String leadIndustry;

    @Column(name = "lead_created_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    private LocalDateTime leadCreatedDate;

    @Column(name = "no_of_employee")
    private Integer noOfEmployee;

    @Column(name = "lead_status")
    private String leadStatus;

    @Column(name = "lead_source")
    private String leadSource;

    @Column(name = "user_id_fk")
    private Long userIdFk;

    @Column(name = "upload_document", columnDefinition = "TEXT")
    private String uploadDocument;

    @Column(name = "upload_document1", columnDefinition = "TEXT")
    private String uploadDocument1;

    @Column(name = "upload_document2", columnDefinition = "TEXT")
    private String uploadDocument2;

    @Column(name = "upload_document3", columnDefinition = "TEXT")
    private String uploadDocument3;

    @Column(name = "lead_type")
    private String leadType;

    @Column(name = "lead_reason", columnDefinition = "TEXT")
    private String leadReason;

    @Column(name = "designation")
    private String designation;

    @Column(name = "inquiry_date")
    private LocalDate inquiryDate;

    @Column(name = "unique_query_id")
    private String uniqueQueryId;

    @Column(name = "lead_assigned_team")
    private Long leadAssignedTeam;

    @Column(name = "lead_assigned_member")
    private Long leadAssignedMember;

    // ─── New fields for corrected flow ───
    @Column(name = "enquiry_description", columnDefinition = "TEXT")
    private String enquiryDescription;

    @Column(name = "enquiry_type") // "Qualified" or "Disqualified"
    private String enquiryType;

    @Column(name = "company_contact_person_name")
    private String companyContactPersonName;

    @Column(name = "quotation_number")
    private String quotationNumber;

    @Column(name = "quotation_date")
    private LocalDate quotationDate;

    @Column(name = "quotation_amount")
    private java.math.BigDecimal quotationAmount;

    @Column(name = "follow_up_remark", columnDefinition = "TEXT")
    private String followUpRemark;

    @Column(name = "ongoing_priority") // "A" (Important) or "B" (Most Important)
    private String ongoingPriority;

    @Column(name = "lead_group")
    private String leadGroup;

    @Column(name = "lead_ref")
    private String leadRef;

    @Column(name = "enquiry_status") // "Sent" or "Working"
    private String enquiryStatus;

    @Column(name = "quotation_revision") // e.g. "R1", "R2", etc.
    private String quotationRevision;

    @Column(name = "lead_outcome_status", length = 50)
    private String leadOutcomeStatus;

    // Getter and Setter
    public String getLeadOutcomeStatus() {
        return leadOutcomeStatus;
    }

    public void setLeadOutcomeStatus(String leadOutcomeStatus) {
        this.leadOutcomeStatus = leadOutcomeStatus;
    }
    @Column(name = "lead_rating")
    private Integer leadRating;

    @Column(name = "quotation_sent_date")
    private LocalDate QuotationSentDate;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    private LocalDateTime updatedDate;

    @Column(name = "send_to_main_leads")
    private Boolean sendToMainLeads = false;

    public Boolean getSendToMainLeads() {
        return sendToMainLeads;
    }

    public void setSendToMainLeads(Boolean sendToMainLeads) {
        this.sendToMainLeads = sendToMainLeads;
    }

    private String remarks;

}

