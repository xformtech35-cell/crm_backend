package com.crm.repository;

import com.crm.entity.LeadGroupsMaster;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadGroupRepository
        extends JpaRepository<LeadGroupsMaster, Long> {

    List<LeadGroupsMaster> findByActiveTrue();

    List<LeadGroupsMaster> findByActiveTrueAndUserIdFk(Long userIdFk);

    @org.springframework.data.jpa.repository.Query("SELECT g FROM LeadGroupsMaster g WHERE g.active = true AND (g.userIdFk = :userIdFk OR g.userIdFk IS NULL)")
    List<LeadGroupsMaster> findActiveByUserIdFkOrGlobal(@org.springframework.data.repository.query.Param("userIdFk") Long userIdFk);
}