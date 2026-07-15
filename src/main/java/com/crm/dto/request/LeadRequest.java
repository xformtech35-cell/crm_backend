package com.crm.dto.request;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LeadRequest {
    private String leadFirstName;
    private String leadLastName;
    private String leadTitle;
    private String leadMobileNo;
    private String leadPhoneNo;
    private String leadAddress;
    private String leadEmail;
    private String leadCity;
    private String leadState;
    private String leadCountry;
    private String leadOrganisationName;
    private String leadWebsite;
    private String leadIndustry;
    private Integer noOfEmployee;
    private String leadSource;
    private String leadType;
    private String leadReason;
    private String leadOutcomeStatus;
    private String leadStatus;
    private String designation;
    private LocalDate inquiryDate;
    private Long userIdFk;
    private Long leadAssignedTeam;
    private Long leadAssignedMember;

    // New fields
    private String enquiryDescription;
    private String enquiryType;
    private String companyContactPersonName;
    private String quotationNumber;
    private LocalDate quotationDate;
    private java.math.BigDecimal quotationAmount;
    private String followUpRemark;
    private String ongoingPriority;
    private String leadGroup;
    private String leadRef;
    private String enquiryStatus;
    private String quotationRevision;
    private Integer leadRating;
    private LocalDate quotationSentDate;


    
}
