package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "crm_xformsales_organization")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "organization_name")
    private String organizationName;

    @Column(name = "organization_moblie_no")
    private String organizationMoblieNo;

    @Column(name = "organization_email")
    private String organizationEmail;

    @Column(name = "organization_address")
    private String organizationAddress;

    @Column(name = "organization_city")
    private String organizationCity;

    @Column(name = "organization_state")
    private String organizationState;

    @Column(name = "organization_country")
    private String organizationCountry;

    @Column(name = "organization_background", columnDefinition = "TEXT")
    private String organizationBackground;

    @Column(name = "organization_occasion")
    private String organizationOccasion;

    @Column(name = "organization_postcode")
    private String organizationPostcode;

    @Column(name = "organization_occasion_date")
    private LocalDate organizationOccasionDate;

    @Column(name = "user_id_fk")
    private Long userIdFk;
}
