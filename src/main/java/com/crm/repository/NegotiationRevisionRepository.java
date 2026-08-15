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
    
    List<NegotiationRevision> findByNegotiationIdOrLeadIdFkOrderByUpdatedDateDesc(Long negotiationId, Long leadIdFk);
    
    List<NegotiationRevision> findByQuotationNo(String quotationNo);

    Optional<NegotiationRevision> findFirstByNegotiationIdAndQuotationRevision(Long negotiationId, String quotationRevision);
}