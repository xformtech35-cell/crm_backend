package com.crm.repository;

import com.crm.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    List<Document> findByNegotiationRevisionId(Long negotiationRevisionId);
    
    List<Document> findByQuotationNo(String quotationNo);
    
    void deleteByNegotiationRevisionId(Long negotiationRevisionId);
    
    void deleteByQuotationNo(String quotationNo);
}