package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "crm_xformsales_contact")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contact_id")
    private Long contactId;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_mobile_no")
    private String contactMobileNo;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_address")
    private String contactAddress;

    @Column(name = "contact_city")
    private String contactCity;

    @Column(name = "contact_state")
    private String contactState;

    @Column(name = "contact_country")
    private String contactCountry;

    @Column(name = "contact_occasion")
    private String contactOccasion;

    @Column(name = "contact_postal_code")
    private String contactPostalCode;

    @Column(name = "contact_occasion_date")
    private LocalDate contactOccasionDate;

    @Column(name = "follow_task_category")
    private String followTaskCategory;

    @Column(name = "user_id_fk")
    private Long userIdFk;
}
