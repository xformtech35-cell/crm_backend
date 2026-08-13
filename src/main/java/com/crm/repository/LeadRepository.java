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

    @Query("SELECT l FROM Lead l WHERE (l.userIdFk IN :userIds OR l.leadAssignedMember IN :userIds) ORDER BY l.leadId DESC")
    List<Lead> findByUserIdFkInOrLeadAssignedMemberIn(@Param("userIds") List<Long> userIds);

    @Query("SELECT DISTINCT l FROM Lead l WHERE (l.userIdFk IN :userIds OR l.leadAssignedMember IN :userIds OR l.leadAssignedTeam IN :teamIds OR LOWER(l.createdBy) IN :emails OR LOWER(l.updatedBy) IN :emails) ORDER BY l.leadId DESC")
    List<Lead> findByTeamLeadCriteria(@Param("userIds") List<Long> userIds, @Param("teamIds") List<Long> teamIds, @Param("emails") List<String> emails);

    @Query("SELECT l FROM Lead l WHERE (l.userIdFk IN :userIds OR l.leadAssignedMember IN :userIds) AND l.leadStatus = :status ORDER BY l.leadId DESC")
    List<Lead> findByUserIdFkInAndLeadStatus(@Param("userIds") List<Long> userIds, @Param("status") String status);

    @Query("SELECT l FROM Lead l WHERE (l.userIdFk = :userId OR l.leadAssignedMember = :userId) ORDER BY l.leadId DESC")
    List<Lead> findByUserIdFkOrLeadAssignedMember(@Param("userId") Long userId);

    @Query("SELECT l FROM Lead l WHERE (l.userIdFk = :userId OR l.leadAssignedMember = :userId) AND l.leadStatus = :status ORDER BY l.leadId DESC")
    List<Lead> findByUserIdFkOrLeadAssignedMemberAndLeadStatus(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT DISTINCT l FROM Lead l WHERE " +
           "(l.userIdFk = :userId OR " +
           "l.leadAssignedMember = :userId OR " +
           "(:teamMemberId IS NOT NULL AND l.leadAssignedMember = :teamMemberId) OR " +
           "(:userEmail IS NOT NULL AND LOWER(l.createdBy) = LOWER(:userEmail)) OR " +
           "(:teamMemberName IS NOT NULL AND LOWER(l.leadRef) = LOWER(:teamMemberName))) " +
           "ORDER BY l.leadId DESC")
    List<Lead> findByOwnDataCriteria(
        @Param("userId") Long userId,
        @Param("teamMemberId") Long teamMemberId,
        @Param("userEmail") String userEmail,
        @Param("teamMemberName") String teamMemberName
    );

    @Query("SELECT DISTINCT l FROM Lead l WHERE " +
           "((l.userIdFk = :userId OR " +
           "l.leadAssignedMember = :userId OR " +
           "(:teamMemberId IS NOT NULL AND l.leadAssignedMember = :teamMemberId) OR " +
           "(:userEmail IS NOT NULL AND LOWER(l.createdBy) = LOWER(:userEmail)) OR " +
           "(:teamMemberName IS NOT NULL AND LOWER(l.leadRef) = LOWER(:teamMemberName))) AND " +
           "l.leadStatus = :status) " +
           "ORDER BY l.leadId DESC")
    List<Lead> findByOwnDataCriteriaAndStatus(
        @Param("userId") Long userId,
        @Param("teamMemberId") Long teamMemberId,
        @Param("userEmail") String userEmail,
        @Param("teamMemberName") String teamMemberName,
        @Param("status") String status
    );


    Optional<Lead> findByUniqueQueryId(String uniqueQueryId);

    boolean existsByUniqueQueryId(String uniqueQueryId);
    long countByLeadStatus(String leadStatus);
    long countByUserIdFk(Long userIdFk);    

    @Query("SELECT l.leadStatus AS status, COUNT(l) AS count FROM Lead l GROUP BY l.leadStatus")
    List<Object[]> countGroupByStatus();

    @Query("SELECT l.leadSource AS source, COUNT(l) AS count FROM Lead l GROUP BY l.leadSource")
    List<Object[]> countGroupBySource();

    @Query("SELECT l.leadStatus AS status, COUNT(l) AS count FROM Lead l WHERE l.userIdFk = :userId GROUP BY l.leadStatus")
    List<Object[]> countGroupByStatusForUser(@Param("userId") Long userId);

    @Query("SELECT l.leadSource AS source, COUNT(l) AS count FROM Lead l WHERE l.userIdFk = :userId GROUP BY l.leadSource")
    List<Object[]> countGroupBySourceForUser(@Param("userId") Long userId);

    @Query("SELECT l FROM Lead l WHERE l.leadCreatedDate BETWEEN :fromDate AND :toDate")
    List<Lead> findByLeadCreatedDateBetween(@Param("fromDate") java.time.LocalDateTime fromDate, @Param("toDate") java.time.LocalDateTime toDate);

}


