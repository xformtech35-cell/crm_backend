package com.crm.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.crm.dto.request.ImportLeadRequest;
import com.crm.dto.request.LeadRequest;
import com.crm.entity.IntegrationConfig;
import com.crm.entity.Lead;
import com.crm.entity.LeadNote;
import com.crm.entity.LeadReminder;
import com.crm.entity.Negotiation;
import com.crm.entity.NegotiationRevision;
import com.crm.entity.Opportunity;
import com.crm.entity.Task;
import com.crm.exception.BadRequestException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.CreateTeamRepository;
import com.crm.repository.IntegrationConfigRepository;
import com.crm.repository.LeadNoteRepository;
import com.crm.repository.LeadReminderRepository;
import com.crm.repository.LeadRepository;
import com.crm.repository.LeadScoreRepository;
import com.crm.repository.NegotiationRepository;
import com.crm.repository.NegotiationRevisionRepository;
import com.crm.repository.OpportunityRepository;
import com.crm.repository.TaskRepository;
import com.crm.repository.TeamMemberRepository;
import com.crm.repository.UserRepository;
import com.crm.entity.TeamMember;
import com.crm.entity.User;
import com.crm.util.AppConstants;
import com.crm.util.AuthUtil;
import com.crm.util.FileUploadUtil;

import org.springframework.data.domain.Sort;
import java.util.HashMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;
    private final LeadNoteRepository leadNoteRepository;
    private final LeadReminderRepository leadReminderRepository;
    private final LeadScoreRepository leadScoreRepository;
    private final OpportunityRepository opportunityRepository;
    private final TaskRepository taskRepository;
    private final FileUploadUtil fileUploadUtil;
    private final WebClient webClient;
    private final LeadScoringService leadScoringService;
    private final AuthUtil authUtil;
    private final IntegrationConfigRepository integrationConfigRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CreateTeamRepository createTeamRepository;

    private final NegotiationRevisionRepository negotiationRevisionRepository;
    private final NegotiationRepository negotiationRepository;


    // public LeadService() {
    //     this.leadRepository = null;
    //     this.leadNoteRepository = null;
    //     this.leadReminderRepository = null;
    //     this.leadScoreRepository = null;
    //     this.opportunityRepository = null;
    //     this.taskRepository = null;
    //     this.fileUploadUtil = null;
    //     this.webClient = null;
    //     this.leadScoringService = null;
    //     this.authUtil = null;
    //     this.integrationConfigRepository = null;
    //     this.negotiationRepository = null;
    // }
    // public LeadService(LeadRepository leadRepository, LeadNoteRepository leadNoteRepository, LeadReminderRepository leadReminderRepository, LeadScoreRepository leadScoreRepository, OpportunityRepository opportunityRepository, TaskRepository taskRepository, FileUploadUtil fileUploadUtil, WebClient webClient, LeadScoringService leadScoringService, AuthUtil authUtil, IntegrationConfigRepository integrationConfigRepository, NegotiationRepository negotiationRepository) {
    //     this.leadRepository = leadRepository;
    //     this.leadNoteRepository = leadNoteRepository;
    //     this.leadReminderRepository = leadReminderRepository;
    //     this.leadScoreRepository = leadScoreRepository;
    //     this.opportunityRepository = opportunityRepository;
    //     this.taskRepository = taskRepository;
    //     this.fileUploadUtil = fileUploadUtil;
    //     this.webClient = webClient;
    //     this.leadScoringService = leadScoringService;
    //     this.authUtil = authUtil;
    //     this.integrationConfigRepository = integrationConfigRepository;
    //     this.negotiationRepository = negotiationRepository;
    // }
    @Value("${app.indiamart.api-key}")
    private String indiamartApiKey;

    @Value("${app.indiamart.url}")
    private String indiamartUrl;

    private static final DateTimeFormatter INDIAMART_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy",
            Locale.ENGLISH);

    public List<Long> getCompanyUserIds(Long userId, String role) {
        User user = userRepository.findById(userId).orElse(null);
        Long adminId = authUtil.getCompanyAdminId(user);
        if (adminId == null) adminId = userId;

        Long selectedTmId = authUtil.getSelectedTeamMemberId();
        if (selectedTmId != null) {
            Optional<TeamMember> tmOpt = teamMemberRepository.findById(selectedTmId);
            if (tmOpt.isPresent() && adminId.equals(tmOpt.get().getUserIdFk())) {
                Optional<User> uOpt = userRepository.findByUserEmail(tmOpt.get().getTeamMemberEmail());
                if (uOpt.isPresent()) {
                    return List.of(uOpt.get().getUserid());
                }
                return List.of(selectedTmId);
            }
        }

        List<Long> userIds = new ArrayList<>();
        if (!userIds.contains(adminId)) {
            userIds.add(adminId);
        }
        if (user != null && user.getUserid() != null && !userIds.contains(user.getUserid())) {
            userIds.add(user.getUserid());
        }

        List<TeamMember> teamMembers = teamMemberRepository.findByUserIdFk(adminId);
        for (TeamMember tm : teamMembers) {
            if (tm.getTeamMemberId() != null && !userIds.contains(tm.getTeamMemberId())) {
                userIds.add(tm.getTeamMemberId());
            }
            if (tm.getTeamMemberEmail() != null && !tm.getTeamMemberEmail().isBlank()) {
                userRepository.findByUserEmail(tm.getTeamMemberEmail())
                        .ifPresent(u -> {
                            if (!userIds.contains(u.getUserid())) {
                                userIds.add(u.getUserid());
                            }
                        });
            }
        }
        return userIds;
    }

    private void populateCreatorInfoIfMissing(List<Lead> leads) {
        Map<Long, String> userDisplayMap = new HashMap<>();
        for (Lead lead : leads) {
            if (lead.getCreatedBy() == null || lead.getCreatedBy().isBlank()) {
                if (lead.getUserIdFk() != null) {
                    String display = userDisplayMap.computeIfAbsent(lead.getUserIdFk(), id -> {
                        return userRepository.findById(id)
                                .map(u -> u.getUserEmail() != null ? u.getUserEmail() : u.getUsername())
                                .orElse("Admin");
                    });
                    lead.setCreatedBy(display);
                } else {
                    lead.setCreatedBy("Admin");
                }
            }
            if (lead.getUpdatedBy() == null || lead.getUpdatedBy().isBlank()) {
                lead.setUpdatedBy(lead.getCreatedBy() != null ? lead.getCreatedBy() : "Admin");
            }
        }
    }

    public List<Lead> getAllLeads(Long userId, String role) {
        List<Lead> leads;
        User user = userRepository.findById(userId).orElse(null);
        String scopeMode = authUtil.resolveDataScopeMode(user, "LEADS");

        Long selectedTmId = authUtil.getSelectedTeamMemberId();
        if (selectedTmId != null && !authUtil.isSuperAdmin(role)) {
            Optional<TeamMember> tmOpt = teamMemberRepository.findById(selectedTmId);
            if (!tmOpt.isPresent()) {
                Optional<User> targetUser = userRepository.findById(selectedTmId);
                if (targetUser.isPresent()) {
                    tmOpt = teamMemberRepository.findByTeamMemberEmail(targetUser.get().getUserEmail());
                }
            }
            if (tmOpt.isPresent()) {
                TeamMember tm = tmOpt.get();
                List<Long> targetUserIds = new ArrayList<>();
                if (tm.getTeamMemberId() != null) targetUserIds.add(tm.getTeamMemberId());
                if (userRepository.findByUserEmail(tm.getTeamMemberEmail()).isPresent()) {
                    targetUserIds.add(userRepository.findByUserEmail(tm.getTeamMemberEmail()).get().getUserid());
                }

                List<Long> targetTeamIds = new ArrayList<>();
                if (tm.getTeamIdFk() != null) targetTeamIds.add(tm.getTeamIdFk());
                createTeamRepository.findByTeamMemberIdFk(tm.getTeamMemberId()).forEach(ct -> {
                    if (ct.getTeamIdFk() != null && !targetTeamIds.contains(ct.getTeamIdFk())) {
                        targetTeamIds.add(ct.getTeamIdFk());
                    }
                });

                List<String> targetEmails = new ArrayList<>();
                if (tm.getTeamMemberEmail() != null && !tm.getTeamMemberEmail().isBlank()) {
                    targetEmails.add(tm.getTeamMemberEmail().toLowerCase());
                }

                if (targetUserIds.isEmpty()) targetUserIds = List.of(-1L);
                if (targetTeamIds.isEmpty()) targetTeamIds = List.of(-1L);
                if (targetEmails.isEmpty()) targetEmails = List.of("__NONE__");

                leads = leadRepository.findByTeamLeadCriteria(targetUserIds, targetTeamIds, targetEmails);
                populateCreatorInfoIfMissing(leads);
                return leads;
            }
        }

        if (authUtil.isSuperAdmin(role)) {
            leads = leadRepository.findAll(Sort.by(Sort.Direction.DESC, "leadId"));
        } else if ("ALL_DATA".equals(scopeMode)) {
            List<Long> userIds = getCompanyUserIds(userId, role);
            leads = leadRepository.findByUserIdFkInOrLeadAssignedMemberIn(userIds);
        } else if ("TEAM_DATA".equals(scopeMode)) {
            List<Long> teamUserIds = authUtil.getTeamLeadMemberUserIds(user);
            List<Long> teamIds = authUtil.getTeamLeadTeamIds(user);
            List<String> memberEmails = authUtil.getTeamLeadMemberEmails(user);
            if (teamUserIds.isEmpty()) teamUserIds = List.of(-1L);
            if (teamIds.isEmpty()) teamIds = List.of(-1L);
            if (memberEmails.isEmpty()) memberEmails = List.of("__NONE__");
            leads = leadRepository.findByTeamLeadCriteria(teamUserIds, teamIds, memberEmails);
        } else {
            // OWN_DATA_ONLY
            Optional<TeamMember> tmOpt = teamMemberRepository.findByTeamMemberEmail(user != null ? user.getUserEmail() : "");
            Long teamMemberId = tmOpt.map(TeamMember::getTeamMemberId).orElse(null);
            String teamMemberName = tmOpt.map(TeamMember::getTeamMemberName).orElse(null);
            String userEmail = user != null ? user.getUserEmail() : null;

            leads = leadRepository.findByOwnDataCriteria(userId, teamMemberId, userEmail, teamMemberName);
        }
        populateCreatorInfoIfMissing(leads);
        return leads;
    }


    public Lead getLeadById(Long id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead", "id", id));
    }

    public Lead createLead(LeadRequest request, Long userId,
            MultipartFile doc, MultipartFile doc1,
            MultipartFile doc2, MultipartFile doc3) throws IOException {
        Lead lead = mapToEntity(request, new Lead());
        User currentUser = userRepository.findById(userId).orElse(null);
        Long companyAdminId = authUtil.getCompanyAdminId(currentUser);
        if (companyAdminId == null) companyAdminId = userId;
        lead.setUserIdFk(companyAdminId);

        lead.setLeadCreatedDate(LocalDateTime.now());
        lead.setUpdatedDate(LocalDateTime.now());

        String creatorDisplay = currentUser != null && currentUser.getUserEmail() != null ? currentUser.getUserEmail() : "Admin";
        if (request.getCreatedBy() != null && !request.getCreatedBy().isBlank()) {
            lead.setCreatedBy(request.getCreatedBy());
        } else {
            lead.setCreatedBy(creatorDisplay);
        }
        lead.setUpdatedBy(lead.getCreatedBy());

        lead.setUploadDocument(fileUploadUtil.upload(doc));
        lead.setUploadDocument1(fileUploadUtil.upload(doc1));
        lead.setUploadDocument2(fileUploadUtil.upload(doc2));
        lead.setUploadDocument3(fileUploadUtil.upload(doc3));
        Lead saved = leadRepository.save(lead);
        if ("Qualified".equals(saved.getEnquiryType())) {
            createSalesTaskIfNotExist(saved, saved.getUserIdFk() != null ? saved.getUserIdFk() : userId);
        }
        if ("Won".equals(saved.getLeadOutcomeStatus())) {
            createProjectTaskIfNotExist(saved, saved.getUserIdFk() != null ? saved.getUserIdFk() : userId);
        }
        leadScoringService.scoreAndCache(saved.getLeadId());
        return saved;
    }


    // public Lead updateLead(Long id, LeadRequest request, Long userId,
    //         MultipartFile doc, MultipartFile doc1,
    //         MultipartFile doc2, MultipartFile doc3) throws IOException {
    //     Lead lead = getLeadById(id);
    //     saveNegotiationRevision(lead);
    //     mapToEntity(request, lead);
    //     if ("Qualified".equalsIgnoreCase(lead.getLeadStatus())) {
    //         if (lead.getLeadOutcomeStatus() == null
    //                 || lead.getLeadOutcomeStatus().isBlank()) {
    //             lead.setLeadOutcomeStatus("Open");
    //         }
    //         if (lead.getEnquiryStatus() == null
    //                 || lead.getEnquiryStatus().isBlank()) {
    //             lead.setEnquiryStatus("Pending");
    //         }
    //     }
    //     if ("Disqualified".equalsIgnoreCase(lead.getLeadStatus())) {
    //         lead.setLeadOutcomeStatus(null);
    //         lead.setEnquiryStatus(null);
    //     }
    //     if (request.getUserIdFk() != null) {
    //         lead.setUserIdFk(request.getUserIdFk());
    //     }
    //     if (doc != null && !doc.isEmpty()) {
    //         String path = fileUploadUtil.upload(doc);
    //         System.out.println("UPLOAD PATH = " + path);
    //         lead.setUploadDocument(path);
    //     }
    //     if (doc1 != null && !doc1.isEmpty()) {
    //         lead.setUploadDocument1(fileUploadUtil.upload(doc1));
    //     }
    //     if (doc2 != null && !doc2.isEmpty()) {
    //         lead.setUploadDocument2(fileUploadUtil.upload(doc2));
    //     }
    //     if (doc3 != null && !doc3.isEmpty()) {
    //         lead.setUploadDocument3(fileUploadUtil.upload(doc3));
    //     }
    //     Lead saved = leadRepository.save(lead);
    //     return saved;
    // }
    public Lead updateLead(Long id, LeadRequest request, Long userId,
            MultipartFile doc, MultipartFile doc1,
            MultipartFile doc2, MultipartFile doc3) throws IOException {

        Lead lead = getLeadById(id);
        
        // Save current state as revision before updating

        // Update Lead fields
        mapToEntity(request, lead);

        // Handle status transitions
        if ("Qualified".equalsIgnoreCase(lead.getLeadStatus())) {
            if (lead.getLeadOutcomeStatus() == null || lead.getLeadOutcomeStatus().isBlank()) {
                lead.setLeadOutcomeStatus("Open");
            }
            if (lead.getEnquiryStatus() == null || lead.getEnquiryStatus().isBlank()) {
                lead.setEnquiryStatus("Pending");
            }
        }
        lead.setQuotationNumber(request.getQuotationNumber());
        if ("Disqualified".equalsIgnoreCase(lead.getLeadStatus())) {
            lead.setLeadOutcomeStatus(null);
            lead.setEnquiryStatus(null);
        }

        if (lead.getUserIdFk() == null) {
            User currentUser = userRepository.findById(userId).orElse(null);
            Long companyAdminId = authUtil.getCompanyAdminId(currentUser);
            lead.setUserIdFk(companyAdminId != null ? companyAdminId : userId);
        }

        // Upload Documents with correct relative path for /api/view/ endpoint
        if (doc != null && !doc.isEmpty()) {
            String docUrl = uploadFileWithRelativePath(doc, lead);
            lead.setUploadDocument(docUrl);
        }

        if (doc1 != null && !doc1.isEmpty()) {
            String docUrl = uploadFileWithRelativePath(doc1, lead);
            lead.setUploadDocument1(docUrl);
        }

        if (doc2 != null && !doc2.isEmpty()) {
            String docUrl = uploadFileWithRelativePath(doc2, lead);
            lead.setUploadDocument2(docUrl);
        }

        if (doc3 != null && !doc3.isEmpty()) {
            String docUrl = uploadFileWithRelativePath(doc3, lead);
            lead.setUploadDocument3(docUrl);
        }

        User currentUser = userRepository.findById(userId).orElse(null);
        if (currentUser != null && currentUser.getUserEmail() != null) {
            lead.setUpdatedBy(currentUser.getUserEmail());
        }
        lead.setUpdatedDate(LocalDateTime.now());

        // Save Lead
        Lead saved = leadRepository.save(lead);

        saveNegotiationRevision(lead);

        // ==========================
        // Sync Negotiation Table
        // ==========================
        syncNegotiationForLead(saved);

        return saved;
    }

    public void syncNegotiationForLead(Lead lead) {
        if (lead == null || lead.getLeadId() == null) return;

        boolean isNegotiation = "Negotiation".equalsIgnoreCase(lead.getLeadStatus())
                || "Negotiation".equalsIgnoreCase(lead.getLeadOutcomeStatus());

        if (isNegotiation) {
            Negotiation negotiation = negotiationRepository
                    .findFirstByLeadIdFk(lead.getLeadId())
                    .orElse(null);

            if (negotiation == null) {
                negotiation = Negotiation.builder()
                        .leadIdFk(lead.getLeadId())
                        .negotiationName(lead.getLeadOrganisationName())
                        .negotiationTitle(lead.getLeadTitle())
                        .quotationNo(lead.getQuotationNumber())
                        .quotationRevision(lead.getQuotationRevision())
                        .quotationAmount(lead.getQuotationAmount())
                        .remarks(lead.getFollowUpRemark())
                        .negotiationStatus("Negotiation")
                        .userIdFk(lead.getUserIdFk())
                        .build();
            } else {
                negotiation.setNegotiationName(lead.getLeadOrganisationName());
                negotiation.setNegotiationTitle(lead.getLeadTitle());
                negotiation.setQuotationNo(lead.getQuotationNumber());
                negotiation.setQuotationRevision(lead.getQuotationRevision());
                negotiation.setQuotationAmount(lead.getQuotationAmount());
                negotiation.setRemarks(lead.getFollowUpRemark());
                if (lead.getLeadOutcomeStatus() != null && !lead.getLeadOutcomeStatus().isBlank()) {
                    negotiation.setNegotiationStatus(lead.getLeadOutcomeStatus());
                } else if (lead.getLeadStatus() != null && !lead.getLeadStatus().isBlank()) {
                    negotiation.setNegotiationStatus(lead.getLeadStatus());
                }
            }
            negotiationRepository.save(negotiation);
        }
    }

    /**

     * Save current state as a negotiation revision before updating
     */
    private void saveNegotiationRevision(Lead lead) {
        // Get the current negotiation
        Negotiation negotiation = negotiationRepository
                .findFirstByLeadIdFk(lead.getLeadId())
                .orElse(null);

        if (negotiation == null) {
            return;
        }

        // Create revision from current state
        NegotiationRevision revision = new NegotiationRevision();

        // Set basic IDs
        revision.setNegotiationId(negotiation.getId());
        revision.setLeadIdFk(lead.getLeadId());

        // Save Lead values (current state)
        revision.setQuotationNo(lead.getQuotationNumber());
        revision.setQuotationRevision(lead.getQuotationRevision());
        revision.setQuotationAmount(lead.getQuotationAmount());
        revision.setRemarks(lead.getFollowUpRemark());
        revision.setEnquiryDescription(lead.getEnquiryDescription());
        revision.setQuotationDate(lead.getQuotationDate());
        
        // Save Negotiation values (current state)
        revision.setNegotiationStatus(negotiation.getNegotiationStatus());
        revision.setUserIdFk(negotiation.getUserIdFk());

        // Set revision timestamp
        revision.setUpdatedDate(LocalDateTime.now());

        // Save revision
        negotiationRevisionRepository.save(revision);
        
        log.info("Saved negotiation revision for lead ID: {}, Negotiation ID: {}", 
                 lead.getLeadId(), negotiation.getId());
    }

    /**
     * Upload file and return relative path for /api/view/ endpoint
     */
    private String uploadFileWithRelativePath(MultipartFile file, Lead lead) throws IOException {
        // Generate a unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        
        // Determine subdirectory based on quotation number
        String quotationNo = lead.getQuotationNumber();
        String subDirectory;
        
        if (quotationNo != null && !quotationNo.isEmpty()) {
            // Use quotation number - replace slashes with underscores for filesystem
            String sanitizedQuotation = quotationNo.replace("/", "_");
            subDirectory = "quotation/" + sanitizedQuotation;
        } else {
            // If no quotation number, use lead ID
            subDirectory = "lead/" + lead.getLeadId();
        }
        
        // Upload file to filesystem
        String fullUrl = fileUploadUtil.uploadFile(file, subDirectory);
        
        // Extract relative path for /api/view/ endpoint
        String relativePath = extractRelativePathForView(fullUrl, quotationNo);
        
        log.info("File uploaded: {} -> Relative path: {}", fullUrl, relativePath);
        
        return relativePath;
    }

    /**
     * Extract relative path for /api/view/ endpoint
     */
    private String extractRelativePathForView(String fileUrl, String quotationNo) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return fileUrl;
        }
        
        String relativePath = fileUrl;
        
        // Remove "files/" prefix if present
        if (relativePath.startsWith("files/")) {
            relativePath = relativePath.substring(6);
        }
        
        // If it's a full URL with domain, extract the path
        if (fileUrl.contains("://")) {
            // Try to find /api/view/
            int apiViewIndex = fileUrl.indexOf("/api/view/");
            if (apiViewIndex != -1) {
                relativePath = fileUrl.substring(apiViewIndex + 10);
            } else {
                // Try to find /api/
                int apiIndex = fileUrl.indexOf("/api/");
                if (apiIndex != -1) {
                    relativePath = fileUrl.substring(apiIndex + 5);
                } else {
                    // Try to find /files/
                    int filesIndex = fileUrl.indexOf("/files/");
                    if (filesIndex != -1) {
                        relativePath = fileUrl.substring(filesIndex + 7);
                    }
                }
            }
        } else if (fileUrl.contains("/api/view/")) {
            relativePath = fileUrl.substring(fileUrl.indexOf("/api/view/") + 10);
        } else if (fileUrl.contains("/api/")) {
            relativePath = fileUrl.substring(fileUrl.indexOf("/api/") + 5);
        } else if (fileUrl.contains("/files/")) {
            relativePath = fileUrl.substring(fileUrl.indexOf("/files/") + 7);
        }
        
        // Remove leading slash
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        
        // Remove "files/" if it's at the beginning
        if (relativePath.startsWith("files/")) {
            relativePath = relativePath.substring(6);
        }
        
        // Convert underscores back to slashes in the quotation number part
        if (quotationNo != null && !quotationNo.isEmpty()) {
            String quotationWithUnderscores = quotationNo.replace("/", "_");
            if (relativePath.contains("quotation/" + quotationWithUnderscores)) {
                String pathWithUnderscores = "quotation/" + quotationWithUnderscores;
                String pathWithSlashes = "quotation/" + quotationNo;
                relativePath = relativePath.replace(pathWithUnderscores, pathWithSlashes);
            }
        }
        
        return relativePath;
    }
    @Transactional
    public void deleteLead(Long id) {
        Lead lead = getLeadById(id);
        leadNoteRepository.deleteByLeadIdFk(id);
        leadReminderRepository.deleteByLeadIdFk(id);
        leadScoreRepository.deleteByLeadIdFk(id);
        opportunityRepository.deleteByLeadIdFk(id);
        leadRepository.delete(lead);
    }

    public Lead updateLeadStatus(Long id, String status, User user) {
        Lead lead = getLeadById(id);
        lead.setLeadStatus(status);
        if (user != null && user.getUserEmail() != null) {
            lead.setUpdatedBy(user.getUserEmail());
        }
        lead.setUpdatedDate(LocalDateTime.now());

        if ("Qualified".equalsIgnoreCase(status)) {
            lead.setEnquiryType("Qualified");
            lead.setLeadOutcomeStatus("Open");
            lead.setEnquiryStatus("Pending");
            createSalesTaskIfNotExist(lead, lead.getUserIdFk() != null ? lead.getUserIdFk() : 1L);
        } else if ("Negotiation".equalsIgnoreCase(status)) {
            lead.setLeadOutcomeStatus("Negotiation");
        } else if ("Disqualified".equalsIgnoreCase(status)) {
            lead.setEnquiryType("Disqualified");
            lead.setLeadOutcomeStatus(null);
            lead.setEnquiryStatus(null);
        } else if ("Won".equalsIgnoreCase(status)) {
            createProjectTaskIfNotExist(lead, lead.getUserIdFk() != null ? lead.getUserIdFk() : 1L);
        }

        Lead saved = leadRepository.save(lead);
        syncNegotiationForLead(saved);
        leadScoringService.scoreAndCache(saved.getLeadId());
        return saved;

    }

    public Lead updateLeadStatus(Long id, String status) {
        return updateLeadStatus(id, status, null);
    }

    public Lead updateLeadGroup(Long id, String group, User user) {
        Lead lead = getLeadById(id);
        lead.setLeadGroup(group);
        if (user != null && user.getUserEmail() != null) {
            lead.setUpdatedBy(user.getUserEmail());
        }
        lead.setUpdatedDate(LocalDateTime.now());
        return leadRepository.save(lead);
    }

    public Lead updateLeadGroup(Long id, String group) {
        return updateLeadGroup(id, group, null);
    }

    public Lead updateLeadEnquiryStatus(Long id, String status, User user) {
        Lead lead = getLeadById(id);
        lead.setEnquiryStatus(status);
        if (user != null && user.getUserEmail() != null) {
            lead.setUpdatedBy(user.getUserEmail());
        }
        lead.setUpdatedDate(LocalDateTime.now());
        return leadRepository.save(lead);
    }

    public Lead updateLeadEnquiryStatus(Long id, String status) {
        return updateLeadEnquiryStatus(id, status, null);
    }

    public Lead updateLeadOutcomeStatus(Long id, String leadOutcomeStatus, User user) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + id));
        lead.setLeadOutcomeStatus(leadOutcomeStatus);
        if (user != null && user.getUserEmail() != null) {
            lead.setUpdatedBy(user.getUserEmail());
        }
        lead.setUpdatedDate(LocalDateTime.now());

        Lead saved = leadRepository.save(lead);


        if ("Negotiation".equalsIgnoreCase(leadOutcomeStatus)) {

            Negotiation negotiation = negotiationRepository
                    .findFirstByLeadIdFk(saved.getLeadId())
                    .orElse(null);

            if (negotiation == null) {

                negotiation = Negotiation.builder()
                        .leadIdFk(saved.getLeadId())
                        .negotiationName(saved.getLeadOrganisationName())
                        .negotiationTitle(saved.getLeadTitle())
                        .quotationNo(saved.getQuotationNumber())
                        .quotationRevision(saved.getQuotationRevision())
                        .quotationAmount(saved.getQuotationAmount())
                        .remarks(saved.getFollowUpRemark())
                        .negotiationStatus("Negotiation")
                        .userIdFk(saved.getUserIdFk())
                        .build();

            } else {

                negotiation.setNegotiationName(saved.getLeadOrganisationName());
                negotiation.setNegotiationTitle(saved.getLeadTitle());
                negotiation.setQuotationNo(saved.getQuotationNumber());
                negotiation.setQuotationRevision(saved.getQuotationRevision());
                negotiation.setQuotationAmount(saved.getQuotationAmount());
                negotiation.setRemarks(saved.getFollowUpRemark());
                negotiation.setNegotiationStatus("Negotiation");
            }

            negotiationRepository.save(negotiation);
        }

        return saved;
    }

    public Lead updateLeadOutcomeStatus(Long id, String leadOutcomeStatus) {
        return updateLeadOutcomeStatus(id, leadOutcomeStatus, null);
    }


    public List<Lead> getLeadsByStatus(String status, Long userId, String role) {
        List<Lead> leads;
        if (authUtil.isSuperAdmin(role)) {
            leads = leadRepository.findByLeadStatus(status);
        } else if (authUtil.isAdmin(role)) {
            List<Long> userIds = getCompanyUserIds(userId, role);
            leads = leadRepository.findByUserIdFkInAndLeadStatus(userIds, status);
        } else if (authUtil.isTeamLead(role)) {
            User user = userRepository.findById(userId).orElse(null);
            List<Long> teamUserIds = authUtil.getTeamLeadMemberUserIds(user);
            leads = leadRepository.findByUserIdFkInAndLeadStatus(teamUserIds, status);
        } else {
            User user = userRepository.findById(userId).orElse(null);
            Optional<TeamMember> tmOpt = teamMemberRepository.findByTeamMemberEmail(user != null ? user.getUserEmail() : "");
            Long teamMemberId = tmOpt.map(TeamMember::getTeamMemberId).orElse(null);
            String teamMemberName = tmOpt.map(TeamMember::getTeamMemberName).orElse(null);
            String userEmail = user != null ? user.getUserEmail() : null;

            leads = leadRepository.findByOwnDataCriteriaAndStatus(userId, teamMemberId, userEmail, teamMemberName, status);
        }
        populateCreatorInfoIfMissing(leads);
        return leads;
    }



    public List<LeadNote> getNotes(Long leadId) {
        getLeadById(leadId);
        return leadNoteRepository.findByLeadIdFkOrderByNoteDateDesc(leadId);
    }

    public List<LeadNote> getAllNotes() {
        return leadNoteRepository.findAllByOrderByNoteDateDesc();
    }

    public LeadNote addNote(Long leadId, String noteText, Long userId) {
        getLeadById(leadId);
        LeadNote note = LeadNote.builder()
                .leadIdFk(leadId)
                .noteText(noteText)
                .noteDate(LocalDateTime.now())
                .userIdFk(userId)
                .build();
        return leadNoteRepository.save(note);
    }

    public List<LeadReminder> getReminders(Long leadId) {
        getLeadById(leadId);
        return leadReminderRepository.findByLeadIdFkOrderByReminderDate(leadId);
    }

    public LeadReminder addReminder(Long leadId, String reminderText, String reminderDate, Long userId) {
        getLeadById(leadId);
        LeadReminder reminder = LeadReminder.builder()
                .leadIdFk(leadId)
                .reminderText(reminderText)
                .reminderDate(reminderDate != null
                        ? java.time.LocalDateTime
                                .parse(reminderDate.length() == 10 ? reminderDate + "T00:00:00" : reminderDate)
                        : LocalDateTime.now())
                .userIdFk(userId)
                .build();
        return leadReminderRepository.save(reminder);
    }

    public Opportunity convertToOpportunity(Long leadId, Long userId) {
        Lead lead = getLeadById(leadId);
        if (AppConstants.LEAD_STATUS_CONVERTED.equals(lead.getLeadStatus())) {
            throw new BadRequestException("Lead is already converted");
        }
        lead.setLeadStatus(AppConstants.LEAD_STATUS_CONVERTED);
        leadRepository.save(lead);

        Opportunity opp = Opportunity.builder()
                .oppName(lead.getLeadFirstName() + " " + lead.getLeadLastName())
                .oppTitle(lead.getLeadTitle())
                .oppStatus(AppConstants.OPP_STATUS_OPEN)
                .leadIdFk(leadId)
                .userIdFk(userId)
                .build();
        return opportunityRepository.save(opp);
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public List<Lead> importFromIndiamart(ImportLeadRequest request, Long userId) {
        validateImportRequest(request);

        try {
            String finalUrl = indiamartUrl;
            String finalApiKey = indiamartApiKey;

            Optional<IntegrationConfig> configOpt = integrationConfigRepository.findByNameAndUserIdFk("INDIAMART",
                    userId);
            if (configOpt.isPresent() && configOpt.get().isEnabled()) {
                IntegrationConfig config = configOpt.get();
                if (config.getApiUrl() != null && !config.getApiUrl().trim().isEmpty()) {
                    finalUrl = config.getApiUrl();
                }
                if (config.getApiKey() != null && !config.getApiKey().trim().isEmpty()) {
                    finalApiKey = config.getApiKey();
                }
            }

            String url = UriComponentsBuilder.fromHttpUrl(finalUrl)
                    .queryParam("glusr_crm_key", finalApiKey)
                    .queryParam("start_time", formatIndiamartDate(request.getFromDate()))
                    .queryParam("end_time", formatIndiamartDate(request.getToDate()))
                    .build()
                    .toUriString();

            Map<String, Object> response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> data = extractIndiamartLeads(response);
            List<Lead> imported = new ArrayList<>();

            for (Map<String, Object> item : data) {
                String queryId = text(item, "UNIQUE_QUERY_ID");
                if (queryId.isBlank() || leadRepository.existsByUniqueQueryId(queryId)) {
                    continue;
                }

                Lead lead = Lead.builder()
                        .leadFirstName(text(item, "SENDER_NAME"))
                        .leadEmail(text(item, "SENDER_EMAIL"))
                        .leadMobileNo(text(item, "SENDER_MOBILE"))
                        .leadPhoneNo(text(item, "SENDER_PHONE"))
                        .leadOrganisationName(text(item, "SENDER_COMPANY"))
                        .leadAddress(text(item, "SENDER_ADDRESS"))
                        .leadCity(text(item, "SENDER_CITY"))
                        .leadState(text(item, "SENDER_STATE"))
                        .leadCountry(text(item, "SENDER_COUNTRY_ISO"))
                        .leadTitle(firstPresent(item, "SUBJECT", "QUERY_PRODUCT_NAME"))
                        .leadReason(text(item, "QUERY_MESSAGE"))
                        .uniqueQueryId(queryId)
                        .leadSource(AppConstants.INDIAMART_SOURCE)
                        .leadType(AppConstants.INDIAMART_DEFAULT_TYPE)
                        .leadStatus(AppConstants.INDIAMART_DEFAULT_STATUS)
                        .inquiryDate(parseInquiryDate(item))
                        .leadCreatedDate(LocalDateTime.now())
                        .userIdFk(userId)
                        .build();
                Lead saved = leadRepository.save(lead);
                refreshLeadScore(saved);
                imported.add(saved);
            }

            log.info("Imported {} new Indiamart lead(s) between {} and {}",
                    imported.size(), request.getFromDate(), request.getToDate());
            return imported;
        } catch (Exception e) {
            log.error("Indiamart import error", e);
            throw new BadRequestException("Failed to import leads from Indiamart: " + e.getMessage());
        }
    }

    private void validateImportRequest(ImportLeadRequest request) {
        if (request.getFromDate() == null || request.getToDate() == null) {
            throw new BadRequestException("Both From Date and To Date are required.");
        }
        if (request.getFromDate().isAfter(request.getToDate())) {
            throw new BadRequestException("From Date cannot be later than To Date.");
        }
        if (indiamartApiKey == null || indiamartApiKey.isBlank()) {
            throw new BadRequestException("Indiamart API key is not configured.");
        }
        if (indiamartUrl == null || indiamartUrl.isBlank()) {
            throw new BadRequestException("Indiamart API URL is not configured.");
        }
    }

    private String formatIndiamartDate(LocalDate date) {
        return INDIAMART_DATE_FORMAT.format(date).toUpperCase(Locale.ENGLISH);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractIndiamartLeads(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return List.of();
        }

        Object code = response.get("CODE");
        if (code != null && !isSuccessfulCode(code)) {
            String message = firstPresent(response, "MESSAGE", "STATUS", "ERROR_MESSAGE");
            throw new BadRequestException(message.isBlank() ? "Indiamart returned error code " + code : message);
        }

        Object leads = response.get("RESPONSE");
        if (!(leads instanceof List)) {
            leads = response.get("DATA");
        }
        if (!(leads instanceof List<?> list)) {
            return List.of();
        }

        List<Map<String, Object>> parsed = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                parsed.add((Map<String, Object>) map);
            }
        }
        return parsed;
    }

    private String firstPresent(Map<String, Object> item, String... keys) {
        for (String key : keys) {
            String value = text(item, key);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String text(Map<String, Object> item, String key) {
        Object value = item.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private boolean isSuccessfulCode(Object code) {
        if (code instanceof Number number) {
            return number.intValue() == 200;
        }
        return "200".equals(String.valueOf(code).trim());
    }

    private LocalDate parseInquiryDate(Map<String, Object> item) {
        String value = firstPresent(item, "QUERY_TIME", "QUERY_DATE", "DATE_RE");
        if (value.isBlank()) {
            return LocalDate.now();
        }

        List<DateTimeFormatter> formats = List.of(
                caseInsensitiveFormatter("dd-MMM-yyyy"),
                caseInsensitiveFormatter("dd-MMM-yyyy HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH),
                DateTimeFormatter.ISO_LOCAL_DATE);

        for (DateTimeFormatter formatter : formats) {
            try {
                if (formatter == DateTimeFormatter.ISO_LOCAL_DATE) {
                    return LocalDate.parse(value, formatter);
                }
                if (value.length() > 11) {
                    return LocalDateTime.parse(value, formatter).toLocalDate();
                }
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next known IndiaMART date shape.
            }
        }
        return LocalDate.now();
    }

    private DateTimeFormatter caseInsensitiveFormatter(String pattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .toFormatter(Locale.ENGLISH);
    }

    private void refreshLeadScore(Lead lead) {
        try {
            leadScoringService.scoreAndCache(lead.getLeadId());
        } catch (Exception e) {
            log.warn("Lead {} imported but score refresh failed: {}", lead.getLeadId(), e.getMessage());
        }
    }

    private Lead mapToEntity(LeadRequest req, Lead lead) {
        lead.setLeadFirstName(req.getLeadFirstName());
        lead.setLeadLastName(req.getLeadLastName());
        lead.setLeadTitle(req.getLeadTitle());
        lead.setLeadAddress(req.getLeadAddress());
        lead.setLeadCity(req.getLeadCity());
        lead.setLeadState(req.getLeadState());
        lead.setLeadCountry(req.getLeadCountry());
        lead.setLeadMobileNo(req.getLeadMobileNo());
        lead.setLeadPhoneNo(req.getLeadPhoneNo());
        lead.setLeadEmail(req.getLeadEmail());
        lead.setLeadOrganisationName(req.getLeadOrganisationName());
        lead.setLeadWebsite(req.getLeadWebsite());
        lead.setLeadIndustry(req.getLeadIndustry());
        lead.setNoOfEmployee(req.getNoOfEmployee());
        lead.setLeadStatus(req.getLeadStatus());
        lead.setLeadSource(req.getLeadSource());
        lead.setLeadType(req.getLeadType());
        lead.setLeadReason(req.getLeadReason());
        lead.setDesignation(req.getDesignation());
        lead.setInquiryDate(req.getInquiryDate());
        lead.setLeadAssignedTeam(req.getLeadAssignedTeam());
        lead.setLeadAssignedMember(req.getLeadAssignedMember());
        if (req.getLeadRating() != null) {
            lead.setLeadRating(req.getLeadRating());
        }

        // New fields
        lead.setEnquiryDescription(req.getEnquiryDescription());
        if (req.getEnquiryType() != null) {
            lead.setEnquiryType(req.getEnquiryType());
        }
        lead.setCompanyContactPersonName(req.getCompanyContactPersonName());
        lead.setQuotationNumber(req.getQuotationNumber());
        lead.setQuotationDate(req.getQuotationDate());
        lead.setQuotationSentDate(req.getQuotationSentDate());
        lead.setQuotationAmount(req.getQuotationAmount());
        lead.setFollowUpRemark(req.getFollowUpRemark());
        lead.setOngoingPriority(req.getOngoingPriority());
        lead.setLeadGroup(req.getLeadGroup());
        lead.setLeadRef(req.getLeadRef());
        lead.setEnquiryStatus(req.getEnquiryStatus());

        if (req.getLeadOutcomeStatus() != null
                && !req.getLeadOutcomeStatus().isBlank()) {
            lead.setLeadOutcomeStatus(req.getLeadOutcomeStatus());
        }

        String quotationNumber = lead.getQuotationNumber(); // current DB value
        String quotationRevision = req.getQuotationRevision();

        if (quotationNumber != null && quotationRevision != null) {
            quotationNumber = quotationNumber.replaceAll("/R\\d+$", "");
            quotationNumber = quotationNumber + "/" + quotationRevision;

            lead.setQuotationNumber(quotationNumber);
            lead.setQuotationRevision(quotationRevision);
        }

        lead.setQuotationNumber(quotationNumber);
        lead.setQuotationRevision(quotationRevision);

        // lead.setQuotationRevision(req.getQuotationRevision());
        // Adjust lead status based on enquiry type
        if ("Qualified".equals(req.getEnquiryType())) {
            String currentStatus = lead.getLeadStatus();
            if (currentStatus == null
                    || currentStatus.equals("New Lead")
                    || currentStatus.equals("NotContacted")
                    || currentStatus.equals("Contacted")
                    || currentStatus.equals("Working")
                    || currentStatus.isEmpty()) {
                lead.setLeadStatus("Qualified");
            }
        } else if ("Disqualified".equals(req.getEnquiryType())) {
            lead.setLeadStatus("Disqualified");
        }

        return lead;
    }

    private void createSalesTaskIfNotExist(Lead lead, Long userId) {
        String relatedTo = "Lead #" + lead.getLeadId();
        List<Task> existing = taskRepository.findByTaskRelatedTo(relatedTo);
        boolean hasSalesTask = existing.stream()
                .anyMatch(t -> "Sales Call".equals(t.getTaskType()) || "Sales".equals(t.getTaskType()));
        if (!hasSalesTask) {
            String clientName = (lead.getLeadFirstName() != null ? lead.getLeadFirstName() : "") + " "
                    + (lead.getLeadLastName() != null ? lead.getLeadLastName() : "");
            clientName = clientName.trim();
            if (clientName.isEmpty()) {
                clientName = "Client";
            }
            String company = lead.getLeadOrganisationName() != null ? lead.getLeadOrganisationName()
                    : "Unknown Company";

            Task task = Task.builder()
                    .taskName("Sales: Follow up with " + clientName + " (" + company + ")")
                    .taskAssignedMember(lead.getLeadAssignedMember())
                    .taskAssignedTo(lead.getLeadAssignedMember())
                    .taskAssign("To Do")
                    .taskStartDate(LocalDate.now().toString())
                    .taskDueDate(LocalDate.now().plusDays(2).toString())
                    .taskRelatedTo(relatedTo)
                    .taskDescription("Auto-generated Sales Task for Qualified Lead.\n"
                            + "Enquiry Details:\n"
                            + "Description: " + (lead.getEnquiryDescription() != null ? lead.getEnquiryDescription() : "")
                            + "\n"
                            + "Contact Person: "
                            + (lead.getCompanyContactPersonName() != null ? lead.getCompanyContactPersonName() : "")
                            + "\n"
                            + "Phone: " + (lead.getLeadMobileNo() != null ? lead.getLeadMobileNo() : "") + "\n"
                            + "Email: " + (lead.getLeadEmail() != null ? lead.getLeadEmail() : ""))
                    .taskPriority("High")
                    .taskPercentageCompleted(0)
                    .taskType("Sales Call")
                    .taskPhone(lead.getLeadMobileNo())
                    .taskEmail(lead.getLeadEmail())
                    .userIdFk(userId)
                    .taskCreatedBy("System")
                    .build();
            taskRepository.save(task);
        }
    }

    private void createProjectTaskIfNotExist(Lead lead, Long userId) {
        String relatedTo = "Lead #" + lead.getLeadId();
        List<Task> existing = taskRepository.findByTaskRelatedTo(relatedTo);
        boolean hasProjectTask = existing.stream()
                .anyMatch(t -> "Development".equals(t.getTaskType()) || "Project".equals(t.getTaskType()));
        if (!hasProjectTask) {
            String clientName = (lead.getLeadFirstName() != null ? lead.getLeadFirstName() : "") + " "
                    + (lead.getLeadLastName() != null ? lead.getLeadLastName() : "");
            clientName = clientName.trim();
            if (clientName.isEmpty()) {
                clientName = "Client";
            }
            String company = lead.getLeadOrganisationName() != null ? lead.getLeadOrganisationName()
                    : "Unknown Company";

            Task task = Task.builder()
                    .taskName("Project Delivery: " + clientName + " (" + company + ")")
                    .taskAssignedMember(lead.getLeadAssignedMember())
                    .taskAssignedTo(lead.getLeadAssignedMember())
                    .taskAssign("To Do")
                    .taskStartDate(LocalDate.now().toString())
                    .taskDueDate(LocalDate.now().plusWeeks(1).toString())
                    .taskRelatedTo(relatedTo)
                    .taskDescription("Auto-generated Project Task for WON Lead.\n"
                            + "Quotation Details:\n"
                            + "Quotation Number: "
                            + (lead.getQuotationNumber() != null ? lead.getQuotationNumber() : "N/A") + "\n"
                            + "Quotation Amount: "
                            + (lead.getQuotationAmount() != null ? lead.getQuotationAmount().toString() : "N/A") + "\n"
                            + "Quotation Date: "
                            + (lead.getQuotationDate() != null ? lead.getQuotationDate().toString() : "N/A"))
                    .taskPriority("High")
                    .taskPercentageCompleted(0)
                    .taskType("Development")
                    .taskPhone(lead.getLeadMobileNo())
                    .taskEmail(lead.getLeadEmail())
                    .userIdFk(userId)
                    .taskCreatedBy("System")
                    .build();
            taskRepository.save(task);
        }
    }

    public Negotiation convertToNegotiation(Long leadId, Long userId) {

        List<Negotiation> existing = negotiationRepository.findByLeadIdFk(leadId);

        if (!existing.isEmpty()) {
            throw new BadRequestException("Lead already converted to negotiation");
        }

        Lead lead = getLeadById(leadId);

        lead.setLeadOutcomeStatus("Negotiation");
        leadRepository.save(lead);

        Negotiation negotiation = Negotiation.builder()
                .leadIdFk(lead.getLeadId())
                .negotiationName(lead.getLeadOrganisationName())
                .negotiationTitle(lead.getLeadTitle())
                .quotationNo(lead.getQuotationNumber())
                .quotationRevision(lead.getQuotationRevision())
                .quotationAmount(lead.getQuotationAmount())
                .remarks(lead.getFollowUpRemark())
                .negotiationStatus("Negotiation")
                .userIdFk(userId)
                .build();

        return negotiationRepository.save(negotiation);
    }

 

    // Update Lead Rating
    @Transactional
    public Lead updateLeadRating(Long leadId, Integer rating) {
        Optional<Lead> leadOptional = leadRepository.findById(leadId);

        if (leadOptional.isEmpty()) {
            return null;
        }

        Lead lead = leadOptional.get();
        lead.setLeadRating(rating);
        // lead.setUpdatedAt(LocalDateTime.now());

        return leadRepository.save(lead);
    }
}
