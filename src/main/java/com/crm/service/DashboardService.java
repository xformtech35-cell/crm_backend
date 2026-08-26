package com.crm.service;

import com.crm.dto.response.DashboardResponse;
import com.crm.entity.User;
import com.crm.repository.*;
import com.crm.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final LeadRepository leadRepository;
    private final OpportunityRepository opportunityRepository;
    private final ProjectRepository projectRepository;
    private final LeadService leadService;
    private final AuthUtil authUtil;

    public DashboardResponse getDashboardStats(User user) {
        boolean isSuperAdmin = authUtil.isSuperAdmin(user.getRole());

        Map<String, Long> leadStatusMap;
        Map<String, Long> oppStatusMap;
        Map<String, Long> projStatusMap;
        Map<String, Long> leadSourceMap;
        long leadAllCount;
        long projectCount;

        if (isSuperAdmin) {
            leadStatusMap = buildStatusMap(leadRepository.countGroupByStatus());
            oppStatusMap  = buildStatusMap(opportunityRepository.countGroupByStatus());
            projStatusMap = buildStatusMap(projectRepository.countGroupByStatus());
            leadSourceMap = buildStatusMap(leadRepository.countGroupBySource());
            leadAllCount = leadRepository.countActiveLeads();
            projectCount = projectRepository.count();
        } else {
            List<Long> companyUserIds = leadService.getCompanyUserIds(user.getUserid(), user.getRole());
            if (companyUserIds.isEmpty()) companyUserIds = List.of(user.getUserid());

            String scopeMode = authUtil.resolveDataScopeMode(user, "LEADS");
            List<Long> scopedUserIds;
            if ("ALL_DATA".equals(scopeMode) || authUtil.isAdmin(user.getRole())) {
                scopedUserIds = companyUserIds;
            } else if ("TEAM_DATA".equals(scopeMode) || authUtil.isTeamLead(user.getRole())) {
                scopedUserIds = authUtil.getTeamLeadMemberUserIds(user);
                if (scopedUserIds.isEmpty()) scopedUserIds = List.of(user.getUserid());
            } else {
                scopedUserIds = List.of(user.getUserid());
            }

            leadStatusMap = buildStatusMap(leadRepository.countGroupByStatusForUserIds(scopedUserIds));
            oppStatusMap  = buildStatusMap(opportunityRepository.countGroupByStatusForUserIds(scopedUserIds));
            projStatusMap = buildStatusMap(projectRepository.countGroupByStatusForUserIds(scopedUserIds));
            leadSourceMap = buildStatusMap(leadRepository.countGroupBySourceForUserIds(scopedUserIds));
            leadAllCount = leadRepository.countByUserIdFkIn(scopedUserIds);
            projectCount = projectRepository.countByUserIdFkIn(scopedUserIds);
        }

        List<Map<String, Object>> leadChartData = buildChartList(leadStatusMap);
        List<Map<String, Object>> oppChartData  = buildChartList(oppStatusMap);
        List<Map<String, Object>> sourceChartData = buildChartList(leadSourceMap);

        long qualified = getCountMatching(leadStatusMap, "qualif");
        long working = getCountMatching(leadStatusMap, "working");
        long quotationSent = getCountMatching(leadStatusMap, "quotation", "sent");
        long negotiation = getCountMatching(leadStatusMap, "negoti");
        long contacted = getCountMatching(leadStatusMap, "contact");
        long notContacted = getCountMatching(leadStatusMap, "not", "new", "open");
        long converted = getCountMatching(leadStatusMap, "convert", "won");

        long oppWon = getCountMatching(oppStatusMap, "won");
        long oppLost = getCountMatching(oppStatusMap, "lost", "close");
        long oppOpen = getCountMatching(oppStatusMap, "open");
        if (oppOpen == 0 && oppStatusMap.isEmpty()) {
            oppOpen = Math.max(0L, leadAllCount - oppWon - oppLost);
        }

        long leadOpenCount = Math.max(0L, leadAllCount - getCountMatching(leadStatusMap, "won", "close", "lost", "disqualif"));

        return DashboardResponse.builder()
                .leadAll(leadAllCount)
                .leadNotContacted(notContacted)
                .leadContacted(contacted)
                .leadQualified(qualified)
                .leadWorking(working)
                .leadQuotationSent(quotationSent)
                .leadNegotiation(negotiation)
                .leadConverted(converted)
                .opportunityWon(oppWon)
                .opportunityLost(oppLost)
                .opportunityOpen(oppOpen)
                .leadOpen(leadOpenCount)
                .projectCount(projectCount)
                .leadSourceWiseCount(sourceChartData)
                .projectStatusWiseCount(buildChartList(projStatusMap))
                .opportunityStatusWiseCount(oppChartData)
                .statusWiseLeadByMonth(leadChartData)
                .build();
    }

    private long getCountMatching(Map<String, Long> statusMap, String... keywords) {
        if (statusMap == null || statusMap.isEmpty()) return 0L;
        long total = 0L;
        for (Map.Entry<String, Long> entry : statusMap.entrySet()) {
            String key = entry.getKey() != null ? entry.getKey().toLowerCase() : "";
            for (String kw : keywords) {
                if (key.contains(kw.toLowerCase())) {
                    total += entry.getValue();
                    break;
                }
            }
        }
        return total;
    }

    private Map<String, Long> buildStatusMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String key = row[0] != null ? row[0].toString() : "Unknown";
            Long val  = ((Number) row[1]).longValue();
            map.put(key, val);
        }
        return map;
    }

    private List<Map<String, Object>> buildChartList(Map<String, Long> map) {
        List<Map<String, Object>> list = new ArrayList<>();
        map.forEach((k, v) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("label", k);
            item.put("count", v);
            list.add(item);
        });
        return list;
    }
}
