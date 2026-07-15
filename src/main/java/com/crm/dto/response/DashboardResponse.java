package com.crm.dto.response;

import lombok.*;
import java.util.List;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardResponse {
    private long leadAll;
    private long leadQualified;
    private long leadWorking;
    private long leadQuotationSent;
    private long leadNegotiation;
    private long leadContacted;
    private long leadNotContacted;
    private long leadConverted;
    private long opportunityWon;
    private long opportunityLost;
    private long opportunityOpen;
    private long leadOpen;
    private long projectCount;
    private List<Map<String, Object>> leadSourceWiseCount;
    private List<Map<String, Object>> projectStatusWiseCount;
    private List<Map<String, Object>> opportunityStatusWiseCount;
    private List<Map<String, Object>> statusWiseLeadByMonth;
}
