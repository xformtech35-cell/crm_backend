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
import com.crm.entity.QuotationStatusMaster;
import com.crm.repository.QuotationStatusRepository;
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
    private final QuotationStatusRepository quotationStatusRepository;


    private final LeadRepository leadRepository;
    private final NegotiationRepository negotiationRepository;
    private final com.crm.repository.UserRepository userRepository;
    private final LeadReminderEmailService leadReminderEmailService;

    // ========== TEST ENDPOINTS ==========
    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "Server is running on port 8090");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/seed-dummy-marketplace")
    public ResponseEntity<Map<String, Object>> seedDummyMarketplaceLeads() {
        Map<String, Object> res = new HashMap<>();
        try {
            long imCount = leadRepository.findAll().stream().filter(l -> l.getLeadSource() != null && l.getLeadSource().toLowerCase().contains("indiamart")).count();
            if (imCount == 0) {
                Lead l1 = new Lead();
                l1.setLeadOrganisationName("Apex Aqua Technologies Pvt Ltd");
                l1.setCompanyContactPersonName("Rajesh Sharma");
                l1.setLeadMobileNo("9822011223");
                l1.setLeadEmail("info@apexaqua.com");
                l1.setLeadCity("Pune");
                l1.setLeadState("Maharashtra");
                l1.setLeadCountry("India");
                l1.setLeadSource("IndiaMART");
                l1.setEnquiryDescription("100 KLD Industrial RO Plant & WTP System for manufacturing plant");
                l1.setLeadStatus("New Lead");
                l1.setLeadOutcomeStatus("New Lead");
                l1.setInquiryDate(java.time.LocalDate.now().minusDays(1));
                l1.setLeadCreatedDate(LocalDateTime.now().minusDays(1));
                l1.setSendToMainLeads(false);
                leadRepository.save(l1);

                Lead l2 = new Lead();
                l2.setLeadOrganisationName("Sterling Chemicals & Organics");
                l2.setCompanyContactPersonName("Amit Patel");
                l2.setLeadMobileNo("9765432109");
                l2.setLeadEmail("purchase@sterlingchem.com");
                l2.setLeadCity("Ahmedabad");
                l2.setLeadState("Gujarat");
                l2.setLeadCountry("India");
                l2.setLeadSource("IndiaMART");
                l2.setEnquiryDescription("50 KLD Effluent Treatment Plant (ETP) with Zero Liquid Discharge (ZLD)");
                l2.setLeadStatus("New Lead");
                l2.setLeadOutcomeStatus("New Lead");
                l2.setInquiryDate(java.time.LocalDate.now().minusDays(2));
                l2.setLeadCreatedDate(LocalDateTime.now().minusDays(2));
                l2.setSendToMainLeads(false);
                leadRepository.save(l2);

                Lead l3 = new Lead();
                l3.setLeadOrganisationName("Bhartiya Bio-Enviro Systems");
                l3.setCompanyContactPersonName("Sanjay Kulkarni");
                l3.setLeadMobileNo("9890123456");
                l3.setLeadEmail("sales@bhartiyabio.com");
                l3.setLeadCity("Surat");
                l3.setLeadState("Gujarat");
                l3.setLeadCountry("India");
                l3.setLeadSource("IndiaMART");
                l3.setEnquiryDescription("25 M3/HR Demineralization Plant (DM) for boiler feed water application");
                l3.setLeadStatus("New Lead");
                l3.setLeadOutcomeStatus("New Lead");
                l3.setInquiryDate(java.time.LocalDate.now());
                l3.setLeadCreatedDate(LocalDateTime.now());
                l3.setSendToMainLeads(false);
                leadRepository.save(l3);

                Lead t1 = new Lead();
                t1.setLeadOrganisationName("Kirloskar Enviro Projects");
                t1.setCompanyContactPersonName("Vikram Deshmukh");
                t1.setLeadMobileNo("9844556677");
                t1.setLeadEmail("enquiry@kirloskar-enviro.com");
                t1.setLeadCity("Mumbai");
                t1.setLeadState("Maharashtra");
                t1.setLeadCountry("India");
                t1.setLeadSource("TradeIndia");
                t1.setEnquiryDescription("200 KLD Sewage Treatment Plant (STP) for commercial complex");
                t1.setLeadStatus("New Lead");
                t1.setLeadOutcomeStatus("New Lead");
                t1.setInquiryDate(java.time.LocalDate.now().minusDays(1));
                t1.setLeadCreatedDate(LocalDateTime.now().minusDays(1));
                t1.setSendToMainLeads(false);
                leadRepository.save(t1);

                Lead t2 = new Lead();
                t2.setLeadOrganisationName("SunTech Water Solutions");
                t2.setCompanyContactPersonName("Pravin Mehta");
                t2.setLeadMobileNo("9922334455");
                t2.setLeadEmail("projects@suntechwater.in");
                t2.setLeadCity("Vadodara");
                t2.setLeadState("Gujarat");
                t2.setLeadCountry("India");
                t2.setLeadSource("TradeIndia");
                t2.setEnquiryDescription("UF & RO Skid System 10 M3/HR capacity");
                t2.setLeadStatus("New Lead");
                t2.setLeadOutcomeStatus("New Lead");
                t2.setInquiryDate(java.time.LocalDate.now().minusDays(3));
                t2.setLeadCreatedDate(LocalDateTime.now().minusDays(3));
                t2.setSendToMainLeads(false);
                leadRepository.save(t2);

                Lead t3 = new Lead();
                t3.setLeadOrganisationName("Maharastra Industrial Processors");
                t3.setCompanyContactPersonName("Dinesh More");
                t3.setLeadMobileNo("9876543210");
                t3.setLeadEmail("contact@maharashtraprocess.com");
                t3.setLeadCity("Nashik");
                t3.setLeadState("Maharashtra");
                t3.setLeadCountry("India");
                t3.setLeadSource("TradeIndia");
                t3.setEnquiryDescription("Ultra Pure Water System for Pharmaceutical Formulation Unit");
                t3.setLeadStatus("New Lead");
                t3.setLeadOutcomeStatus("New Lead");
                t3.setInquiryDate(java.time.LocalDate.now());
                t3.setLeadCreatedDate(LocalDateTime.now());
                t3.setSendToMainLeads(false);
                leadRepository.save(t3);

                res.put("message", "Seeded 3 IndiaMART leads and 3 TradeIndia leads successfully");
            } else {
                res.put("message", "Marketplace leads already exist");
            }

            // Assign to admin@uwsenviro.com
            Long targetUserId = userRepository.findByUserEmail("admin@uwsenviro.com")
                    .map(User::getUserid)
                    .orElse(1L);

            List<Lead> leads = leadRepository.findAll().stream()
                    .filter(l -> l.getLeadSource() != null && 
                            (l.getLeadSource().toLowerCase().contains("indiamart") || 
                             l.getLeadSource().toLowerCase().contains("tradeindia")))
                    .collect(java.util.stream.Collectors.toList());

            for (Lead l : leads) {
                l.setUserIdFk(targetUserId);
                l.setLeadAssignedMember(targetUserId);
                l.setCreatedBy("admin@uwsenviro.com");
                l.setUpdatedBy("admin@uwsenviro.com");
                leadRepository.save(l);
            }

            res.put("success", true);
            res.put("assignedToUser", "admin@uwsenviro.com");
            res.put("assignedUserId", targetUserId);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("error", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    @GetMapping("/bulk/assign-dummy-marketplace")
    public ResponseEntity<Map<String, Object>> assignDummyMarketplaceLeads() {
        Map<String, Object> res = new HashMap<>();
        try {
            Long targetUserId = userRepository.findByUserEmail("admin@uwsenviro.com")
                    .map(User::getUserid)
                    .orElse(1L);

            List<Lead> leads = leadRepository.findAll().stream()
                    .filter(l -> l.getLeadSource() != null && 
                            (l.getLeadSource().toLowerCase().contains("indiamart") || 
                             l.getLeadSource().toLowerCase().contains("tradeindia")))
                    .collect(java.util.stream.Collectors.toList());

            for (Lead l : leads) {
                l.setUserIdFk(targetUserId);
                l.setLeadAssignedMember(targetUserId);
                l.setCreatedBy("admin@uwsenviro.com");
                l.setUpdatedBy("admin@uwsenviro.com");
                leadRepository.save(l);
            }

            res.put("success", true);
            res.put("assignedCount", leads.size());
            res.put("assignedToUserId", targetUserId);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("error", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
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
    public ResponseEntity<ApiResponse<Lead>> getLeadById(@PathVariable Long id, Authentication auth) {
        User user = auth != null ? authUtil.getCurrentUser(auth) : null;
        return ResponseEntity.ok(ApiResponse.success("Lead fetched", leadService.getLeadById(id, user)));
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

    @PostMapping("/sync-all-entities")
    public ResponseEntity<ApiResponse<Void>> syncAllEntities() {
        leadService.syncAllExistingLeadsToEntities();
        return ResponseEntity.ok(ApiResponse.success("Synced all leads to Contacts, Organizations, and Opportunities", null));
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

    @PatchMapping("/{id}/send-to-main-leads")
    public ResponseEntity<ApiResponse<Lead>> updateSendToMainLeads(@PathVariable Long id,
            @RequestBody Map<String, Object> body, Authentication auth) {
        User user = null;
        try {
            if (auth != null) {
                user = authUtil.getCurrentUser(auth);
            }
        } catch (Exception ignored) {}
        Object rawVal = body != null ? body.get("sendToMainLeads") : null;
        Boolean sendVal = Boolean.TRUE.equals(rawVal) || "true".equalsIgnoreCase(String.valueOf(rawVal));
        return ResponseEntity.ok(ApiResponse.success("Send to main leads updated",
                leadService.updateSendToMainLeads(id, sendVal, user)));
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

    @DeleteMapping("/reminders/{reminderId}")
    public ResponseEntity<ApiResponse<Void>> deleteReminder(@PathVariable Long reminderId) {
        leadService.deleteReminder(reminderId);
        return ResponseEntity.ok(ApiResponse.success("Reminder deleted", null));
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
        String name = source.getSourceName() != null ? source.getSourceName().trim() : "";
        if (name.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lead source name cannot be empty"));
        }

        Long companyAdminId = authUtil.getCompanyAdminId(user);
        if (companyAdminId == null) {
            companyAdminId = user.getUserid();
        }

        List<LeadSourceMaster> existing = leadSourceRepository.findActiveByUserIdFkOrGlobal(companyAdminId);
        boolean duplicate = existing.stream()
                .anyMatch(s -> s.getSourceName() != null && s.getSourceName().trim().equalsIgnoreCase(name));
        if (duplicate) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lead source '" + name + "' already exists!"));
        }

        source.setId(null);
        source.setSourceName(name);
        source.setActive(true);
        source.setUserIdFk(companyAdminId);
        LeadSourceMaster saved = leadSourceRepository.save(source);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lead source created", saved));
    }

    @PutMapping("/lead-source/{id}")
    public ResponseEntity<ApiResponse<LeadSourceMaster>> updateLeadSource(@PathVariable Long id,
            @Valid @RequestBody LeadSourceMaster source, Authentication auth) {
        LeadSourceMaster existingSource = leadSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead source not found with id: " + id));

        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        if (companyAdminId == null) {
            companyAdminId = user.getUserid();
        }

        String name = source.getSourceName() != null ? source.getSourceName().trim() : "";
        if (name.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lead source name cannot be empty"));
        }

        List<LeadSourceMaster> existing = leadSourceRepository.findActiveByUserIdFkOrGlobal(companyAdminId);
        boolean duplicate = existing.stream()
                .anyMatch(s -> s.getSourceName() != null && s.getSourceName().trim().equalsIgnoreCase(name) && !s.getId().equals(id));
        if (duplicate) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lead source '" + name + "' already exists!"));
        }

        existingSource.setSourceName(name);
        existingSource.setDescription(source.getDescription());
        if (companyAdminId != null && existingSource.getUserIdFk() == null) {
            existingSource.setUserIdFk(companyAdminId);
        }
        LeadSourceMaster updated = leadSourceRepository.save(existingSource);
        return ResponseEntity.ok(ApiResponse.success("Lead source updated", updated));
    }

    @DeleteMapping("/lead-source/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLeadSource(@PathVariable Long id, Authentication auth) {
        LeadSourceMaster source = leadSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead source not found"));
        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        if (companyAdminId == null) {
            companyAdminId = user.getUserid();
        }

        if (source.getUserIdFk() == null) {
            LeadSourceMaster companyOverride = new LeadSourceMaster();
            companyOverride.setSourceName(source.getSourceName());
            companyOverride.setDescription(source.getDescription());
            companyOverride.setActive(false);
            companyOverride.setUserIdFk(companyAdminId);
            leadSourceRepository.save(companyOverride);
        } else {
            source.setActive(false);
            leadSourceRepository.save(source);
        }
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
        String name = group.getGroupName() != null ? group.getGroupName().trim() : "";
        if (name.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lead group name cannot be empty"));
        }

        Long companyAdminId = authUtil.getCompanyAdminId(user);
        if (companyAdminId == null) {
            companyAdminId = user.getUserid();
        }

        List<LeadGroupsMaster> existing = leadGroupRepository.findActiveByUserIdFkOrGlobal(companyAdminId);
        boolean duplicate = existing.stream()
                .anyMatch(g -> g.getGroupName() != null && g.getGroupName().trim().equalsIgnoreCase(name));
        if (duplicate) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lead group '" + name + "' already exists!"));
        }

        group.setId(null);
        group.setGroupName(name);
        group.setActive(true);
        group.setUserIdFk(companyAdminId);
        LeadGroupsMaster saved = leadGroupRepository.save(group);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lead group created", saved));
    }

    @PutMapping("/lead-group/{id}")
    public ResponseEntity<ApiResponse<LeadGroupsMaster>> updateLeadGroup(@PathVariable Long id,
            @Valid @RequestBody LeadGroupsMaster group, Authentication auth) {
        LeadGroupsMaster existingGroup = leadGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead group not found with id: " + id));

        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        if (companyAdminId == null) {
            companyAdminId = user.getUserid();
        }

        String name = group.getGroupName() != null ? group.getGroupName().trim() : "";
        if (name.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lead group name cannot be empty"));
        }

        List<LeadGroupsMaster> existing = leadGroupRepository.findActiveByUserIdFkOrGlobal(companyAdminId);
        boolean duplicate = existing.stream()
                .anyMatch(g -> g.getGroupName() != null && g.getGroupName().trim().equalsIgnoreCase(name) && !g.getId().equals(id));
        if (duplicate) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lead group '" + name + "' already exists!"));
        }

        existingGroup.setGroupName(name);
        existingGroup.setDescription(group.getDescription());
        if (companyAdminId != null && existingGroup.getUserIdFk() == null) {
            existingGroup.setUserIdFk(companyAdminId);
        }
        LeadGroupsMaster updated = leadGroupRepository.save(existingGroup);
        return ResponseEntity.ok(ApiResponse.success("Lead group updated", updated));
    }

    @DeleteMapping("/lead-group/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLeadGroup(@PathVariable Long id, Authentication auth) {
        LeadGroupsMaster group = leadGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead group not found"));
        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        if (companyAdminId == null) {
            companyAdminId = user.getUserid();
        }

        if (group.getUserIdFk() == null) {
            LeadGroupsMaster companyOverride = new LeadGroupsMaster();
            companyOverride.setGroupName(group.getGroupName());
            companyOverride.setActive(false);
            companyOverride.setUserIdFk(companyAdminId);
            leadGroupRepository.save(companyOverride);
        } else {
            group.setActive(false);
            leadGroupRepository.save(group);
        }
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
        String name = status.getStatusName() != null ? status.getStatusName().trim() : "";
        if (name.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lead status name cannot be empty"));
        }

        Long companyAdminId = authUtil.getCompanyAdminId(user);
        if (companyAdminId == null) {
            companyAdminId = user.getUserid();
        }

        List<LeadStatusMaster> existing = leadStatusRepository.findActiveByUserIdFkOrGlobal(companyAdminId);
        boolean duplicate = existing.stream()
                .anyMatch(s -> s.getStatusName() != null && s.getStatusName().trim().equalsIgnoreCase(name));
        if (duplicate) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lead status '" + name + "' already exists!"));
        }

        status.setId(null);
        status.setStatusName(name);
        status.setActive(true);
        status.setUserIdFk(companyAdminId);
        LeadStatusMaster saved = leadStatusRepository.save(status);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lead status created", saved));
    }

    @PutMapping("/lead-status/{id}")
    public ResponseEntity<ApiResponse<LeadStatusMaster>> updateLeadStatus(@PathVariable Long id,
            @Valid @RequestBody LeadStatusMaster status, Authentication auth) {
        LeadStatusMaster existingStatus = leadStatusRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead status not found with id: " + id));

        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        if (companyAdminId == null) {
            companyAdminId = user.getUserid();
        }

        String name = status.getStatusName() != null ? status.getStatusName().trim() : "";
        if (name.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lead status name cannot be empty"));
        }

        List<LeadStatusMaster> existing = leadStatusRepository.findActiveByUserIdFkOrGlobal(companyAdminId);
        boolean duplicate = existing.stream()
                .anyMatch(s -> s.getStatusName() != null && s.getStatusName().trim().equalsIgnoreCase(name) && !s.getId().equals(id));
        if (duplicate) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lead status '" + name + "' already exists!"));
        }

        existingStatus.setStatusName(name);
        existingStatus.setDescription(status.getDescription());
        if (companyAdminId != null && existingStatus.getUserIdFk() == null) {
            existingStatus.setUserIdFk(companyAdminId);
        }
        LeadStatusMaster updated = leadStatusRepository.save(existingStatus);
        return ResponseEntity.ok(ApiResponse.success("Lead status updated", updated));
    }

    @DeleteMapping("/lead-status/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLeadStatus(@PathVariable Long id, Authentication auth) {
        LeadStatusMaster status = leadStatusRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead status not found"));
        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        if (companyAdminId == null) {
            companyAdminId = user.getUserid();
        }

        if (status.getUserIdFk() == null) {
            LeadStatusMaster companyOverride = new LeadStatusMaster();
            companyOverride.setStatusName(status.getStatusName());
            companyOverride.setDescription(status.getDescription());
            companyOverride.setActive(false);
            companyOverride.setUserIdFk(companyAdminId);
            leadStatusRepository.save(companyOverride);
        } else {
            status.setActive(false);
            leadStatusRepository.save(status);
        }
        return ResponseEntity.ok(ApiResponse.success("Lead status deleted", null));
    }

    // ========== QUOTATION STATUS ENDPOINTS ==========
    @GetMapping("/quotation-status")
    public ResponseEntity<ApiResponse<List<QuotationStatusMaster>>> getAllQuotationStatuses(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        List<QuotationStatusMaster> statuses;
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        if (companyAdminId != null) {
            statuses = quotationStatusRepository.findActiveByUserIdFkOrGlobal(companyAdminId);
        } else {
            statuses = quotationStatusRepository.findByActiveTrue();
        }
        statuses = statuses.stream()
                .filter(s -> s != null && s.getStatusName() != null && !s.getStatusName().trim().isEmpty() && !"null".equalsIgnoreCase(s.getStatusName().trim()))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Quotation statuses fetched", statuses));
    }

    @GetMapping("/quotation-status/{id}")
    public ResponseEntity<ApiResponse<QuotationStatusMaster>> getQuotationStatusById(@PathVariable Long id) {
        QuotationStatusMaster status = quotationStatusRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quotation status not found with id: " + id));
        return ResponseEntity.ok(ApiResponse.success("Quotation status fetched", status));
    }

    @PostMapping("/quotation-status")
    public ResponseEntity<ApiResponse<QuotationStatusMaster>> createQuotationStatus(
            @Valid @RequestBody QuotationStatusMaster status, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        String name = status.getStatusName() != null ? status.getStatusName().trim() : "";
        if (name.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Quotation status name cannot be empty"));
        }

        Long companyAdminId = authUtil.getCompanyAdminId(user);
        if (companyAdminId == null) {
            companyAdminId = user.getUserid();
        }

        List<QuotationStatusMaster> existing = quotationStatusRepository.findActiveByUserIdFkOrGlobal(companyAdminId);
        boolean duplicate = existing.stream()
                .anyMatch(s -> s.getStatusName() != null && s.getStatusName().trim().equalsIgnoreCase(name));
        if (duplicate) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Quotation status '" + name + "' already exists!"));
        }

        status.setId(null);
        status.setStatusName(name);
        status.setActive(true);
        status.setUserIdFk(companyAdminId);
        QuotationStatusMaster saved = quotationStatusRepository.save(status);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Quotation status created", saved));
    }

    @PutMapping("/quotation-status/{id}")
    public ResponseEntity<ApiResponse<QuotationStatusMaster>> updateQuotationStatus(@PathVariable Long id,
            @Valid @RequestBody QuotationStatusMaster status, Authentication auth) {
        QuotationStatusMaster existingStatus = quotationStatusRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quotation status not found with id: " + id));

        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        if (companyAdminId == null) {
            companyAdminId = user.getUserid();
        }

        String name = status.getStatusName() != null ? status.getStatusName().trim() : "";
        if (name.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Quotation status name cannot be empty"));
        }

        List<QuotationStatusMaster> existing = quotationStatusRepository.findActiveByUserIdFkOrGlobal(companyAdminId);
        boolean duplicate = existing.stream()
                .anyMatch(s -> s.getStatusName() != null && s.getStatusName().trim().equalsIgnoreCase(name) && !s.getId().equals(id));
        if (duplicate) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Quotation status '" + name + "' already exists!"));
        }

        existingStatus.setStatusName(name);
        existingStatus.setDescription(status.getDescription());
        if (companyAdminId != null && existingStatus.getUserIdFk() == null) {
            existingStatus.setUserIdFk(companyAdminId);
        }
        QuotationStatusMaster updated = quotationStatusRepository.save(existingStatus);
        return ResponseEntity.ok(ApiResponse.success("Quotation status updated", updated));
    }

    @DeleteMapping("/quotation-status/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteQuotationStatus(@PathVariable Long id, Authentication auth) {
        QuotationStatusMaster status = quotationStatusRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quotation status not found"));
        User user = authUtil.getCurrentUser(auth);
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        if (companyAdminId == null) {
            companyAdminId = user.getUserid();
        }

        if (status.getUserIdFk() == null) {
            QuotationStatusMaster companyOverride = new QuotationStatusMaster();
            companyOverride.setStatusName(status.getStatusName());
            companyOverride.setDescription(status.getDescription());
            companyOverride.setActive(false);
            companyOverride.setUserIdFk(companyAdminId);
            quotationStatusRepository.save(companyOverride);
        } else {
            status.setActive(false);
            quotationStatusRepository.save(status);
        }
        return ResponseEntity.ok(ApiResponse.success("Quotation status deleted", null));
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

    @GetMapping("/max-quotation-serial")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMaxQuotationSerial(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        int maxSerial = leadService.getMaxQuotationSerial(user);
        Map<String, Object> resp = new HashMap<>();
        resp.put("maxSerial", maxSerial);
        resp.put("nextSerial", maxSerial + 1);
        return ResponseEntity.ok(ApiResponse.success("Max quotation serial fetched", resp));
    }

    @PatchMapping("/{id}/enquiryType")
    public ResponseEntity<ApiResponse<Lead>> updateLeadEnquiryType(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String enquiryType = body.get("enquiryType");
        Lead updated = leadService.updateLeadEnquiryType(id, enquiryType);
        return ResponseEntity.ok(ApiResponse.success("Enquiry type updated successfully", updated));
    }

    private String generateQuotationNo() {
        return "QTN-" + System.currentTimeMillis();
    }
}