package com.crm.service;

import com.crm.dto.request.TeamRequest;
import com.crm.entity.Team;
import com.crm.exception.BadRequestException;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final com.crm.util.AuthUtil authUtil;

    public List<Team> getAllTeams(com.crm.entity.User user, Long companyAdminId, String role) {
        if (authUtil.isSuperAdmin(role)) {
            return teamRepository.findAll();
        }
        String scopeMode = authUtil.resolveDataScopeMode(user, "TEAMS");
        if ("ALL_DATA".equals(scopeMode)) {
            return teamRepository.findByUserIdFk(companyAdminId);
        }
        return authUtil.getLedTeamsForUser(user);
    }

    public Team getById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", id));
    }

    public Team create(TeamRequest req, Long companyAdminId, String role) {
        boolean exists = authUtil.isSuperAdmin(role)
                ? teamRepository.existsByTeamName(req.getTeamName())
                : teamRepository.existsByTeamNameAndUserIdFk(req.getTeamName(), companyAdminId);
        if (exists) {
            throw new BadRequestException("Team with name '" + req.getTeamName() + "' already exists");
        }
        return teamRepository.save(Team.builder()
                .teamName(req.getTeamName())
                .userIdFk(authUtil.isSuperAdmin(role) ? null : companyAdminId)
                .teamLeadId(req.getTeamLeadId())
                .build());
    }

    public Team update(Long id, TeamRequest req) {
        Team team = getById(id);
        team.setTeamName(req.getTeamName());
        team.setTeamLeadId(req.getTeamLeadId());
        return teamRepository.save(team);
    }

    public void delete(Long id) {
        teamRepository.delete(getById(id));
    }
}
