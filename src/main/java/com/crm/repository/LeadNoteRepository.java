package com.crm.repository;

import com.crm.entity.LeadNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadNoteRepository extends JpaRepository<LeadNote, Long> {
    List<LeadNote> findByLeadIdFkOrderByNoteDateDesc(Long leadIdFk);

    List<LeadNote> findAllByOrderByNoteDateDesc();

    long countByLeadIdFk(Long leadIdFk);

    void deleteByLeadIdFk(Long leadIdFk);
}

