package com.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "crm_xformsales_project")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "project_code")
    private String projectCode;

    @Column(name = "organisation_name")
    private String organisationName;

    @Column(name = "project_status")
    private String projectStatus;

    @Column(name = "project_start_date")
    private LocalDate projectStartDate;

    @Column(name = "project_completed_date")
    private LocalDate projectCompletedDate;

    @Column(name = "forecast_completed_date")
    private LocalDate forecastCompletedDate;

    @Column(name = "project_description", columnDefinition = "TEXT")
    private String projectDescription;

    @Column(name = "project_doc")
    private String projectDoc;

    @Column(name = "user_id_fk")
    private Long userIdFk;

    @Column(name = "opp_id_fk")
    private Long oppIdFk;
}
