package com.crm.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
            
            // Get documents using quotationNo instead of revision ID
            String quotationNo = rev.getQuotationNo();
            List<Document> documents = documentRepository.findByQuotationNo(quotationNo);
            
            // Add document details
            if (documents != null && !documents.isEmpty()) {
                List<Map<String, Object>> docList = new ArrayList<>();
                for (Document doc : documents) {
                    Map<String, Object> docMap = new HashMap<>();
                    docMap.put("id", doc.getId());
                    docMap.put("fileName", doc.getFileName());
                    docMap.put("fileSize", doc.getFileSize());
                    docMap.put("fileType", doc.getFileType());
                    docMap.put("uploadedDate", doc.getUploadedDate());
                    docMap.put("fileUrl", doc.getFileUrl()); // Add file URL for viewing
                    docList.add(docMap);
                }
                map.put("documents", docList);
                map.put("documentCount", documents.size());
            } else {
                map.put("documents", new ArrayList<>());
                map.put("documentCount", 0);
            }

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
