package com.crm.service;

import com.crm.dto.request.OpportunityRequest;
import com.crm.entity.Opportunity;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.OpportunityRepository;
import com.crm.util.FileUploadUtil;
import com.crm.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final FileUploadUtil fileUploadUtil;
    private final AuthUtil authUtil;
    private final com.crm.repository.UserRepository userRepository;
    private final LeadService leadService;

    public List<Opportunity> getAllOpportunities(Long userId, String role) {
        if (authUtil.isSuperAdmin(role)) return opportunityRepository.findAll();
        com.crm.entity.User user = userRepository.findById(userId).orElse(null);
        String scopeMode = authUtil.resolveDataScopeMode(user, "OPPORTUNITIES");

        if ("ALL_DATA".equals(scopeMode)) {
            List<Long> companyUserIds = leadService.getCompanyUserIds(userId, role);
            return opportunityRepository.findByUserIdFkIn(companyUserIds);
        }
        if ("TEAM_DATA".equals(scopeMode)) {
            List<com.crm.entity.Lead> teamLeads = leadService.getAllLeads(userId, role);
            List<Long> leadIds = teamLeads.stream().map(com.crm.entity.Lead::getLeadId).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toList());
            List<Long> teamUserIds = authUtil.getTeamLeadMemberUserIds(user);
            if (teamUserIds.isEmpty()) teamUserIds = List.of(-1L);
            final List<Long> finalTeamUserIds = teamUserIds;

            return opportunityRepository.findAll().stream()
                    .filter(o -> (o.getLeadIdFk() != null && leadIds.contains(o.getLeadIdFk())) ||
                                 (o.getUserIdFk() != null && finalTeamUserIds.contains(o.getUserIdFk())))
                    .collect(java.util.stream.Collectors.toList());
        }
        // OWN_DATA_ONLY
        List<com.crm.entity.Lead> ownLeads = leadService.getAllLeads(userId, role);
        List<Long> ownLeadIds = ownLeads.stream().map(com.crm.entity.Lead::getLeadId).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toList());

        return opportunityRepository.findAll().stream()
                .filter(o -> (o.getLeadIdFk() != null && ownLeadIds.contains(o.getLeadIdFk())) ||
                             (o.getUserIdFk() != null && o.getUserIdFk().equals(userId)))
                .collect(java.util.stream.Collectors.toList());
    }

    public Opportunity getById(Long id) {
        return opportunityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity", "id", id));
    }

    public Opportunity create(OpportunityRequest req, Long companyAdminId, MultipartFile doc) throws IOException {
        Opportunity opp = mapToEntity(req, new Opportunity());
        opp.setUserIdFk(companyAdminId);
        if (doc != null && !doc.isEmpty()) opp.setOppDoc(fileUploadUtil.upload(doc));
        return opportunityRepository.save(opp);
    }

    public Opportunity update(Long id, OpportunityRequest req, MultipartFile doc) throws IOException {
        Opportunity opp = getById(id);
        mapToEntity(req, opp);
        if (doc != null && !doc.isEmpty()) opp.setOppDoc(fileUploadUtil.upload(doc));
        return opportunityRepository.save(opp);
    }

    public void delete(Long id) {
        opportunityRepository.delete(getById(id));
    }

    private Opportunity mapToEntity(OpportunityRequest req, Opportunity opp) {
        opp.setOppName(req.getOppName());
        opp.setOppTitle(req.getOppTitle());
        opp.setOppStatus(req.getOppStatus());
        opp.setOppAmount(req.getOppAmount());
        opp.setOppForcastCloseDate(req.getOppForcastCloseDate());
        opp.setOppActualCloseDate(req.getOppActualCloseDate());
        opp.setOppDescription(req.getOppDescription());
        opp.setLeadIdFk(req.getLeadIdFk());
        return opp;
    }
}
