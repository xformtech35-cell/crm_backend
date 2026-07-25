package com.crm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.crm.entity.NegotiationRevision;

@Repository
public interface NegotiationRevisionRepository
        extends JpaRepository<NegotiationRevision, Long> {

    List<NegotiationRevision> findByNegotiationIdOrderByUpdatedDateDesc(Long negotiationId);
    
//    Optional<NegotiationRevision> findByQuotationNo(String quotationNo);

    List<NegotiationRevision> findByQuotationNo(String quotationNo);

    
    // Find by quotation number (returns list since multiple revisions can have same quotation no)
    
    // Find by quotation number and revision (returns unique result)
    Optional<NegotiationRevision> findByQuotationNoAndQuotationRevision(String quotationNo, String quotationRevision);
    
    // Or use native query
    @Query("SELECT n FROM NegotiationRevision n WHERE n.quotationNo = :quotationNo AND n.quotationRevision = :revision")
    Optional<NegotiationRevision> findByQuotationNoAndRevision(@Param("quotationNo") String quotationNo, @Param("revision") String revision);
}