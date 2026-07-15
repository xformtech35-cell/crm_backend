package com.crm.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.crm.dto.response.ApiResponse;
import com.crm.entity.Lead;
import com.crm.entity.Negotiation;
import com.crm.entity.NegotiationRevision;
import com.crm.repository.LeadRepository;
import com.crm.repository.NegotiationRepository;
import com.crm.repository.NegotiationRevisionRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/negotiations")
@RequiredArgsConstructor
public class NegotiationController {

    private final NegotiationRepository negotiationRepository;
    private final LeadRepository leadRepository;

    private final NegotiationRevisionRepository negotiationRevisionRepository;

    // @GetMapping("/user/{userId}")
    // public ResponseEntity<ApiResponse<List<java.util.Map<String, Object>>>> getByUser(@PathVariable Long userId) {
    //     List<Negotiation> negotiations = negotiationRepository.findByUserIdFk(userId);
    //     List<java.util.Map<String, Object>> responseList = new java.util.ArrayList<>();
    //     for (Negotiation n : negotiations) {
    //         java.util.Map<String, Object> map = new java.util.HashMap<>();
    //         map.put("id", n.getId());
    //         map.put("leadIdFk", n.getLeadIdFk());
    //         map.put("negotiationName", n.getNegotiationName());
    //         map.put("negotiationTitle", n.getNegotiationTitle());
    //         map.put("quotationNo", n.getQuotationNo());
    //         map.put("quotationRevision", n.getQuotationRevision());
    //         map.put("quotationAmount", n.getQuotationAmount());
    //         map.put("negotiationStatus", n.getNegotiationStatus());
    //         map.put("remarks", n.getRemarks());
    //         map.put("userIdFk", n.getUserIdFk());
    //         String leadStatus = null;
    //         String leadOutcomeStatus = null;
    //         String quotationDate = null;
    //         String inquiryDate = null;
    //         String leadRef = null;
    //         if (n.getLeadIdFk() != null) {
    //             java.util.Optional<Lead> leadOpt = leadRepository.findById(n.getLeadIdFk());
    //             if (leadOpt.isPresent()) {
    //                 Lead l = leadOpt.get();
    //                 leadStatus = l.getLeadStatus();
    //                 leadOutcomeStatus = l.getLeadOutcomeStatus();
    //                         leadRef = l.getLeadRef();   
    //                 if (l.getQuotationDate() != null) {
    //                     quotationDate = l.getQuotationDate().toString();
    //                 }
    //                 if (l.getInquiryDate() != null) {
    //                     inquiryDate = l.getInquiryDate().toString();
    //                 }
    //             }
    //         }
    //         map.put("leadRef", leadRef);
    //         map.put("leadStatus", leadStatus);
    //         map.put("leadOutcomeStatus", leadOutcomeStatus);
    //         map.put("quotationDate", quotationDate);
    //         map.put("inquiryDate", inquiryDate);
    //         responseList.add(map);
    //     }
    //     return ResponseEntity.ok(ApiResponse.success("Negotiations fetched", responseList));
    // }
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getByUser(@PathVariable Long userId) {

        List<Negotiation> negotiations = negotiationRepository.findByUserIdFk(userId);
        List<Map<String, Object>> responseList = new ArrayList<>();

        for (Negotiation n : negotiations) {

            Map<String, Object> map = new HashMap<>();

            String leadStatus = null;
            String leadOutcomeStatus = null;
            String quotationDate = null;
            String inquiryDate = null;
            String leadRef = null;
            String enquiryDescription = null;

            // Default quotation number from Negotiation
            String quotationNo = n.getQuotationNo();

            if (n.getLeadIdFk() != null) {

                Optional<Lead> leadOpt = leadRepository.findById(n.getLeadIdFk());

                if (leadOpt.isPresent()) {

                    Lead l = leadOpt.get();

                    leadStatus = l.getLeadStatus();
                    leadOutcomeStatus = l.getLeadOutcomeStatus();
                    leadRef = l.getLeadRef();
                    enquiryDescription = l.getEnquiryDescription();

                    // Latest quotation number from Lead
                    if (l.getQuotationNumber() != null && !l.getQuotationNumber().isBlank()) {
                        quotationNo = l.getQuotationNumber();
                    }

                    if (l.getQuotationDate() != null) {
                        quotationDate = l.getQuotationDate().toString();
                    }

                    if (l.getInquiryDate() != null) {
                        inquiryDate = l.getInquiryDate().toString();
                    }
                }
            }

            map.put("id", n.getId());
            map.put("leadIdFk", n.getLeadIdFk());
            map.put("negotiationName", n.getNegotiationName());
            map.put("negotiationTitle", n.getNegotiationTitle());
            map.put("quotationNo", quotationNo);
            map.put("quotationRevision", n.getQuotationRevision());
            map.put("quotationAmount", n.getQuotationAmount());
            map.put("negotiationStatus", n.getNegotiationStatus());
            map.put("remarks", n.getRemarks());
            map.put("userIdFk", n.getUserIdFk());

            // Lead fields
            map.put("leadRef", leadRef);
            map.put("leadStatus", leadStatus);
            map.put("leadOutcomeStatus", leadOutcomeStatus);
            map.put("quotationDate", quotationDate);
            map.put("inquiryDate", inquiryDate);
            map.put("enquiryDescription", enquiryDescription);

            responseList.add(map);
        }

        return ResponseEntity.ok(ApiResponse.success("Negotiations fetched", responseList));
    }

    @GetMapping("/{id}/details")
public ResponseEntity<ApiResponse<Lead>> getDetails(@PathVariable Long id) {
    Negotiation negotiation = negotiationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Negotiation not found"));

    Lead lead = leadRepository.findById(negotiation.getLeadIdFk())
            .orElseThrow(() -> new RuntimeException("Lead not found"));

    return ResponseEntity.ok(ApiResponse.success("Lead details fetched", lead));
}

    @GetMapping("/{id}/revisions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRevisions(@PathVariable Long id) {

        List<NegotiationRevision> revisions
                = negotiationRevisionRepository.findByNegotiationIdOrderByUpdatedDateDesc(id);

        List<Map<String, Object>> list = new ArrayList<>();

        for (NegotiationRevision rev : revisions) {

            Map<String, Object> map = new HashMap<>();

            map.put("id", rev.getId());
            map.put("revisionNo", rev.getQuotationRevision());
            map.put("quotationNo", rev.getQuotationNo());
            map.put("quotationAmount", rev.getQuotationAmount());
            map.put("negotiationStatus", rev.getNegotiationStatus());
            map.put("remarks", rev.getRemarks());
            map.put("enquiryDescription", rev.getEnquiryDescription());
            map.put("quotationDate", rev.getQuotationDate());
            map.put("updatedDate", rev.getUpdatedDate());

            list.add(map);
        }

        return ResponseEntity.ok(ApiResponse.success("Revision history fetched", list));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Negotiation>> update(
            @PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, Object> updates) {
        Negotiation negotiation = negotiationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Negotiation not found"));

        if (updates.containsKey("negotiationStatus")) {
            String status = (String) updates.get("negotiationStatus");
            negotiation.setNegotiationStatus(status);
            if (negotiation.getLeadIdFk() != null) {
                java.util.Optional<Lead> leadOpt = leadRepository.findById(negotiation.getLeadIdFk());
                if (leadOpt.isPresent()) {
                    Lead l = leadOpt.get();
                    l.setLeadOutcomeStatus(status);
                    leadRepository.save(l);
                }
            }
        }
        if (updates.containsKey("negotiationTitle")) {
            negotiation.setNegotiationTitle((String) updates.get("negotiationTitle"));
        }
        if (updates.containsKey("quotationAmount")) {
            negotiation.setQuotationAmount(new java.math.BigDecimal(updates.get("quotationAmount").toString()));
        }
        if (updates.containsKey("quotationRevision")) {
            negotiation.setQuotationRevision((String) updates.get("quotationRevision"));
        }
        if (updates.containsKey("remarks")) {
            negotiation.setRemarks((String) updates.get("remarks"));
        }

        negotiation = negotiationRepository.save(negotiation);
        return ResponseEntity.ok(ApiResponse.success("Negotiation updated", negotiation));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Negotiation negotiation = negotiationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Negotiation not found"));
        negotiationRepository.delete(negotiation);
        return ResponseEntity.ok(ApiResponse.success("Negotiation deleted", null));
    }

}
