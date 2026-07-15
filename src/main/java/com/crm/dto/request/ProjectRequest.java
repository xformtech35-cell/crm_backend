package com.crm.dto.request;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProjectRequest {
    private String projectName;
    private String projectCode;
    private String organisationName;
    private String projectStatus;
    private LocalDate projectStartDate;
    private LocalDate projectCompletedDate;
    private LocalDate forecastCompletedDate;
    private String projectDescription;
    private Long oppIdFk;
}
