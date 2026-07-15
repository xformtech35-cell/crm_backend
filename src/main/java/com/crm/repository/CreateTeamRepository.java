package com.crm.repository;

import com.crm.entity.CreateTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreateTeamRepository extends JpaRepository<CreateTeam, Long> {
    List<CreateTeam> findByTeamIdFk(Long teamIdFk);
    List<CreateTeam> findByTeamMemberIdFk(Long teamMemberIdFk);
    void deleteByTeamIdFkAndTeamMemberIdFk(Long teamIdFk, Long teamMemberIdFk);
    List<CreateTeam> findByUserIdFk(Long userIdFk);
}
