package com.crm.repository;

import com.crm.entity.LeadScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeadScoreRepository extends JpaRepository<LeadScore, Long> {

    Optional<LeadScore> findFirstByLeadIdFk(Long leadIdFk);
    default Optional<LeadScore> findByLeadIdFk(Long leadIdFk) {
        return findFirstByLeadIdFk(leadIdFk);
    }

    /** Returns all scores sorted highest first — used by the /scores endpoint */
    List<LeadScore> findAllByOrderByScoreDesc();

    void deleteByLeadIdFk(Long leadIdFk);
}
