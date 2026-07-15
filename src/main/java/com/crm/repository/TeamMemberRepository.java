package com.crm.repository;

import com.crm.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByUserIdFk(Long userIdFk);
    Optional<TeamMember> findByTeamMemberEmail(String email);
}
