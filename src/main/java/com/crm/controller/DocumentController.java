package com.crm.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

import com.crm.dto.response.DocumentResponse;
import com.crm.service.DocumentService;
import com.crm.util.FileUploadUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/documents")  // Removed /xformcrm
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;
    private final FileUploadUtil fileUploadUtil;

    /**
     * Upload documents
     * POST http://localhost:8080/xformcrm/api/documents/upload?quotationNo=UWS/RRW/26-27/003/R3
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocuments(
            @RequestParam("quotationNo") String quotationNo,
            @RequestParam(value = "leadId", required = false) Long leadId,
            @RequestParam("files") List<MultipartFile> files) {
        
        try {
            log.info("Uploading {} documents for quotation: {}, leadId: {}", files.size(), quotationNo, leadId);
            
            if (files == null || files.isEmpty()) {
                return ResponseEntity.badRequest().body("No files to upload");
            }
            
            List<DocumentResponse> responses = documentService.uploadDocuments(quotationNo, leadId, files);
            return new ResponseEntity<>(responses, HttpStatus.CREATED);
            
        } catch (Exception e) {
            log.error("Error uploading documents: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload documents: " + e.getMessage());
        }
    }

    /**
     * Get documents by quotation number
     * GET http://localhost:8080/xformcrm/api/documents?quotationNo=UWS/RRW/26-27/003/R3
     */
    @GetMapping
    public ResponseEntity<?> getDocumentsByQuotationNo(
            @RequestParam("quotationNo") String quotationNo) {
        
        try {
            log.info("Fetching documents for quotation: {}", quotationNo);
            List<DocumentResponse> documents = documentService.getDocumentsByQuotationNo(quotationNo);
            
            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No documents found for quotation: " + quotationNo);
            }
            
            return ResponseEntity.ok(documents);
            
        } catch (Exception e) {
            log.error("Error fetching documents: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch documents: " + e.getMessage());
        }
    }

    /**
     * Get document by ID
     * GET http://localhost:8080/xformcrm/api/documents/id/1
     */
    @GetMapping("/id/{documentId}")
    public ResponseEntity<?> getDocumentById(@PathVariable Long documentId) {
        try {
            DocumentResponse document = documentService.getDocumentById(documentId);
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            log.error("Error fetching document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Document not found with id: " + documentId);
        }
    }

    /**
     * Delete document by ID
     * DELETE http://localhost:8080/xformcrm/api/documents/id/1
     */
    @DeleteMapping("/id/{documentId}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long documentId) {
        try {
            log.info("Deleting document with id: {}", documentId);
            documentService.deleteDocument(documentId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting document: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete document: " + e.getMessage());
        }
        
        
    }

    /**
     * Delete all documents by quotation number
     * DELETE http://localhost:8080/xformcrm/api/documents?quotationNo=UWS/RRW/26-27/003/R3
     */
    @DeleteMapping
    public ResponseEntity<?> deleteAllDocumentsByQuotationNo(
            @RequestParam("quotationNo") String quotationNo,
            @RequestParam(value = "leadId", required = false) Long leadId) {
        
        try {
            log.info("Deleting all documents for quotation: {}, leadId: {}", quotationNo, leadId);
            documentService.deleteAllDocumentsByQuotationNo(quotationNo, leadId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting documents: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete documents: " + e.getMessage());
        }
    }
    
   
}