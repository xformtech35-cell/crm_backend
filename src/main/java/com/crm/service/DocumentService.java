package com.crm.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.crm.dto.response.DocumentResponse;
import com.crm.entity.Document;
import com.crm.entity.Lead;
import com.crm.entity.Negotiation;
import com.crm.entity.NegotiationRevision;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.DocumentRepository;
import com.crm.repository.NegotiationRepository;
import com.crm.repository.NegotiationRevisionRepository;
import com.crm.util.FileUploadUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final NegotiationRepository negotiationepository;
    private final NegotiationRevisionRepository negotiationRevisionRepository;
    private final FileUploadUtil fileUploadUtil;
    private final com.crm.repository.LeadRepository leadRepository;

    // Method 1: Upload using Quotation Number (Handles duplicates)
    @Transactional
    public List<DocumentResponse> uploadDocuments(String quotationNo, List<MultipartFile> files) {
        return uploadDocuments(quotationNo, null, files);
    }

    @Transactional
    public List<DocumentResponse> uploadDocuments(String quotationNo, Long leadId, List<MultipartFile> files) {
        try {
            List<NegotiationRevision> revisions = negotiationRevisionRepository
                    .findByQuotationNo(quotationNo);
            
            NegotiationRevision negotiationRevision = null;
            if (!revisions.isEmpty()) {
                negotiationRevision = revisions.get(0);
                log.info("Using revision ID: {} for quotation: {}", negotiationRevision.getId(), quotationNo);
            }

            Lead lead = null;
            if (leadId != null) {
                lead = leadRepository.findById(leadId).orElse(null);
            }
            if (lead == null) {
                // Find matching lead for this quotation number (exact or base)
                List<Lead> leads = leadRepository.findByQuotationNumber(quotationNo);
                if (leads == null || leads.isEmpty()) {
                    String baseQuot = quotationNo.replaceAll("/R\\d+$", "");
                    leads = leadRepository.findByQuotationNumber(baseQuot);
                }
                if (leads != null && !leads.isEmpty()) {
                    lead = leads.get(0);
                }
            }

            if (lead != null) {
                if (lead.getQuotationNumber() == null || !lead.getQuotationNumber().equals(quotationNo)) {
                    lead.setQuotationNumber(quotationNo);
                }
                Negotiation negotiation = negotiationepository.findFirstByLeadIdFk(lead.getLeadId()).orElse(null);
                if (negotiation == null) {
                    negotiation = Negotiation.builder()
                            .leadIdFk(lead.getLeadId())
                            .negotiationName(lead.getLeadOrganisationName())
                            .negotiationTitle(lead.getLeadTitle())
                            .quotationNo(lead.getQuotationNumber() != null ? lead.getQuotationNumber() : quotationNo)
                            .quotationRevision(lead.getQuotationRevision() != null ? lead.getQuotationRevision() : "R0")
                            .quotationAmount(lead.getQuotationAmount())
                            .remarks(lead.getFollowUpRemark())
                            .negotiationStatus(lead.getLeadOutcomeStatus() != null ? lead.getLeadOutcomeStatus() : "Open")
                            .userIdFk(lead.getUserIdFk())
                            .build();
                    negotiation = negotiationepository.saveAndFlush(negotiation);
                }

                String revCode = (lead.getQuotationRevision() != null && !lead.getQuotationRevision().isBlank()) 
                        ? lead.getQuotationRevision() : "R0";
                if (quotationNo.matches(".*/R\\d+$")) {
                    revCode = quotationNo.substring(quotationNo.lastIndexOf('/') + 1).toUpperCase();
                }

                final String finalRev = revCode;
                if (negotiationRevision == null) {
                    negotiationRevision = negotiationRevisionRepository
                            .findFirstByNegotiationIdAndQuotationRevision(negotiation.getId(), finalRev)
                            .orElse(null);
                }

                if (negotiationRevision == null) {
                    NegotiationRevision newRev = new NegotiationRevision();
                    newRev.setNegotiationId(negotiation.getId());
                    newRev.setLeadIdFk(lead.getLeadId());
                    newRev.setQuotationNo(quotationNo);
                    newRev.setQuotationRevision(finalRev);
                    newRev.setQuotationAmount(lead.getQuotationAmount());
                    newRev.setRemarks(lead.getFollowUpRemark());
                    newRev.setEnquiryDescription(lead.getEnquiryDescription());
                    newRev.setQuotationDate(lead.getQuotationDate());
                    newRev.setNegotiationStatus(negotiation.getNegotiationStatus());
                    newRev.setUserIdFk(lead.getUserIdFk());
                    newRev.setUpdatedDate(LocalDateTime.now());
                    negotiationRevision = negotiationRevisionRepository.saveAndFlush(newRev);
                }
            }

            // Remove previous documents for this exact quotation number so only 1 active document exists per revision
            List<Document> existingDocs = documentRepository.findByQuotationNo(quotationNo);
            if (existingDocs != null && !existingDocs.isEmpty()) {
                existingDocs = existingDocs.stream()
                        .filter(d -> d != null && d.getQuotationNo() != null && d.getQuotationNo().equalsIgnoreCase(quotationNo))
                        .collect(Collectors.toList());
                documentRepository.deleteAll(existingDocs);
            }

            List<Document> savedDocuments = new ArrayList<>();
            String lastRelativeUrl = null;

            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    // Create sub-directory using quotation number
                    // Store with underscores for filesystem compatibility
                    String subDirectoryForStorage = "quotation/" + quotationNo.replace("/", "_");
                    
                    // Upload file and get URL
                    String fileUrl = fileUploadUtil.uploadFile(file, subDirectoryForStorage);
                    
                    // Extract relative path for /api/view/ endpoint
                    // Keep the original quotation number with slashes for the URL
                    String relativeUrl = extractRelativePath(fileUrl, quotationNo);
                    lastRelativeUrl = relativeUrl;
                    
                    log.info("File uploaded. Original URL: {}, Relative URL: {}", fileUrl, relativeUrl);
                    
                    Document document = Document.builder()
                            .quotationNo(quotationNo)
                            .fileName(file.getOriginalFilename())
                            .fileUrl(relativeUrl)
                            .fileSize(file.getSize())
                            .fileType(file.getContentType())
                            .uploadedDate(LocalDateTime.now())
                            .negotiationRevision(negotiationRevision)
                            .build();
                    
                    savedDocuments.add(document);
                }
            }

            savedDocuments = documentRepository.saveAll(savedDocuments);

            if (lastRelativeUrl != null && lead != null && "R0".equalsIgnoreCase(finalRev)) {
                lead.setUploadDocument(lastRelativeUrl);
                leadRepository.saveAndFlush(lead);
            }

            return savedDocuments.stream()
                    .map(this::mapToDocumentResponse)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("Error uploading documents: {}", e.getMessage());
            throw new RuntimeException("Failed to upload documents: " + e.getMessage());
        }
    }

    // Method 2: Upload using Revision ID (Recommended - uses unique ID)
    @Transactional
    public List<DocumentResponse> uploadDocumentsByRevisionId(Long revisionId, List<MultipartFile> files) {
        try {
            NegotiationRevision negotiationRevision = negotiationRevisionRepository
                    .findById(revisionId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Negotiation Revision not found with id: " + revisionId));

            String quotationNo = negotiationRevision.getQuotationNo();
            log.info("Uploading documents for revision ID: {}, Quotation: {}", revisionId, quotationNo);
            
            // Remove previous documents for this revision ID so only 1 active document exists
            List<Document> existingByRev = documentRepository.findByNegotiationRevisionId(revisionId);
            if (existingByRev != null && !existingByRev.isEmpty()) {
                documentRepository.deleteAll(existingByRev);
            }

            List<Document> savedDocuments = new ArrayList<>();

            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    // Create sub-directory using quotation number with underscores for filesystem
                    String subDirectoryForStorage = "quotation/" + quotationNo.replace("/", "_");
                    
                    // Upload file and get URL
                    String fileUrl = fileUploadUtil.uploadFile(file, subDirectoryForStorage);
                    
                    // Extract relative path for /api/view/ endpoint with original slashes
                    String relativeUrl = extractRelativePath(fileUrl, quotationNo);
                    
                    log.info("File uploaded. Original URL: {}, Relative URL: {}", fileUrl, relativeUrl);
                    
                    Document document = Document.builder()
                            .quotationNo(quotationNo)
                            .fileName(file.getOriginalFilename())
                            .fileUrl(relativeUrl)
                            .fileSize(file.getSize())
                            .fileType(file.getContentType())
                            .uploadedDate(LocalDateTime.now())
                            .negotiationRevision(negotiationRevision)
                            .build();
                    
                    savedDocuments.add(document);
                }
            }

            savedDocuments = documentRepository.saveAll(savedDocuments);

            return savedDocuments.stream()
                    .map(this::mapToDocumentResponse)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("Error uploading documents: {}", e.getMessage());
            throw new RuntimeException("Failed to upload documents: " + e.getMessage());
        }
    }

    public List<DocumentResponse> getDocumentsByQuotationNo(String quotationNo) {
        List<Document> documents = documentRepository.findByQuotationNo(quotationNo);
        return documents.stream()
                .map(this::mapToDocumentResponse)
                .collect(Collectors.toList());
    }

    public DocumentResponse getDocumentById(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));
        return mapToDocumentResponse(document);
    }


    /**
     * Extract relative path for /api/view/ endpoint
     * Handles both underscore and slash formats for quotation numbers
     */
 private String extractRelativePath(String fileUrl, String quotationNo) {
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
        int apiViewIndex = fileUrl.indexOf("/api/view/");
        if (apiViewIndex != -1) {
            relativePath = fileUrl.substring(apiViewIndex + 10);
        } else {
            int apiIndex = fileUrl.indexOf("/api/");
            if (apiIndex != -1) {
                relativePath = fileUrl.substring(apiIndex + 5);
            }
        }
    }
    
    // Remove leading slash
    if (relativePath.startsWith("/")) {
        relativePath = relativePath.substring(1);
    }
    
    // Remove "files/" if it appears again
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
    
    log.info("Extracted relative path: {} from: {}", relativePath, fileUrl);
    return relativePath;
}
    @Transactional
    public void deleteDocument(Long documentId) {
        Document doc = documentRepository.findById(documentId).orElse(null);
        if (doc != null) {
            doc.setNegotiationRevision(null);
            documentRepository.delete(doc);
            log.info("Document permanently deleted with id: {}", documentId);
        }
    }

    @Transactional
    public void deleteAllDocumentsByQuotationNo(String quotationNo) {
        deleteAllDocumentsByQuotationNo(quotationNo, null);
    }

    @Transactional
    public void deleteAllDocumentsByQuotationNo(String quotationNo, Long leadId) {
        if (quotationNo == null || quotationNo.isBlank()) return;

        List<Document> allDocsToDelete = new ArrayList<>();

        // 1. Direct quotationNo match
        List<Document> docs = documentRepository.findByQuotationNo(quotationNo);
        if (docs != null && !docs.isEmpty()) {
            allDocsToDelete.addAll(docs);
        }

        String underscoreNo = quotationNo.replace("/", "_");
        List<Document> underscoreDocs = documentRepository.findByQuotationNo(underscoreNo);
        if (underscoreDocs != null && !underscoreDocs.isEmpty()) {
            allDocsToDelete.addAll(underscoreDocs);
        }

        // 2. Identify revision code
        String revCode = "R0";
        String baseQuot = quotationNo;
        if (quotationNo.matches(".*/R\\d+$")) {
            revCode = quotationNo.substring(quotationNo.lastIndexOf('/') + 1).toUpperCase();
            baseQuot = quotationNo.replaceAll("/R\\d+$", "");
        }

        // 3. Find revisions matching quotationNo or baseQuot
        List<NegotiationRevision> revs = negotiationRevisionRepository.findByQuotationNo(quotationNo);
        if (revs == null || revs.isEmpty()) {
            revs = negotiationRevisionRepository.findByQuotationNo(baseQuot);
        }
        if (leadId != null) {
            List<NegotiationRevision> leadRevs = negotiationRevisionRepository.findByLeadIdFkOrderByUpdatedDateDesc(leadId);
            if (leadRevs != null && !leadRevs.isEmpty()) {
                if (revs == null) revs = new ArrayList<>();
                revs.addAll(leadRevs);
            }
        }

        if (revs != null) {
            for (NegotiationRevision r : revs) {
                String rCode = r.getQuotationRevision() != null ? r.getQuotationRevision().toUpperCase() : "R0";
                if (rCode.equalsIgnoreCase(revCode)) {
                    List<Document> revDocs = documentRepository.findByNegotiationRevisionId(r.getId());
                    if (revDocs != null && !revDocs.isEmpty()) {
                        allDocsToDelete.addAll(revDocs);
                    }
                }
            }
        }

        // Deduplicate and delete
        List<Document> distinctDocs = allDocsToDelete.stream()
                .filter(d -> d != null && d.getId() != null)
                .collect(Collectors.toMap(Document::getId, d -> d, (existing, replacement) -> existing))
                .values().stream().collect(Collectors.toList());

        for (Document d : distinctDocs) {
            d.setNegotiationRevision(null);
        }
        if (!distinctDocs.isEmpty()) {
            documentRepository.deleteAll(distinctDocs);
            log.info("Deleted {} documents for quotation: {}, leadId: {}", distinctDocs.size(), quotationNo, leadId);
        }

        // If R0, clear uploadDocument on Lead
        if ("R0".equalsIgnoreCase(revCode)) {
            Lead lead = null;
            if (leadId != null) {
                lead = leadRepository.findById(leadId).orElse(null);
            }
            if (lead == null) {
                List<Lead> leads = leadRepository.findByQuotationNumber(quotationNo);
                if (leads == null || leads.isEmpty()) {
                    leads = leadRepository.findByQuotationNumber(baseQuot);
                }
                if (leads != null && !leads.isEmpty()) {
                    lead = leads.get(0);
                }
            }
            if (lead != null) {
                lead.setUploadDocument(null);
                lead.setUploadDocument1(null);
                lead.setUploadDocument2(null);
                lead.setUploadDocument3(null);
                leadRepository.saveAndFlush(lead);
                log.info("Cleared lead uploadDocument fields for leadId: {}", lead.getLeadId());
            }
        }
    }

    private DocumentResponse mapToDocumentResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .quotationNo(document.getQuotationNo())
                .fileName(document.getFileName())
                .fileUrl(document.getFileUrl())
                .fileSize(document.getFileSize())
                .fileType(document.getFileType())
                .uploadedDate(document.getUploadedDate())
                .build();
    }
}