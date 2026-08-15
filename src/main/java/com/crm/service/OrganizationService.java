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
            return organizationRepository.findByUserIdFkIn(teamUserIds);
        }
        List<Long> companyUserIds = leadService.getCompanyUserIds(userId, role);
        return organizationRepository.findByUserIdFkIn(companyUserIds);
    }

    public Organization getById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));
    }

    public Organization create(OrganizationRequest req, Long userId) {
        Organization org = mapToEntity(req, new Organization());
        org.setUserIdFk(userId);
        return organizationRepository.save(org);
    }

    public Organization update(Long id, OrganizationRequest req) {
        Organization org = getById(id);
        return organizationRepository.save(mapToEntity(req, org));
    }

    @Transactional
    public void delete(Long id) {
        Organization org = getById(id);
        Long adminUserId = org.getUserIdFk();
        
        // Delete organization itself first
        organizationRepository.delete(org);

        if (adminUserId != null) {
            // 1. Delete Attendance records
            List<Attendance> attendances = attendanceRepository.findByUserIdFk(adminUserId);
            attendanceRepository.deleteAll(attendances);

            // 2. Delete Integration Configs
            List<IntegrationConfig> integrationConfigs = integrationConfigRepository.findByUserIdFk(adminUserId);
            integrationConfigRepository.deleteAll(integrationConfigs);

            // 3. Delete Data Scope Configs
            List<DataScopeConfig> dataScopeConfigs = dataScopeConfigRepository.findByCompanyAdminIdFk(adminUserId);
            dataScopeConfigRepository.deleteAll(dataScopeConfigs);

            // 4. Delete Team Members and their login accounts (Users)
            List<TeamMember> teamMembers = teamMemberRepository.findByUserIdFk(adminUserId);
            for (TeamMember member : teamMembers) {
                if (member.getTeamMemberEmail() != null) {
                    userRepository.findByUserEmail(member.getTeamMemberEmail())
                            .ifPresent(userRepository::delete);
                }
            }
            teamMemberRepository.deleteAll(teamMembers);

            // 5. Delete Teams
            List<Team> teams = teamRepository.findByUserIdFk(adminUserId);
            teamRepository.deleteAll(teams);

            // 6. Delete Tasks and TaskTimeLogs
            List<Task> tasks = taskRepository.findByUserIdFk(adminUserId);
            for (Task task : tasks) {
                List<TaskTimeLog> logs = taskTimeLogRepository.findByTaskId(task.getTaskId());
                taskTimeLogRepository.deleteAll(logs);
            }
            taskRepository.deleteAll(tasks);

            // 7. Delete Projects
            List<Project> projects = projectRepository.findByUserIdFk(adminUserId);
            projectRepository.deleteAll(projects);

            // 8. Delete Opportunities
            List<Opportunity> opps = opportunityRepository.findByUserIdFk(adminUserId);
            opportunityRepository.deleteAll(opps);

            // 9. Delete Contacts
            List<Contact> contacts = contactRepository.findByUserIdFk(adminUserId);
            contactRepository.deleteAll(contacts);

            // 10. Delete Leads and their related entities (Negotiations, Notes, Reminders, Scores)
            List<Lead> leads = leadRepository.findByUserIdFk(adminUserId);
            for (Lead lead : leads) {
                leadNoteRepository.deleteByLeadIdFk(lead.getLeadId());
                leadReminderRepository.deleteByLeadIdFk(lead.getLeadId());
                leadScoreRepository.findByLeadIdFk(lead.getLeadId())
                        .ifPresent(leadScoreRepository::delete);

                List<Negotiation> negotiations = negotiationRepository.findByLeadIdFk(lead.getLeadId());
                for (Negotiation neg : negotiations) {
                    List<NegotiationRevision> revisions = negotiationRevisionRepository.findByNegotiationIdOrderByUpdatedDateDesc(neg.getId());
                    for (NegotiationRevision rev : revisions) {
                        List<Document> docs = documentRepository.findByNegotiationRevisionId(rev.getId());
                        documentRepository.deleteAll(docs);
                    }
                    negotiationRevisionRepository.deleteAll(revisions);
                }
                negotiationRepository.deleteAll(negotiations);
            }
            leadRepository.deleteAll(leads);

            // 11. Delete Roles
            List<Role> roles = roleRepository.findByUserIdFk(adminUserId);
            roleRepository.deleteAll(roles);

            // 12. Delete Admin User record itself
            userRepository.findById(adminUserId)
                    .ifPresent(userRepository::delete);
        }
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
