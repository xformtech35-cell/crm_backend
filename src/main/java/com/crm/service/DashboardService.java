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
            leadAllCount = leadRepository.count();
            projectCount = projectRepository.count();
        } else {
            Long companyAdminId = authUtil.getCompanyAdminId(user);
            if (companyAdminId == null) companyAdminId = user.getUserid();
            
            String scopeMode = authUtil.resolveDataScopeMode(user, "LEADS");
            if ("ALL_DATA".equals(scopeMode) || authUtil.isAdmin(user.getRole())) {
                leadStatusMap = buildStatusMap(leadRepository.countGroupByStatusForUser(companyAdminId));
                oppStatusMap  = buildStatusMap(opportunityRepository.countGroupByStatusForUser(companyAdminId));
                projStatusMap = buildStatusMap(projectRepository.countGroupByStatusForUser(companyAdminId));
                leadSourceMap = buildStatusMap(leadRepository.countGroupBySourceForUser(companyAdminId));
                leadAllCount = leadRepository.countByUserIdFk(companyAdminId);
                projectCount = projectRepository.countByUserIdFk(companyAdminId);
            } else {
                Long userId = user.getUserid();
                leadStatusMap = buildStatusMap(leadRepository.countGroupByStatusForUser(userId));
                oppStatusMap  = buildStatusMap(opportunityRepository.countGroupByStatusForUser(userId));
                projStatusMap = buildStatusMap(projectRepository.countGroupByStatusForUser(userId));
                leadSourceMap = buildStatusMap(leadRepository.countGroupBySourceForUser(userId));
                leadAllCount = leadRepository.countByUserIdFk(userId);
                projectCount = projectRepository.countByUserIdFk(userId);
            }
        }

        List<Map<String, Object>> leadChartData = buildChartList(leadStatusMap);
        List<Map<String, Object>> oppChartData  = buildChartList(oppStatusMap);
        List<Map<String, Object>> sourceChartData = buildChartList(leadSourceMap);

        return DashboardResponse.builder()
                .leadAll(leadAllCount)
                .leadNotContacted(leadStatusMap.getOrDefault("NotContacted", 0L))
                .leadContacted(leadStatusMap.getOrDefault("Contacted", 0L))
                .leadQualified(leadStatusMap.getOrDefault("Qualified Lead", 0L))
                .leadWorking(leadStatusMap.getOrDefault("Working", 0L))
                .leadQuotationSent(leadStatusMap.getOrDefault("QuotationSent", 0L))
                .leadNegotiation(leadStatusMap.getOrDefault("Negotiation", 0L))
                .leadConverted(leadStatusMap.getOrDefault("Converted", 0L))
                .opportunityWon(oppStatusMap.getOrDefault("Won", 0L))
                .opportunityLost(oppStatusMap.getOrDefault("Lost", 0L))
                .opportunityOpen(oppStatusMap.getOrDefault("Open", 0L))
                .leadOpen(leadStatusMap.values().stream().mapToLong(Long::longValue).sum())
                .projectCount(projectCount)
                .leadSourceWiseCount(sourceChartData)
                .projectStatusWiseCount(buildChartList(projStatusMap))
                .opportunityStatusWiseCount(oppChartData)
                .statusWiseLeadByMonth(leadChartData)
                .build();
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
