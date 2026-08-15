package com.crm.repository;

import com.crm.entity.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity> {
    List<Opportunity> findByUserIdFk(Long userIdFk);
    List<Opportunity> findByUserIdFkIn(List<Long> userIds);
    List<Opportunity> findByOppStatus(String oppStatus);
    List<Opportunity> findByLeadIdFk(Long leadIdFk);
    void deleteByLeadIdFk(Long leadIdFk);
    long countByOppStatus(String oppStatus);
    long countByUserIdFk(Long userIdFk);

    @Query("SELECT o.oppStatus AS status, COUNT(o) AS count FROM Opportunity o GROUP BY o.oppStatus")
    List<Object[]> countGroupByStatus();

    @Query("SELECT o.oppStatus AS status, COUNT(o) AS count FROM Opportunity o WHERE o.userIdFk = :userId GROUP BY o.oppStatus")
    List<Object[]> countGroupByStatusForUser(@Param("userId") Long userId);
}
