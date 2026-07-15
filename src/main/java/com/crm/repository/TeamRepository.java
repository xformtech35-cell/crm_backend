package com.crm.repository;

import com.crm.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    boolean existsByTeamName(String teamName);
    java.util.List<Team> findByUserIdFk(Long userIdFk);
    boolean existsByTeamNameAndUserIdFk(String teamName, Long userIdFk);
}
