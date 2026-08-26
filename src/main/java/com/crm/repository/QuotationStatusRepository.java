package com.crm.repository;

import com.crm.entity.QuotationStatusMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuotationStatusRepository extends JpaRepository<QuotationStatusMaster, Long> {

    List<QuotationStatusMaster> findByActiveTrue();

    List<QuotationStatusMaster> findByActiveTrueAndUserIdFk(Long userIdFk);

    @org.springframework.data.jpa.repository.Query("SELECT q FROM QuotationStatusMaster q WHERE q.active = true AND q.statusName IS NOT NULL AND TRIM(q.statusName) != '' AND LOWER(TRIM(q.statusName)) != 'null' AND (q.userIdFk = :userIdFk OR (q.userIdFk IS NULL AND q.statusName NOT IN (SELECT o.statusName FROM QuotationStatusMaster o WHERE o.userIdFk = :userIdFk AND o.active = false)))")
    List<QuotationStatusMaster> findActiveByUserIdFkOrGlobal(@org.springframework.data.repository.query.Param("userIdFk") Long userIdFk);
}
