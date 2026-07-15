package com.crm.dto.request;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OrganizationRequest {
    private String organizationName;
    private String organizationMoblieNo;
    private String organizationEmail;
    private String organizationAddress;
    private String organizationCity;
    private String organizationState;
    private String organizationCountry;
    private String organizationBackground;
    private String organizationOccasion;
    private String organizationPostcode;
    private LocalDate organizationOccasionDate;
}
