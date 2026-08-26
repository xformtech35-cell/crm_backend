package com.crm.repository;

import com.crm.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByUserIdFk(Long userIdFk);
    List<TeamMember> findByTeamIdFk(Long teamIdFk);
    Optional<TeamMember> findFirstByTeamMemberEmail(String email);
    default Optional<TeamMember> findByTeamMemberEmail(String email) {
        return findFirstByTeamMemberEmail(email);
    }
    Optional<TeamMember> findFirstByTeamMemberName(String name);
    default Optional<TeamMember> findByTeamMemberName(String name) {
        return findFirstByTeamMemberName(name);
    }
    long countByTeamMemberRole(Long roleId);

    @Modifying
    @Query("UPDATE TeamMember tm SET tm.teamIdFk = NULL, tm.reportingToFk = NULL WHERE tm.teamIdFk = :teamId AND (tm.isDeleted = false OR tm.isDeleted IS NULL)")
    void clearTeamAssignmentByTeamId(@Param("teamId") Long teamId);

    @Modifying
    @Query("UPDATE TeamMember tm SET tm.reportingToFk = NULL WHERE tm.reportingToFk = :leadId AND (tm.isDeleted = false OR tm.isDeleted IS NULL)")
    void clearReportingToByLeadId(@Param("leadId") Long leadId);
}