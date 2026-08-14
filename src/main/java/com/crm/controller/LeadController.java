package com.crm.controller;

import com.crm.repository.LeadGroupRepository;
import com.crm.repository.LeadSourceRepository;
import com.crm.repository.LeadStatusRepository;
import com.crm.dto.request.ImportLeadRequest;
import com.crm.dto.request.LeadRequest;
import com.crm.dto.response.ApiResponse;
import com.crm.entity.LeadGroupsMaster;
import com.crm.entity.LeadSourceMaster;
import com.crm.entity.LeadStatusMaster;
import com.crm.entity.Lead;
import com.crm.entity.LeadNote;
import com.crm.entity.LeadReminder;
import com.crm.entity.Opportunity;
import com.crm.entity.User;
import com.crm.service.LeadService;
import com.crm.service.LeadReminderEmailService;
import com.crm.util.AuthUtil;
import com.crm.repository.LeadRepository;
import com.crm.repository.NegotiationRepository;
import com.crm.entity.Negotiation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leads")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;
    private final AuthUtil authUtil;
    private final LeadSourceRepository leadSourceRepository;
    private final LeadGroupRepository leadGroupRepository;
    private final LeadStatusRepository leadStatusRepository;

    private final LeadRepository leadRepository;
    private final NegotiationRepository negotiationRepository;
    private final LeadReminderEmailService leadReminderEmailService;

    // ========== TEST ENDPOINTS ==========
    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "Server is running on port 8090");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Backend is working");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    // ========== LEAD ENDPOINTS (existing) ==========
    @GetMapping
    public ResponseEntity<ApiResponse<List<Lead>>> getAllLeads(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity
                .ok(ApiResponse.success("Leads fetched", leadService.getAllLeads(user.getUserid(), user.getRole())));
    }

    @GetMapping("/{id}/lead-outcome-status")
    public ResponseEntity<ApiResponse<Lead>> getLeadWithOutcomeStatus(@PathVariable Long id) {
        Lead lead = leadService.getLeadById(id);
        return ResponseEntity.ok(ApiResponse.success("Lead fetched", lead));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Lead>> getLeadById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Lead fetched", leadService.getLeadById(id)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<Lead>>> getLeadsByStatus(@PathVariable String status, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Leads fetched",
                leadService.getLeadsByStatus(status, user.getUserid(), user.getRole())));
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<Lead>> createLead(
            @Valid @RequestPart("lead") LeadRequest request,
            @RequestPart(value = "uploadDocument", required = false) MultipartFile doc,
            @RequestPart(value = "uploadDocument1", required = false) MultipartFile doc1,
            @RequestPart(value = "uploadDocument2", required = false) MultipartFile doc2,
            @RequestPart(value = "uploadDocument3", required = false) MultipartFile doc3,
            Authentication auth) throws IOException {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Lead created",
                leadService.createLead(request, user.getUserid(), doc, doc1, doc2, doc3)));
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<Lead>> updateLead(
            @PathVariable Long id,
            @Valid @RequestPart("lead") LeadRequest request,
            @RequestPart(value = "uploadDocument", required = false) MultipartFile doc,
            @RequestPart(value = "uploadDocument1", required = false) MultipartFile doc1,
            @RequestPart(value = "uploadDocument2", required = false) MultipartFile doc2,
            @RequestPart(value = "uploadDocument3", required = false) MultipartFile doc3,
            Authentication auth) throws IOException {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Lead updated",
                leadService.updateLead(id, request, user.getUserid(), doc, doc1, doc2, doc3)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLead(@PathVariable Long id) {
        leadService.deleteLead(id);
        return ResponseEntity.ok(ApiResponse.success("Lead deleted", null));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Lead>> updateStatus(@PathVariable Long id,
            @RequestBody Map<String, String> body, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity
                .ok(ApiResponse.success("Status updated", leadService.updateLeadStatus(id, body.get("status"), user)));
    }

    @PatchMapping("/{id}/group")
    public ResponseEntity<ApiResponse<Lead>> updateGroup(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity
                .ok(ApiResponse.success("Group updated", leadService.updateLeadGroup(id, body.get("group"), user)));
    }

    @PatchMapping("/{id}/lead-outcome-status")
    public ResponseEntity<ApiResponse<Lead>> updateLeadOutcomeStatus(@PathVariable Long id,
            @RequestBody Map<String, String> body, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Lead status updated",
                leadService.updateLeadOutcomeStatus(id, body.get("leadOutcomeStatus"), user)));
    }

    @PatchMapping("/{id}/enquiry-status")
    public ResponseEntity<ApiResponse<Lead>> updateEnquiryStatus(@PathVariable Long id,
            @RequestBody Map<String, String> body, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Enquiry status updated",
                leadService.updateLeadEnquiryStatus(id, body.get("enquiryStatus"), user)));
    }


    @GetMapping("/{id}/notes")
    public ResponseEntity<ApiResponse<List<LeadNote>>> getNotes(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Notes fetched", leadService.getNotes(id)));
    }

    @GetMapping("/notes/all")
    public ResponseEntity<ApiResponse<List<LeadNote>>> getAllNotes() {
        return ResponseEntity.ok(ApiResponse.success("All notes fetched", leadService.getAllNotes()));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<ApiResponse<LeadNote>> addNote(@PathVariable Long id, @RequestBody Map<String, String> body,
            Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity
                .ok(ApiResponse.success("Note added", leadService.addNote(id, body.get("noteText"), user.getUserid())));
    }

    @GetMapping("/{id}/reminders")
    public ResponseEntity<ApiResponse<List<LeadReminder>>> getReminders(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Reminders fetched", leadService.getReminders(id)));
    }

    @PostMapping("/{id}/reminders")
    public ResponseEntity<ApiResponse<LeadReminder>> addReminder(@PathVariable Long id,
            @RequestBody Map<String, String> body, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Reminder added",
                leadService.addReminder(id, body.get("reminderText"), body.get("reminderDate"), user.getUserid())));
    }

    @PostMapping("/reminders/{reminderId}/send-email")
    public ResponseEntity<ApiResponse<Void>> sendReminderEmail(@PathVariable Long reminderId) {
        try {
            leadReminderEmailService.sendReminderEmailManual(reminderId);
            return ResponseEntity.ok(ApiResponse.success("Reminder email sent", null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to send email: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/convert")
    public ResponseEntity<ApiResponse<Opportunity>> convertToOpportunity(@PathVariable Long id, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Lead converted to opportunity",
                leadService.convertToOpportunity(id, user.getUserid())));
    }

    @PostMapping({"/import", "/import/indiamart"})
    public ResponseEntity<ApiResponse<List<Lead>>> importFromIndiamart(@Valid @RequestBody ImportLeadRequest request,
            Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Leads imported from Indiamart",
                leadService.importFromIndiamart(request, user.getUserid())));
    }

    // ========== LEAD RATING ENDPOINTS ==========
    
    /**
     * GET - Get lead rating
     * GET /api/leads/{id}/leadRating
     */
    @GetMapping("/{id}/leadRating")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLeadRating(@PathVariable Long id) {
        try {
            Lead lead = leadService.getLeadById(id);
            if (lead == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Lead not found with id: " + id));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("leadId", id);
            response.put("leadRating", lead.getLeadRating() != null ? lead.getLeadRating() : 0);
            response.put("lead", lead);

            return ResponseEntity.ok(ApiResponse.success("Lead rating fetched", response));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get lead rating: " + e.getMessage()));
        }
    }

    /**
     * PUT - Update lead rating
     * PUT /api/leads/{id}/leadRating
     */
    @PutMapping("/{id}/leadRating")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateLeadRating(
            @PathVariable Long id,
            @RequestBody Map<String, Object> requestBody) {
        try {
            // Log the request
            System.out.println("Updating rating for lead: " + id);
            System.out.println("Request body: " + requestBody);

            // Extract rating from request body
            Object ratingObj = requestBody.get("leadRating");
            if (ratingObj == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("leadRating is required"));
            }

            Integer rating;
            try {
                rating = Integer.valueOf(ratingObj.toString());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("leadRating must be a valid number"));
            }

            // Validate rating range (0-5)
            if (rating < 0 || rating > 5) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("leadRating must be between 0 and 5"));
            }

            // Update the rating
            Lead updatedLead = leadService.updateLeadRating(id, rating);
            
            if (updatedLead == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Lead not found with id: " + id));
            }

            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("leadId", id);
            response.put("leadRating", rating);
            response.put("lead", updatedLead);

            return ResponseEntity.ok(ApiResponse.success("Rating updated successfully", response));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update rating: " + e.getMessage()));
        }
    }

    /**
     * PATCH - Partial update lead rating
     * PATCH /api/leads/{id}/leadRating
     */
    @PatchMapping("/{id}/leadRating")
    public ResponseEntity<ApiResponse<Map<String, Object>>> patchLeadRating(
            @PathVariable Long id,
            @RequestBody Map<String, Object> requestBody) {
        // Same implementation as PUT
        return updateLeadRating(id, requestBody);
    }

    // ========== LEAD SOURCE ENDPOINTS ==========
    @GetMapping("/lead-source")
    public ResponseEntity<ApiResponse<List<LeadSourceMaster>>> getAllLeadSources(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        List<LeadSourceMaster> sources;
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        if (companyAdminId != null) {
            sources = leadSourceRepository.findActiveByUserIdFkOrGlobal(companyAdminId);
        } else {
            sources = leadSourceRepository.findByActiveTrue();
        }
        sources = sources.stream()
                .filter(s -> s != null && s.getSourceName() != null && !s.getSourceName().trim().isEmpty() && !"null".equalsIgnoreCase(s.getSourceName().trim()))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Lead sources fetched", sources));
    }

    @GetMapping("/lead-source/{id}")
    public ResponseEntity<ApiResponse<LeadSourceMaster>> getLeadSourceById(@PathVariable Long id) {
        LeadSourceMaster source = leadSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead source not found"));
        return ResponseEntity.ok(ApiResponse.success("Lead source fetched", source));
    }

    @PostMapping("/lead-source")
    public ResponseEntity<ApiResponse<LeadSourceMaster>> createLeadSource(@Valid @RequestBody LeadSourceMaster source, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        source.setId(null);
        source.setActive(true);
        if (!authUtil.isSuperAdmin(user.getRole())) {
            Long companyAdminId = authUtil.getCompanyAdminId(user);
            source.setUserIdFk(companyAdminId);
        }
        LeadSourceMaster saved = leadSourceRepository.save(source);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lead source created", saved));
    }

    @PutMapping("/lead-source/{id}")
    public ResponseEntity<ApiResponse<LeadSourceMaster>> updateLeadSource(@PathVariable Long id,
            @Valid @RequestBody LeadSourceMaster source) {
        if (!leadSourceRepository.existsById(id)) {
            throw new RuntimeException("Lead source not found with id: " + id);
        }
        source.setId(id);
        LeadSourceMaster updated = leadSourceRepository.save(source);
        return ResponseEntity.ok(ApiResponse.success("Lead source updated", updated));
    }

    @DeleteMapping("/lead-source/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLeadSource(@PathVariable Long id) {
        LeadSourceMaster source = leadSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead source not found"));
        source.setActive(false);
        leadSourceRepository.save(source);
        return ResponseEntity.ok(ApiResponse.success("Lead source deleted", null));
    }

    // ========== LEAD GROUP ENDPOINTS ==========
    @GetMapping("/lead-group")
    public ResponseEntity<ApiResponse<List<LeadGroupsMaster>>> getAllLeadGroups(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        List<LeadGroupsMaster> groups;
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        if (companyAdminId != null) {
            groups = leadGroupRepository.findActiveByUserIdFkOrGlobal(companyAdminId);
        } else {
            groups = leadGroupRepository.findByActiveTrue();
        }
        groups = groups.stream()
                .filter(g -> g != null && g.getGroupName() != null && !g.getGroupName().trim().isEmpty() && !"null".equalsIgnoreCase(g.getGroupName().trim()))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Lead groups fetched", groups));
    }

    @GetMapping("/lead-group/{id}")
    public ResponseEntity<ApiResponse<LeadGroupsMaster>> getLeadGroupById(@PathVariable Long id) {
        LeadGroupsMaster group = leadGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead group not found"));
        return ResponseEntity.ok(ApiResponse.success("Lead group fetched", group));
    }

    @PostMapping("/lead-group")
    public ResponseEntity<ApiResponse<LeadGroupsMaster>> createLeadGroup(@Valid @RequestBody LeadGroupsMaster group, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        group.setId(null);
        group.setActive(true);
        if (!authUtil.isSuperAdmin(user.getRole())) {
            Long companyAdminId = authUtil.getCompanyAdminId(user);
            group.setUserIdFk(companyAdminId);
        }
        LeadGroupsMaster saved = leadGroupRepository.save(group);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lead group created", saved));
    }

    @PutMapping("/lead-group/{id}")
    public ResponseEntity<ApiResponse<LeadGroupsMaster>> updateLeadGroup(@PathVariable Long id,
            @Valid @RequestBody LeadGroupsMaster group) {
        if (!leadGroupRepository.existsById(id)) {
            throw new RuntimeException("Lead group not found with id: " + id);
        }
        group.setId(id);
        LeadGroupsMaster updated = leadGroupRepository.save(group);
        return ResponseEntity.ok(ApiResponse.success("Lead group updated", updated));
    }

    @DeleteMapping("/lead-group/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLeadGroup(@PathVariable Long id) {
        LeadGroupsMaster group = leadGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead group not found"));
        group.setActive(false);
        leadGroupRepository.save(group);
        return ResponseEntity.ok(ApiResponse.success("Lead group deleted", null));
    }

    // ========== LEAD STATUS ENDPOINTS ==========
    @GetMapping("/lead-status")
    public ResponseEntity<ApiResponse<List<LeadStatusMaster>>> getAllLeadStatuses(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        List<LeadStatusMaster> statuses;
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        if (companyAdminId != null) {
            statuses = leadStatusRepository.findActiveByUserIdFkOrGlobal(companyAdminId);
        } else {
            statuses = leadStatusRepository.findByActiveTrue();
        }
        statuses = statuses.stream()
                .filter(s -> s != null && s.getStatusName() != null && !s.getStatusName().trim().isEmpty() && !"null".equalsIgnoreCase(s.getStatusName().trim()))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Lead statuses fetched", statuses));
    }

    @GetMapping("/lead-status/{id}")
    public ResponseEntity<ApiResponse<LeadStatusMaster>> getLeadStatusById(@PathVariable Long id) {
        LeadStatusMaster status = leadStatusRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead status not found"));
        return ResponseEntity.ok(ApiResponse.success("Lead status fetched", status));
    }

    @PostMapping("/lead-status")
    public ResponseEntity<ApiResponse<LeadStatusMaster>> createLeadStatus(@Valid @RequestBody LeadStatusMaster status, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        status.setId(null);
        status.setActive(true);
        if (!authUtil.isSuperAdmin(user.getRole())) {
            Long companyAdminId = authUtil.getCompanyAdminId(user);
            status.setUserIdFk(companyAdminId);
        }
        LeadStatusMaster saved = leadStatusRepository.save(status);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lead status created", saved));
    }

    @PutMapping("/lead-status/{id}")
    public ResponseEntity<ApiResponse<LeadStatusMaster>> updateLeadStatus(@PathVariable Long id,
            @Valid @RequestBody LeadStatusMaster status) {
        if (!leadStatusRepository.existsById(id)) {
            throw new RuntimeException("Lead status not found with id: " + id);
        }
        status.setId(id);
        LeadStatusMaster updated = leadStatusRepository.save(status);
        return ResponseEntity.ok(ApiResponse.success("Lead status updated", updated));
    }

    @DeleteMapping("/lead-status/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLeadStatus(@PathVariable Long id) {
        LeadStatusMaster status = leadStatusRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead status not found"));
        status.setActive(false);
        leadStatusRepository.save(status);
        return ResponseEntity.ok(ApiResponse.success("Lead status deleted", null));
    }

    

    @PostMapping("/{leadId}/convert-to-negotiation")
    public ResponseEntity<ApiResponse<Negotiation>> convertToNegotiation(@PathVariable Long leadId) {

        // Find Lead
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));

        // Check if already converted
        List<Negotiation> existing = negotiationRepository.findByLeadIdFk(leadId);

        if (!existing.isEmpty()) {
            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Lead already converted",
                            existing.get(0)
                    )
            );
        }

        // Create Negotiation
        Negotiation negotiation = new Negotiation();

        negotiation.setLeadIdFk(lead.getLeadId());
        negotiation.setNegotiationName(lead.getLeadOrganisationName());
        negotiation.setNegotiationTitle(lead.getLeadTitle());

        // Use existing quotation number if available, otherwise generate one
        if (lead.getQuotationNumber() != null && !lead.getQuotationNumber().isBlank()) {
            negotiation.setQuotationNo(lead.getQuotationNumber());
        } else {
            negotiation.setQuotationNo(generateQuotationNo());
        }

        negotiation.setQuotationRevision(
                lead.getQuotationRevision() != null ? lead.getQuotationRevision() : "R0"
        );

        negotiation.setQuotationAmount(
                lead.getQuotationAmount() != null ? lead.getQuotationAmount() : BigDecimal.ZERO
        );

        negotiation.setNegotiationStatus("Open");

        negotiation.setRemarks(lead.getEnquiryDescription());
        negotiation.setUserIdFk(lead.getUserIdFk());

        negotiation = negotiationRepository.save(negotiation);

        // Update Lead Status
        lead.setLeadOutcomeStatus("Negotiation");
        leadRepository.save(lead);

        return ResponseEntity.ok(
                ApiResponse.success("Lead converted to negotiation", negotiation)
        );
    }

    private String generateQuotationNo() {
        return "QTN-" + System.currentTimeMillis();
    }
}