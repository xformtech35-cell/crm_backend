package com.crm.repository;

import com.crm.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {
    List<Lead> findByUserIdFk(Long userIdFk);
    List<Lead> findByLeadStatus(String leadStatus);
    List<Lead> findByUserIdFkAndLeadStatus(Long userIdFk, String leadStatus);
    List<Lead> findByLeadAssignedMember(Long leadAssignedMember);
    List<Lead> findByLeadAssignedMemberAndLeadStatus(Long leadAssignedMember, String leadStatus);
    List<Lead> findByQuotationNumber(String quotationNumber);

    @Query("SELECT l FROM Lead l WHERE (l.groupId = :companyAdminId OR l.userIdFk IN :userIds OR l.leadAssignedMember IN :userIds) AND (l.isDeleted = false OR l.isDeleted IS NULL) ORDER BY l.leadId DESC")
    List<Lead> findByCompanyAdminCriteria(@Param("companyAdminId") Long companyAdminId, @Param("userIds") List<Long> userIds);

    @Query("SELECT l FROM Lead l WHERE (l.userIdFk IN :userIds OR l.leadAssignedMember IN :userIds) AND (l.isDeleted = false OR l.isDeleted IS NULL) ORDER BY l.leadId DESC")
    List<Lead> findByUserIdFkInOrLeadAssignedMemberIn(@Param("userIds") List<Long> userIds);

    @Query("SELECT DISTINCT l FROM Lead l WHERE l.leadAssignedTeam IN :teamIds AND (l.isDeleted = false OR l.isDeleted IS NULL) ORDER BY l.leadId DESC")
    List<Lead> findByTeamDataCriteria(@Param("teamIds") List<Long> teamIds);

    @Query("SELECT DISTINCT l FROM Lead l WHERE (l.userIdFk IN :userIds OR l.leadAssignedMember IN :userIds OR l.leadAssignedTeam IN :teamIds OR LOWER(l.createdBy) IN :emails OR LOWER(l.updatedBy) IN :emails) AND (l.isDeleted = false OR l.isDeleted IS NULL) ORDER BY l.leadId DESC")
    List<Lead> findByTeamLeadCriteria(@Param("userIds") List<Long> userIds, @Param("teamIds") List<Long> teamIds, @Param("emails") List<String> emails);

    @Query("SELECT l FROM Lead l WHERE (l.userIdFk IN :userIds OR l.leadAssignedMember IN :userIds) AND l.leadStatus = :status AND (l.isDeleted = false OR l.isDeleted IS NULL) ORDER BY l.leadId DESC")
    List<Lead> findByUserIdFkInAndLeadStatus(@Param("userIds") List<Long> userIds, @Param("status") String status);

    @Query("SELECT l FROM Lead l WHERE (l.userIdFk = :userId OR l.leadAssignedMember = :userId) AND (l.isDeleted = false OR l.isDeleted IS NULL) ORDER BY l.leadId DESC")
    List<Lead> findByUserIdFkOrLeadAssignedMember(@Param("userId") Long userId);

    @Query("SELECT l FROM Lead l WHERE (l.userIdFk = :userId OR l.leadAssignedMember = :userId) AND l.leadStatus = :status AND (l.isDeleted = false OR l.isDeleted IS NULL) ORDER BY l.leadId DESC")
    List<Lead> findByUserIdFkOrLeadAssignedMemberAndLeadStatus(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT DISTINCT l FROM Lead l WHERE " +
           "((l.userIdFk = :userId OR " +
           "l.leadAssignedMember = :userId OR " +
           "(:teamMemberId IS NOT NULL AND (l.leadAssignedMember = :teamMemberId OR l.leadId IN (SELECT lm.leadIdFk FROM LeadMember lm WHERE lm.teamMemberIdFk = :teamMemberId))) OR " +
           "(:userEmail IS NOT NULL AND LOWER(l.createdBy) = LOWER(:userEmail))) AND " +
           "(l.isDeleted = false OR l.isDeleted IS NULL)) " +
           "ORDER BY l.leadId DESC")
    List<Lead> findByOwnDataCriteria(
        @Param("userId") Long userId,
        @Param("teamMemberId") Long teamMemberId,
        @Param("userEmail") String userEmail
    );

    @Query("SELECT DISTINCT l FROM Lead l WHERE " +
           "((l.userIdFk = :userId OR " +
           "l.leadAssignedMember = :userId OR " +
           "(:teamMemberId IS NOT NULL AND (l.leadAssignedMember = :teamMemberId OR l.leadId IN (SELECT lm.leadIdFk FROM LeadMember lm WHERE lm.teamMemberIdFk = :teamMemberId))) OR " +
           "(:userEmail IS NOT NULL AND LOWER(l.createdBy) = LOWER(:userEmail))) AND " +
           "l.leadStatus = :status AND " +
           "(l.isDeleted = false OR l.isDeleted IS NULL)) " +
           "ORDER BY l.leadId DESC")
    List<Lead> findByOwnDataCriteriaAndStatus(
        @Param("userId") Long userId,
        @Param("teamMemberId") Long teamMemberId,
        @Param("userEmail") String userEmail,
        @Param("status") String status
    );





    Optional<Lead> findByUniqueQueryId(String uniqueQueryId);

    boolean existsByUniqueQueryId(String uniqueQueryId);
    long countByLeadStatus(String leadStatus);
    long countByUserIdFk(Long userIdFk);    

    @Query("SELECT COUNT(l) FROM Lead l WHERE (l.userIdFk IN :userIds OR l.leadAssignedMember IN :userIds)")
    long countByUserIdFkIn(@Param("userIds") List<Long> userIds);

    @Query("SELECT COALESCE(NULLIF(l.leadOutcomeStatus, ''), NULLIF(l.leadStatus, ''), 'Open') AS status, COUNT(l) AS count FROM Lead l GROUP BY COALESCE(NULLIF(l.leadOutcomeStatus, ''), NULLIF(l.leadStatus, ''), 'Open')")
    List<Object[]> countGroupByStatus();

    @Query("SELECT l.leadSource AS source, COUNT(l) AS count FROM Lead l GROUP BY l.leadSource")
    List<Object[]> countGroupBySource();

    @Query("SELECT COALESCE(NULLIF(l.leadOutcomeStatus, ''), NULLIF(l.leadStatus, ''), 'Open') AS status, COUNT(l) AS count FROM Lead l WHERE l.userIdFk = :userId GROUP BY COALESCE(NULLIF(l.leadOutcomeStatus, ''), NULLIF(l.leadStatus, ''), 'Open')")
    List<Object[]> countGroupByStatusForUser(@Param("userId") Long userId);

    @Query("SELECT l.leadSource AS source, COUNT(l) AS count FROM Lead l WHERE l.userIdFk = :userId GROUP BY l.leadSource")
    List<Object[]> countGroupBySourceForUser(@Param("userId") Long userId);

    @Query("SELECT COALESCE(NULLIF(l.leadOutcomeStatus, ''), NULLIF(l.leadStatus, ''), 'Open') AS status, COUNT(l) AS count FROM Lead l WHERE (l.userIdFk IN :userIds OR l.leadAssignedMember IN :userIds) GROUP BY COALESCE(NULLIF(l.leadOutcomeStatus, ''), NULLIF(l.leadStatus, ''), 'Open')")
    List<Object[]> countGroupByStatusForUserIds(@Param("userIds") List<Long> userIds);

    @Query("SELECT l.leadSource AS source, COUNT(l) AS count FROM Lead l WHERE (l.userIdFk IN :userIds OR l.leadAssignedMember IN :userIds) GROUP BY l.leadSource")
    List<Object[]> countGroupBySourceForUserIds(@Param("userIds") List<Long> userIds);

    @Query("SELECT l FROM Lead l WHERE l.leadCreatedDate BETWEEN :fromDate AND :toDate")
    List<Lead> findByLeadCreatedDateBetween(@Param("fromDate") java.time.LocalDateTime fromDate, @Param("toDate") java.time.LocalDateTime toDate);

}


