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

    public List<Opportunity> getAllOpportunities(Long companyAdminId, String role) {
        if (authUtil.isSuperAdmin(role)) return opportunityRepository.findAll();
        return opportunityRepository.findByUserIdFk(companyAdminId);
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
