package com.crm.service;


import java.io.IOException;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.crm.entity.Negotiation;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.NegotiationRepository;
import com.crm.util.FileUploadUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NegotiationService {

    private final NegotiationRepository negotiationRepository;
    private final FileUploadUtil fileUploadUtil;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Transactional
    public String uploadDocument(Long id, MultipartFile file) {
        log.info("Uploading document for negotiation ID: {}", id);
        
        Negotiation negotiation = negotiationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Negotiation not found with id: " + id));

        try {
            // Delete old document if exists
            if (negotiation.getDocument() != null && !negotiation.getDocument().isBlank()) {
                log.info("Deleting old document: {}", negotiation.getDocument());
                fileUploadUtil.delete(negotiation.getDocument());
            }

            // Upload new file
            String filename = fileUploadUtil.upload(file);
            log.info("File uploaded: {}", filename);
            
            // CRITICAL: Save filename to database
            negotiation.setDocument(filename);
            negotiation.setDocumentUrl("/xformcrm/api/negotiations/" + id + "/document");
            
            negotiationRepository.save(negotiation);
            log.info("Document saved to negotiation ID: {}, filename: {}", id, filename);
            
            return filename;
        } catch (IOException e) {
            log.error("Failed to upload document: {}", e.getMessage());
            throw new RuntimeException("Failed to upload document: " + e.getMessage());
        }
    }

    // NEW: Get only the filename from database
    public String getDocumentFilename(Long id) {
        log.info("Getting document filename for negotiation ID: {}", id);
        
        Negotiation negotiation = negotiationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Negotiation not found with id: " + id));

        String filename = negotiation.getDocument();
        log.info("Document filename: {}", filename);
        return filename;
    }

    public Path getDocumentPath(Long id) {
        log.info("Getting document path for negotiation ID: {}", id);
        
        String filename = getDocumentFilename(id);
        if (filename == null || filename.isBlank()) {
            return null;
        }
        
        return fileUploadUtil.getFilePath(filename);
    }

    @Transactional
    public void deleteDocument(Long id) {
        log.info("Deleting document for negotiation ID: {}", id);
        
        Negotiation negotiation = negotiationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Negotiation not found with id: " + id));

        String filename = negotiation.getDocument();
        if (filename != null && !filename.isBlank()) {
            log.info("Deleting file: {}", filename);
            fileUploadUtil.delete(filename);
            negotiation.setDocument(null);
            negotiation.setDocumentUrl(null);
            negotiationRepository.save(negotiation);
            log.info("Document deleted successfully");
        } else {
            log.warn("No document to delete for negotiation ID: {}", id);
        }
    }

    @Transactional
    public void deleteNegotiation(Long id) {
        log.info("Soft deleting negotiation with id: {}", id);
        Negotiation negotiation = negotiationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Negotiation not found with id: " + id));
        negotiation.setIsDeleted(true);
        negotiation.setDeletedAt(java.time.LocalDateTime.now());
        negotiationRepository.save(negotiation);
    }
}