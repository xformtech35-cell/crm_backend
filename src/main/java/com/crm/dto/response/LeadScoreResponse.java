package com.crm.dto.response;

import com.crm.entity.LeadScore.Grade;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeadScoreResponse {

    private Long leadId;
    private String leadName;
    private Integer score;
    private Grade grade;
    private List<String> topFactors;
    private LocalDateTime calculatedAt;
}
