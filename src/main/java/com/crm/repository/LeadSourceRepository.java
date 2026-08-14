package com.crm.repository;

import com.crm.entity.LeadSourceMaster;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadSourceRepository
        extends JpaRepository<LeadSourceMaster, Long> {

    List<LeadSourceMaster> findByActiveTrue();

    List<LeadSourceMaster> findByActiveTrueAndUserIdFk(Long userIdFk);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM LeadSourceMaster s WHERE s.active = true AND s.sourceName IS NOT NULL AND TRIM(s.sourceName) != '' AND LOWER(TRIM(s.sourceName)) != 'null' AND (s.userIdFk = :userIdFk OR s.userIdFk IS NULL)")
    List<LeadSourceMaster> findActiveByUserIdFkOrGlobal(@org.springframework.data.repository.query.Param("userIdFk") Long userIdFk);
}