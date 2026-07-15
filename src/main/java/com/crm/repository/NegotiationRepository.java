package com.crm.repository;
import java.util.List;
import java.util.Optional;

import com.crm.entity.Negotiation;

import org.springframework.data.jpa.repository.JpaRepository;


public interface NegotiationRepository extends JpaRepository<Negotiation, Long> {
    List<Negotiation> findByLeadIdFk(Long leadIdFk);
    List<Negotiation> findByUserIdFk(Long userIdFk);
    Optional<Negotiation> findFirstByLeadIdFk(Long leadIdFk);
}