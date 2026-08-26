package com.crm.repository;

import com.crm.entity.LeadMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeadMemberRepository extends JpaRepository<LeadMember, Long> {
    List<LeadMember> findByLeadIdFk(Long leadIdFk);
    List<LeadMember> findByLeadIdFkIn(List<Long> leadIdFks);
    List<LeadMember> findByTeamMemberIdFk(Long teamMemberIdFk);
    Optional<LeadMember> findByLeadIdFkAndTeamMemberIdFk(Long leadIdFk, Long teamMemberIdFk);
    void deleteByLeadIdFk(Long leadIdFk);
}

