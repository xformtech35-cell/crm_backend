package com.crm.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.crm.dto.response.ApiResponse;
import com.crm.entity.Document;
import com.crm.entity.Lead;
import com.crm.entity.Negotiation;
import com.crm.entity.NegotiationRevision;
import com.crm.repository.DocumentRepository;
import com.crm.repository.LeadRepository;
import com.crm.repository.NegotiationRepository;
import com.crm.repository.NegotiationRevisionRepository;
import com.crm.service.NegotiationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/negotiations")
@RequiredArgsConstructor
public class NegotiationController {

    private final NegotiationRepository negotiationRepository;
    private final LeadRepository leadRepository;
    private final com.crm.repository.UserRepository userRepository;
    private final com.crm.service.LeadService leadService;
    private final com.crm.util.AuthUtil authUtil;
    
    private final NegotiationService negotiationService;

    private final DocumentRepository documentRepository;


    private final NegotiationRevisionRepository negotiationRevisionRepository;
    
    @Value("${app.upload.dir}")
    private String uploadDir;
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

        com.crm.entity.User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.success("Negotiations fetched", new ArrayList<>()));
        }
        
        // Fetch all leads accessible to the user (respects company, team lead, and own data scope)
        List<Lead> accessibleLeads = leadService.getAllLeads(user.getUserid(), user.getRole());
        for (Lead l : accessibleLeads) {
            boolean isLeadInNeg = "Negotiation".equalsIgnoreCase(l.getLeadStatus()) || "Negotiation".equalsIgnoreCase(l.getLeadOutcomeStatus());
            if (isLeadInNeg) {
                try {
                    leadService.syncNegotiationForLead(l);
                } catch (Exception ex) {
                    log.warn("syncNegotiationForLead failed for lead {}: {}", l.getLeadId(), ex.getMessage());
                }
            }
        }

        String scopeMode = authUtil.resolveDataScopeMode(user, "NEGOTIATIONS");
        List<Long> scopedUserIds;
        if ("ALL_DATA".equals(scopeMode) || authUtil.isAdmin(user.getRole())) {
            scopedUserIds = leadService.getCompanyUserIds(user.getUserid(), user.getRole());
        } else if ("TEAM_DATA".equals(scopeMode) || authUtil.isTeamLead(user.getRole())) {
            scopedUserIds = authUtil.getTeamLeadMemberUserIds(user);
        } else {
            scopedUserIds = List.of(user.getUserid());
        }

        Set<Long> accessibleLeadIds = accessibleLeads.stream()
                .map(Lead::getLeadId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Negotiation> negSet = new HashSet<>();
        if (user != null && ("SUPER_ADMIN".equalsIgnoreCase(user.getRole()) || "SUPER ADMIN".equalsIgnoreCase(user.getRole()))) {
            negSet.addAll(negotiationRepository.findAll());
        } else {
            if (scopedUserIds != null && !scopedUserIds.isEmpty()) {
                negSet.addAll(negotiationRepository.findByUserIdFkIn(scopedUserIds));
            }
            if (!accessibleLeadIds.isEmpty()) {
                negSet.addAll(negotiationRepository.findByLeadIdFkIn(new ArrayList<>(accessibleLeadIds)));
            }
        }

        List<Negotiation> negotiations = new ArrayList<>(negSet);

        List<Map<String, Object>> responseList = new ArrayList<>();

        for (Negotiation n : negotiations) {

            if (n.getIsDeleted() != null && n.getIsDeleted()) {
                continue;
            }

            if (n.getLeadIdFk() == null) {
                continue;
            }

            Optional<Lead> leadOpt = leadRepository.findById(n.getLeadIdFk());
            if (leadOpt.isEmpty()) {
                continue;
            }

            Lead l = leadOpt.get();
            String leadStatus = l.getLeadStatus();
            String leadOutcomeStatus = l.getLeadOutcomeStatus();

            boolean isLeadInNegotiation = "Negotiation".equalsIgnoreCase(leadStatus) || "Negotiation".equalsIgnoreCase(leadOutcomeStatus);
            if (!isLeadInNegotiation) {
                continue;
            }

            Map<String, Object> map = new HashMap<>();

            String quotationDate = null;
            String inquiryDate = null;
            String leadRef = l.getLeadRef();
            String enquiryDescription = l.getEnquiryDescription();

            // Default quotation number from Negotiation
            String quotationNo = n.getQuotationNo();

            // Latest quotation number from Lead or LeadRef fallback
            if (l.getQuotationNumber() != null && !l.getQuotationNumber().isBlank()) {
                quotationNo = l.getQuotationNumber();
            } else if ((quotationNo == null || quotationNo.isBlank()) && l.getLeadRef() != null && !l.getLeadRef().isBlank()) {
                quotationNo = l.getLeadRef();
            }

            if (l.getQuotationDate() != null) {
                quotationDate = l.getQuotationDate().toString();
            }

            if (l.getInquiryDate() != null) {
                inquiryDate = l.getInquiryDate().toString();
            }
            map.put("uploadDocument", l.getUploadDocument());
            map.put("uploadDocument1", l.getUploadDocument1());
            map.put("uploadDocument2", l.getUploadDocument2());
            map.put("uploadDocument3", l.getUploadDocument3());

            // Sync back quotationNo if changed or blank in negotiation record
            if (quotationNo != null && !quotationNo.isBlank() && !quotationNo.equalsIgnoreCase(n.getQuotationNo())) {
                try {
                    n.setQuotationNo(quotationNo);
                    negotiationRepository.save(n);
                } catch (Exception ex) {
                    log.warn("Failed to persist quotationNo for negotiation {}: {}", n.getId(), ex.getMessage());
                }
            }

            map.put("id", n.getId());
            map.put("leadIdFk", n.getLeadIdFk());
            map.put("negotiationName", n.getNegotiationName());
            map.put("quotationNo", quotationNo);
            map.put("quotationNumber", quotationNo);
            String revNo = n.getQuotationRevision();
            if (revNo == null || revNo.isBlank()) {
                revNo = "R0";
            }
            map.put("quotationRevision", revNo);
            map.put("quotationAmount", n.getQuotationAmount());
            map.put("negotiationStatus", n.getNegotiationStatus());
            map.put("remarks", n.getRemarks());
            map.put("userIdFk", n.getUserIdFk());

            if (leadOutcomeStatus == null || leadOutcomeStatus.isBlank()) {
                leadOutcomeStatus = n.getNegotiationStatus() != null ? n.getNegotiationStatus() : "Negotiation";
            }
            if (leadStatus == null || leadStatus.isBlank()) {
                leadStatus = n.getNegotiationStatus() != null ? n.getNegotiationStatus() : "Negotiation";
            }

            if (enquiryDescription == null || enquiryDescription.isBlank() || quotationDate == null) {
                try {
                    List<NegotiationRevision> revs = negotiationRevisionRepository.findByNegotiationIdOrderByUpdatedDateDesc(n.getId());
                    if (revs != null && !revs.isEmpty()) {
                        NegotiationRevision latestRev = revs.get(0);
                        if ((enquiryDescription == null || enquiryDescription.isBlank()) && latestRev.getEnquiryDescription() != null) {
                            enquiryDescription = latestRev.getEnquiryDescription();
                        }
                        if (quotationDate == null && latestRev.getQuotationDate() != null) {
                            quotationDate = latestRev.getQuotationDate().toString();
                        }
                        if ((n.getQuotationAmount() == null || n.getQuotationAmount().compareTo(java.math.BigDecimal.ZERO) == 0) && latestRev.getQuotationAmount() != null) {
                            map.put("quotationAmount", latestRev.getQuotationAmount());
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Failed to fetch revision fallback for negotiation {}: {}", n.getId(), ex.getMessage());
                }
            }

            // Lead fields
            String quotationSentDate = null;
            if (l.getQuotationSentDate() != null) {
                quotationSentDate = l.getQuotationSentDate().toString();
            }

            map.put("leadRef", leadRef);
            map.put("leadStatus", leadStatus);
            map.put("leadOutcomeStatus", leadOutcomeStatus);
            map.put("quotationDate", quotationDate);
            map.put("quotationWorkingDate", quotationDate);
            map.put("quotationSentDate", quotationSentDate);
            map.put("sentQuotationDate", quotationSentDate);
            map.put("inquiryDate", inquiryDate);
            map.put("enquiryDescription", enquiryDescription);

            responseList.add(map);
        }

        return ResponseEntity.ok(ApiResponse.success("Negotiations fetched", responseList));
    }

    @GetMapping("/user/{userId}/revisions/all")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllCompanyRevisions(@PathVariable Long userId) {
        com.crm.entity.User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.success("Revisions fetched", new ArrayList<>()));
        }
        String scopeMode = authUtil.resolveDataScopeMode(user, "NEGOTIATIONS");
        List<Long> scopedUserIds;
        if ("ALL_DATA".equals(scopeMode) || authUtil.isAdmin(user.getRole())) {
            scopedUserIds = leadService.getCompanyUserIds(user.getUserid(), user.getRole());
        } else if ("TEAM_DATA".equals(scopeMode) || authUtil.isTeamLead(user.getRole())) {
            scopedUserIds = authUtil.getTeamLeadMemberUserIds(user);
        } else {
            scopedUserIds = List.of(user.getUserid());
        }
        List<Lead> accessibleLeads = leadService.getAllLeads(user.getUserid(), user.getRole());
        Set<Long> accessibleLeadIds = accessibleLeads.stream()
                .map(Lead::getLeadId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Negotiation> negSet = new HashSet<>();
        if (user != null && ("SUPER_ADMIN".equalsIgnoreCase(user.getRole()) || "SUPER ADMIN".equalsIgnoreCase(user.getRole()))) {
            negSet.addAll(negotiationRepository.findAll());
        } else {
            if (scopedUserIds != null && !scopedUserIds.isEmpty()) {
                negSet.addAll(negotiationRepository.findByUserIdFkIn(scopedUserIds));
            }
            if (!accessibleLeadIds.isEmpty()) {
                negSet.addAll(negotiationRepository.findByLeadIdFkIn(new ArrayList<>(accessibleLeadIds)));
            }
        }

        List<Negotiation> negotiations = new ArrayList<>(negSet);

        List<Map<String, Object>> allRevisions = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        for (Negotiation n : negotiations) {
            List<NegotiationRevision> revs = negotiationRevisionRepository.findByNegotiationIdOrderByUpdatedDateDesc(n.getId());
            for (NegotiationRevision rev : revs) {
                String revCode = rev.getQuotationRevision() != null ? rev.getQuotationRevision() : "R0";
                String key = n.getId() + "-" + revCode.toUpperCase();
                if (seenKeys.contains(key)) {
                    continue;
                }
                seenKeys.add(key);

                Map<String, Object> map = new HashMap<>();
                map.put("id", rev.getId());
                map.put("negotiationId", n.getId());
                map.put("negotiationName", n.getNegotiationName());
                map.put("revisionNo", revCode);
                map.put("quotationNo", rev.getQuotationNo());
                map.put("quotationAmount", rev.getQuotationAmount() != null ? rev.getQuotationAmount() : n.getQuotationAmount());
                map.put("negotiationStatus", rev.getNegotiationStatus() != null ? rev.getNegotiationStatus() : n.getNegotiationStatus());
                map.put("remarks", rev.getRemarks());
                map.put("enquiryDescription", rev.getEnquiryDescription());
                map.put("quotationDate", rev.getQuotationDate());
                map.put("quotationWorkingDate", rev.getQuotationDate());
                
                String quotationSentDate = null;
                if (n.getLeadIdFk() != null) {
                    Optional<Lead> leadOpt = leadRepository.findById(n.getLeadIdFk());
                    if (leadOpt.isPresent() && leadOpt.get().getQuotationSentDate() != null) {
                        quotationSentDate = leadOpt.get().getQuotationSentDate().toString();
                    }
                }
                map.put("quotationSentDate", quotationSentDate);
                map.put("sentQuotationDate", quotationSentDate);
                map.put("updatedDate", rev.getUpdatedDate());
                allRevisions.add(map);
            }
        }
        return ResponseEntity.ok(ApiResponse.success("All revisions fetched", allRevisions));
    }


    @GetMapping("/{id}/details")
public ResponseEntity<ApiResponse<Lead>> getDetails(@PathVariable Long id) {
    Negotiation negotiation = negotiationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Negotiation not found"));

    Lead lead = leadRepository.findById(negotiation.getLeadIdFk())
            .orElseThrow(() -> new RuntimeException("Lead not found"));

    return ResponseEntity.ok(ApiResponse.success("Lead details fetched", lead));
}

    @GetMapping("/lead/{leadId}/revisions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRevisionsByLeadId(@PathVariable Long leadId) {
        try {
            // 1. Find the negotiation for this lead
            Optional<Negotiation> nOpt = negotiationRepository.findFirstByLeadIdFk(leadId);
            if (nOpt.isEmpty()) {
                // Try to sync if lead is in negotiation status
                Optional<Lead> leadOpt = leadRepository.findById(leadId);
                if (leadOpt.isPresent()) {
                    try { leadService.syncNegotiationForLead(leadOpt.get()); } catch (Exception ex) {
                        log.warn("syncNegotiationForLead failed for leadId {}: {}", leadId, ex.getMessage());
                    }
                    nOpt = negotiationRepository.findFirstByLeadIdFk(leadId);
                }
            }
            if (nOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("No negotiation found", new ArrayList<>()));
            }

            Negotiation negotiation = nOpt.get();
            Optional<Lead> leadOpt = negotiation.getLeadIdFk() != null 
                    ? leadRepository.findById(negotiation.getLeadIdFk()) 
                    : Optional.empty();
            if (leadOpt.isPresent()) {
                leadService.ensureR0RevisionExists(negotiation, leadOpt.get());
            }


            List<NegotiationRevision> revisions = negotiationRevisionRepository
                    .findByNegotiationIdOrLeadIdFkOrderByUpdatedDateDesc(negotiation.getId(), leadId);

            List<Map<String, Object>> list = new ArrayList<>();
            Set<String> seenRevisions = new HashSet<>();

            String activeRevCode = negotiation.getQuotationRevision() != null ? negotiation.getQuotationRevision() : "R0";

            for (NegotiationRevision rev : revisions) {
                String revCode = rev.getQuotationRevision() != null ? rev.getQuotationRevision() : "R0";
                if (seenRevisions.contains(revCode.toUpperCase())) {
                    continue;
                }
                seenRevisions.add(revCode.toUpperCase());

                Map<String, Object> map = new HashMap<>();
                map.put("id", rev.getId());
                map.put("revisionNo", revCode);
                String rawQtn = rev.getQuotationNo() != null ? rev.getQuotationNo() : "";
                String baseQuot = rawQtn.replaceAll("/R\\d+$", "");
                String computedQuotNo = "R0".equalsIgnoreCase(revCode) ? baseQuot : (baseQuot.isBlank() ? rawQtn : (baseQuot + "/" + revCode.toUpperCase()));
                map.put("quotationNo", computedQuotNo);
                map.put("quotationAmount", rev.getQuotationAmount());
                map.put("negotiationStatus", rev.getNegotiationStatus());
                map.put("remarks", rev.getRemarks());
                map.put("enquiryDescription", rev.getEnquiryDescription());
                map.put("quotationDate", rev.getQuotationDate());
                map.put("quotationWorkingDate", rev.getQuotationDate());
                
                String quotationSentDate = null;
                if (rev.getQuotationSentDate() != null) {
                    quotationSentDate = rev.getQuotationSentDate().toString();
                } else if ("R0".equalsIgnoreCase(revCode) && leadOpt.isPresent() && leadOpt.get().getQuotationSentDate() != null) {
                    quotationSentDate = leadOpt.get().getQuotationSentDate().toString();
                }
                map.put("quotationSentDate", quotationSentDate);
                map.put("sentQuotationDate", quotationSentDate);
                map.put("updatedDate", rev.getUpdatedDate());
                map.put("isCurrent", revCode.equalsIgnoreCase(activeRevCode));

                try {
                    boolean isR0 = "R0".equalsIgnoreCase(revCode);
                    String targetQuot = isR0 ? baseQuot : (baseQuot + "/" + revCode.toUpperCase());

                    List<Document> documents = documentRepository.findByNegotiationRevisionId(rev.getId());
                    if (documents != null) {
                        documents = documents.stream()
                                .filter(d -> {
                                    if (d == null || (d.getIsDeleted() != null && d.getIsDeleted())) return false;
                                    if (isR0) {
                                        if (d.getQuotationNo() != null && d.getQuotationNo().toUpperCase().matches(".*/R[1-9]\\d*$")) return false;
                                        if (d.getFileUrl() != null && d.getFileUrl().matches("(?i).*/R[1-9]\\d*/.*")) return false;
                                    } else {
                                        boolean matchesRev = (d.getQuotationNo() != null && d.getQuotationNo().toUpperCase().endsWith("/" + revCode.toUpperCase()))
                                                || (d.getFileUrl() != null && d.getFileUrl().toUpperCase().contains("/" + revCode.toUpperCase() + "/"));
                                        if (!matchesRev && d.getNegotiationRevision() != null) {
                                            String rRev = d.getNegotiationRevision().getQuotationRevision();
                                            if (rRev != null && !rRev.equalsIgnoreCase(revCode)) return false;
                                        }
                                    }
                                    return true;
                                })
                                .collect(Collectors.toList());
                    }

                    if (documents == null || documents.isEmpty()) {
                        if (!targetQuot.isBlank()) {
                            List<Document> byQtn = documentRepository.findByQuotationNo(targetQuot);
                            if (byQtn == null || byQtn.isEmpty()) {
                                byQtn = documentRepository.findByQuotationNo(targetQuot.replace("/", "_"));
                            }
                            if (byQtn != null) {
                                documents = byQtn.stream()
                                        .filter(d -> d != null && (d.getIsDeleted() == null || !d.getIsDeleted()))
                                        .filter(d -> {
                                            if (isR0) {
                                                if (d.getQuotationNo() != null && d.getQuotationNo().toUpperCase().matches(".*/R[1-9]\\d*$")) return false;
                                                if (d.getFileUrl() != null && d.getFileUrl().matches("(?i).*/R[1-9]\\d*/.*")) return false;
                                            } else {
                                                if (d.getQuotationNo() != null && !d.getQuotationNo().toUpperCase().endsWith("/" + revCode.toUpperCase())) return false;
                                            }
                                            return true;
                                        })
                                        .collect(Collectors.toList());
                            }
                        }
                    }
                    List<Map<String, Object>> docList = new ArrayList<>();
                    if (documents != null) {
                        for (Document doc : documents) {
                            if (doc.getIsDeleted() != null && doc.getIsDeleted()) continue;
                            Map<String, Object> docMap = new HashMap<>();
                            docMap.put("id", doc.getId());
                            docMap.put("fileName", doc.getFileName());
                            docMap.put("fileSize", doc.getFileSize());
                            docMap.put("fileType", doc.getFileType());
                            docMap.put("uploadedDate", doc.getUploadedDate());
                            docMap.put("fileUrl", doc.getFileUrl());
                            docList.add(docMap);
                        }
                    }
                    if (docList.size() > 1) {
                        docList = docList.subList(docList.size() - 1, docList.size());
                    }
                    map.put("documents", docList);
                    map.put("documentCount", docList.size());
                } catch (Exception ex) {
                    map.put("documents", new ArrayList<>());
                    map.put("documentCount", 0);
                }
                list.add(map);
            }

            list.sort((a, b) -> {
                String rA = String.valueOf(a.get("revisionNo"));
                String rB = String.valueOf(b.get("revisionNo"));
                int numA = 0, numB = 0;
                try { numA = Integer.parseInt(rA.replaceAll("[^0-9]", "")); } catch (Exception e) {}
                try { numB = Integer.parseInt(rB.replaceAll("[^0-9]", "")); } catch (Exception e) {}
                return Integer.compare(numA, numB);
            });

            return ResponseEntity.ok(ApiResponse.success("Revision history fetched", list));
        } catch (Exception e) {
            log.error("Error in getRevisionsByLeadId for leadId {}: {}", leadId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success("Revision history fetched", new ArrayList<>()));
        }
    }

    @GetMapping("/{id}/revisions")
    @Transactional
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRevisions(@PathVariable Long id) {
        Negotiation negotiation = negotiationRepository.findById(id).orElse(null);
        if (negotiation == null) {
            return ResponseEntity.ok(ApiResponse.success("Negotiation not found", new ArrayList<>()));
        }

        if (negotiation.getLeadIdFk() != null) {
            Optional<Lead> leadOpt = leadRepository.findById(negotiation.getLeadIdFk());
            if (leadOpt.isPresent()) {
                leadService.ensureR0RevisionExists(negotiation, leadOpt.get());
            }
        }

        List<NegotiationRevision> revisions;
        if (negotiation.getLeadIdFk() != null) {
            revisions = negotiationRevisionRepository.findByNegotiationIdOrLeadIdFkOrderByUpdatedDateDesc(id, negotiation.getLeadIdFk());
        } else {
            revisions = negotiationRevisionRepository.findByNegotiationIdOrderByUpdatedDateDesc(id);
        }

        List<Map<String, Object>> list = new ArrayList<>();
        Set<String> seenRevisions = new HashSet<>();

        for (NegotiationRevision rev : revisions) {
            String revCode = rev.getQuotationRevision() != null ? rev.getQuotationRevision() : "R0";
            if (seenRevisions.contains(revCode.toUpperCase())) {
                continue;
            }
            seenRevisions.add(revCode.toUpperCase());

            Map<String, Object> map = new HashMap<>();

            map.put("id", rev.getId());
            map.put("revisionNo", revCode);
            String rawQtn = rev.getQuotationNo() != null ? rev.getQuotationNo() : "";
            String baseQuot = rawQtn.replaceAll("/R\\d+$", "");
            String computedQuotNo = "R0".equalsIgnoreCase(revCode) ? baseQuot : (baseQuot.isBlank() ? rawQtn : (baseQuot + "/" + revCode.toUpperCase()));
            map.put("quotationNo", computedQuotNo);
            map.put("quotationAmount", rev.getQuotationAmount());
            map.put("negotiationStatus", rev.getNegotiationStatus());
            map.put("remarks", rev.getRemarks());
            map.put("quotationDate", rev.getQuotationDate());
            map.put("quotationWorkingDate", rev.getQuotationDate());
            
            String quotationSentDate = null;
            if (rev.getQuotationSentDate() != null) {
                quotationSentDate = rev.getQuotationSentDate().toString();
            } else if ("R0".equalsIgnoreCase(revCode) && negotiation.getLeadIdFk() != null) {
                Optional<Lead> leadOptSent = leadRepository.findById(negotiation.getLeadIdFk());
                if (leadOptSent.isPresent() && leadOptSent.get().getQuotationSentDate() != null) {
                    quotationSentDate = leadOptSent.get().getQuotationSentDate().toString();
                }
            }
            map.put("quotationSentDate", quotationSentDate);
            map.put("sentQuotationDate", quotationSentDate);
            map.put("updatedDate", rev.getUpdatedDate());
            
            // Get documents using revision ID or exact quotationNo match
            boolean isR0 = "R0".equalsIgnoreCase(revCode);
            String targetQuot = isR0 ? baseQuot : (baseQuot + "/" + revCode.toUpperCase());

            List<Document> documents = documentRepository.findByNegotiationRevisionId(rev.getId());
            if (documents != null) {
                documents = documents.stream()
                        .filter(d -> {
                            if (d == null || (d.getIsDeleted() != null && d.getIsDeleted())) return false;
                            if (isR0) {
                                if (d.getQuotationNo() != null && d.getQuotationNo().toUpperCase().matches(".*/R[1-9]\\d*$")) return false;
                                if (d.getFileUrl() != null && d.getFileUrl().matches("(?i).*/R[1-9]\\d*/.*")) return false;
                            } else {
                                boolean matchesRev = (d.getQuotationNo() != null && d.getQuotationNo().toUpperCase().endsWith("/" + revCode.toUpperCase()))
                                        || (d.getFileUrl() != null && d.getFileUrl().toUpperCase().contains("/" + revCode.toUpperCase() + "/"));
                                if (!matchesRev && d.getNegotiationRevision() != null) {
                                    String rRev = d.getNegotiationRevision().getQuotationRevision();
                                    if (rRev != null && !rRev.equalsIgnoreCase(revCode)) return false;
                                }
                            }
                            return true;
                        })
                        .collect(Collectors.toList());
            }

            if (documents == null || documents.isEmpty()) {
                if (!targetQuot.isBlank()) {
                    List<Document> byQtn = documentRepository.findByQuotationNo(targetQuot);
                    if (byQtn == null || byQtn.isEmpty()) {
                        byQtn = documentRepository.findByQuotationNo(targetQuot.replace("/", "_"));
                    }
                    if (byQtn != null) {
                        documents = byQtn.stream()
                                .filter(d -> d != null && (d.getIsDeleted() == null || !d.getIsDeleted()))
                                .filter(d -> {
                                    if (isR0) {
                                        if (d.getQuotationNo() != null && d.getQuotationNo().toUpperCase().matches(".*/R[1-9]\\d*$")) return false;
                                        if (d.getFileUrl() != null && d.getFileUrl().matches("(?i).*/R[1-9]\\d*/.*")) return false;
                                    } else {
                                        if (d.getQuotationNo() != null && !d.getQuotationNo().toUpperCase().endsWith("/" + revCode.toUpperCase())) return false;
                                    }
                                    return true;
                                })
                                .collect(Collectors.toList());
                    }
                }
            }
            
            if (documents != null && !documents.isEmpty()) {
                List<Map<String, Object>> docList = new ArrayList<>();
                for (Document doc : documents) {
                    if (doc.getIsDeleted() != null && doc.getIsDeleted()) {
                        continue;
                    }
                    Map<String, Object> docMap = new HashMap<>();
                    docMap.put("id", doc.getId());
                    docMap.put("fileName", doc.getFileName());
                    docMap.put("fileSize", doc.getFileSize());
                    docMap.put("fileType", doc.getFileType());
                    docMap.put("uploadedDate", doc.getUploadedDate());
                    docMap.put("fileUrl", doc.getFileUrl());
                    docList.add(docMap);
                }
                if (docList.size() > 1) {
                    docList = docList.subList(docList.size() - 1, docList.size());
                }
                map.put("documents", docList);
                map.put("documentCount", docList.size());
            } else {
                List<Map<String, Object>> docList = new ArrayList<>();
                boolean isR0Fallback = "R0".equalsIgnoreCase(revCode);
                if (isR0Fallback && negotiation.getLeadIdFk() != null) {
                    Optional<Lead> leadOptDoc = leadRepository.findById(negotiation.getLeadIdFk());
                    if (leadOptDoc.isPresent()) {
                        Lead lead = leadOptDoc.get();
                        List<String> urls = new ArrayList<>();
                        if (lead.getUploadDocument() != null && !lead.getUploadDocument().isBlank()) urls.add(lead.getUploadDocument());
                        if (lead.getUploadDocument1() != null && !lead.getUploadDocument1().isBlank()) urls.add(lead.getUploadDocument1());
                        if (lead.getUploadDocument2() != null && !lead.getUploadDocument2().isBlank()) urls.add(lead.getUploadDocument2());
                        if (lead.getUploadDocument3() != null && !lead.getUploadDocument3().isBlank()) urls.add(lead.getUploadDocument3());

                        for (int i = 0; i < urls.size(); i++) {
                            String url = urls.get(i);
                            if (url != null && url.contains("/R")) continue; // Skip sub-revision files for R0
                            String fileName = url.substring(url.lastIndexOf('/') + 1);
                            if (fileName.contains("_")) {
                                fileName = fileName.substring(fileName.indexOf('_') + 1);
                            }
                            Map<String, Object> docMap = new HashMap<>();
                            docMap.put("id", "lead-doc-" + i);
                            docMap.put("fileName", fileName);
                            docMap.put("fileSize", 1024L);
                            docMap.put("fileType", url.toLowerCase().endsWith(".pdf") ? "application/pdf" : "image/jpeg");
                            docMap.put("uploadedDate", lead.getLeadCreatedDate());
                            docMap.put("fileUrl", url);
                            docList.add(docMap);
                        }
                        if (docList.size() > 1) {
                            docList = docList.subList(docList.size() - 1, docList.size());
                        }
                    }
                }
                map.put("documents", docList);
                map.put("documentCount", docList.size());
            }

            list.add(map);
        }

        list.sort((a, b) -> {
            String rA = String.valueOf(a.get("revisionNo"));
            String rB = String.valueOf(b.get("revisionNo"));
            int numA = 0, numB = 0;
            try { numA = Integer.parseInt(rA.replaceAll("[^0-9]", "")); } catch (Exception e) {}
            try { numB = Integer.parseInt(rB.replaceAll("[^0-9]", "")); } catch (Exception e) {}
            return Integer.compare(numA, numB);
        });

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
        negotiationService.deleteNegotiation(id);
        return ResponseEntity.ok(ApiResponse.success("Negotiation moved to trash", null));
    }


    
    
    // ✅ POST - Upload Document (Already Working)
    @PostMapping(value = "/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            log.info("Uploading file for negotiation ID: {}", id);
            String filename = negotiationService.uploadDocument(id, file);
            log.info("File uploaded successfully: {}", filename);
            return ResponseEntity.ok(ApiResponse.success("File uploaded successfully", filename));
        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Upload failed: " + e.getMessage()));
        }
    }

    // ✅ GET - Get Document by Negotiation ID (FIXED)
    @GetMapping("/{id}/document")
    public ResponseEntity<?> getDocument(@PathVariable Long id) {
        try {
            log.info("Getting document for negotiation ID: {}", id);
            
            // Get the filename from database
            String filename = negotiationService.getDocumentFilename(id);
            log.info("Filename from database: {}", filename);
            
            if (filename == null || filename.isBlank()) {
                log.warn("No document found for negotiation ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            // Build the file path
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
            Path filePath = uploadPath.resolve(filename);
            
            log.info("Looking for file at: {}", filePath);
            
            // Check if file exists
            if (!Files.exists(filePath)) {
                log.warn("File not found on disk: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            // Create resource
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("Resource not readable: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = determineContentType(filename);
            log.info("Content type: {}", contentType);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error retrieving document: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error retrieving document: " + e.getMessage());
        }
    }

    // ✅ GET - Get Document by Filename (Alternative)
    @GetMapping("/document/{filename}")
    public ResponseEntity<?> getDocumentByFilename(@PathVariable String filename) {
        try {
            log.info("Getting document by filename: {}", filename);
            
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
            Path filePath = uploadPath.resolve(filename);
            
            if (!Files.exists(filePath)) {
                log.warn("File not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            String contentType = determineContentType(filename);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Error retrieving document: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error retrieving document: " + e.getMessage());
        }
    }

    // ✅ DELETE - Delete Document
    @DeleteMapping("/{id}/document")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id) {
        try {
            log.info("Deleting document for negotiation ID: {}", id);
            negotiationService.deleteDocument(id);
            return ResponseEntity.ok(ApiResponse.success("Document deleted successfully", null));
        } catch (Exception e) {
            log.error("Delete failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Delete failed: " + e.getMessage()));
        }
    }

    private String determineContentType(String filename) {
        if (filename == null) return "application/octet-stream";
        
        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "csv" -> "text/csv";
            case "txt" -> "text/plain";
            default -> "application/octet-stream";
        };
    }
}


