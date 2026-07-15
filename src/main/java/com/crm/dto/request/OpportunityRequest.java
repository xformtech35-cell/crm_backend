package com.crm.dto.request;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OpportunityRequest {
    private String oppName;
    private String oppTitle;
    private String oppStatus;
    private BigDecimal oppAmount;
    private LocalDate oppForcastCloseDate;
    private LocalDate oppActualCloseDate;
    private String oppDescription;
    private Long leadIdFk;
}
