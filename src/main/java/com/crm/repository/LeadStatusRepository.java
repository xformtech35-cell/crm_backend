package com.crm.repository;

import com.crm.entity.LeadStatusMaster;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadStatusRepository extends JpaRepository<LeadStatusMaster, Long> {

    List<LeadStatusMaster> findByActiveTrue();

    List<LeadStatusMaster> findByActiveTrueAndUserIdFk(Long userIdFk);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM LeadStatusMaster s WHERE s.active = true AND s.statusName IS NOT NULL AND TRIM(s.statusName) != '' AND LOWER(TRIM(s.statusName)) != 'null' AND (s.userIdFk = :userIdFk OR (s.userIdFk IS NULL AND s.statusName NOT IN (SELECT o.statusName FROM LeadStatusMaster o WHERE o.userIdFk = :userIdFk AND o.active = false)))")
    List<LeadStatusMaster> findActiveByUserIdFkOrGlobal(@org.springframework.data.repository.query.Param("userIdFk") Long userIdFk);
}
