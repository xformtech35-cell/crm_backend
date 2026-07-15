package com.crm.dto.request;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ContactRequest {
    private String contactName;
    private String contactMobileNo;
    private String contactEmail;
    private String contactAddress;
    private String contactCity;
    private String contactState;
    private String contactCountry;
    private String contactOccasion;
    private String contactPostalCode;
    private LocalDate contactOccasionDate;
}
