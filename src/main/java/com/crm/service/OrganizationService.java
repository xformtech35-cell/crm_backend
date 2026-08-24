package com.crm.service;

import com.crm.dto.request.OrganizationRequest;
import com.crm.entity.Organization;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.*;
import com.crm.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final com.crm.util.AuthUtil authUtil;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final RoleRepository roleRepository;
    private final LeadRepository leadRepository;
    private final OpportunityRepository opportunityRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final IntegrationConfigRepository integrationConfigRepository;
    private final DataScopeConfigRepository dataScopeConfigRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeadNoteRepository leadNoteRepository;
    private final LeadReminderRepository leadReminderRepository;
    private final LeadScoreRepository leadScoreRepository;
    private final NegotiationRepository negotiationRepository;
    private final NegotiationRevisionRepository negotiationRevisionRepository;
    private final DocumentRepository documentRepository;
    private final ContactRepository contactRepository;
    private final LeadService leadService;
    private final TaskTimeLogRepository taskTimeLogRepository;

    public List<Organization> getAllOrganizations(Long userId, String role) {
        if (authUtil.isSuperAdmin(role)) {
            return organizationRepository.findAll();
        }
        User user = userRepository.findById(userId).orElse(null);
        String scopeMode = authUtil.resolveDataScopeMode(user, "ORGANIZATIONS");

        if ("ALL_DATA".equals(scopeMode)) {
            List<Long> companyUserIds = leadService.getCompanyUserIds(userId, role);
            return organizationRepository.findByUserIdFkIn(companyUserIds);
        }
        if ("TEAM_DATA".equals(scopeMode)) {
            List<Long> teamUserIds = authUtil.getTeamLeadMemberUserIds(user);
            if (teamUserIds.isEmpty()) teamUserIds = List.of(-1L);
            List<Organization> orgs = organizationRepository.findByUserIdFkIn(teamUserIds);
            if (orgs.isEmpty()) {
                List<Long> companyUserIds = leadService.getCompanyUserIds(userId, role);
                return organizationRepository.findByUserIdFkIn(companyUserIds);
            }
            return orgs;
        }
        List<Organization> userOrgs = organizationRepository.findByUserIdFk(userId);
        if (userOrgs.isEmpty()) {
            List<Long> companyUserIds = leadService.getCompanyUserIds(userId, role);
            return organizationRepository.findByUserIdFkIn(companyUserIds);
        }
        return userOrgs;
    }

    public Organization getById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));
    }

    public Organization create(OrganizationRequest req, Long userId) {
        String name = req.getOrganizationName() != null ? req.getOrganizationName().trim() : "";
        if (!name.isEmpty()) {
            boolean duplicate = organizationRepository.findByUserIdFk(userId).stream()
                    .anyMatch(o -> o.getOrganizationName() != null && o.getOrganizationName().trim().equalsIgnoreCase(name));
            if (duplicate) {
                throw new IllegalArgumentException("Organization '" + name + "' already exists!");
            }
        }
        Organization org = mapToEntity(req, new Organization());
        org.setUserIdFk(userId);
        return organizationRepository.save(org);
    }

    public Organization update(Long id, OrganizationRequest req) {
        Organization org = getById(id);
        String name = req.getOrganizationName() != null ? req.getOrganizationName().trim() : "";
        if (!name.isEmpty()) {
            boolean duplicate = organizationRepository.findByUserIdFk(org.getUserIdFk()).stream()
                    .anyMatch(o -> o.getOrganizationName() != null && o.getOrganizationName().trim().equalsIgnoreCase(name) && !Objects.equals(o.getOrganizationId(), id));
            if (duplicate) {
                throw new IllegalArgumentException("Organization '" + name + "' already exists!");
            }
        }
        return organizationRepository.save(mapToEntity(req, org));
    }

    @Transactional
    public void delete(Long id) {
        Organization org = getById(id);
        organizationRepository.delete(org);
    }

    private Organization mapToEntity(OrganizationRequest req, Organization org) {
        org.setOrganizationName(req.getOrganizationName());
        org.setOrganizationMoblieNo(req.getOrganizationMoblieNo());
        org.setOrganizationEmail(req.getOrganizationEmail());
        org.setOrganizationAddress(req.getOrganizationAddress());
        org.setOrganizationCity(req.getOrganizationCity());
        org.setOrganizationState(req.getOrganizationState());
        org.setOrganizationCountry(req.getOrganizationCountry());
        org.setOrganizationBackground(req.getOrganizationBackground());
        org.setOrganizationOccasion(req.getOrganizationOccasion());
        org.setOrganizationPostcode(req.getOrganizationPostcode());
        org.setOrganizationOccasionDate(req.getOrganizationOccasionDate());
        return org;
    }
}
