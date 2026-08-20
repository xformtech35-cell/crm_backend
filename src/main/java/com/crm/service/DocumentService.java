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

    // Method 1: Upload using Quotation Number (Handles duplicates)
    @Transactional
    public List<DocumentResponse> uploadDocuments(String quotationNo, List<MultipartFile> files) {
        try {
            List<NegotiationRevision> revisions = negotiationRevisionRepository
                    .findByQuotationNo(quotationNo);
            
            NegotiationRevision negotiationRevision = null;
            if (!revisions.isEmpty()) {
                negotiationRevision = revisions.get(0);
                log.info("Using revision ID: {} for quotation: {}", negotiationRevision.getId(), quotationNo);
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
        Document doc = documentRepository.findById(documentId)
                .orElse(null);
        if (doc != null) {
            documentRepository.delete(doc);
            log.info("Document permanently deleted with id: {}", documentId);
        }
    }

    @Transactional
    public void deleteAllDocumentsByQuotationNo(String quotationNo) {
        List<Document> docs = documentRepository.findByQuotationNo(quotationNo);
        if (docs != null && !docs.isEmpty()) {
            documentRepository.deleteAll(docs);
            log.info("All documents permanently deleted for quotation: {}", quotationNo);
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