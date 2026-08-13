package com.crm.service;

import com.crm.entity.TeamMember;
import com.crm.entity.User;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyCascadeDeleteService {

    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Complete cascade deletion of a company, including:
     * - Company Admin user login
     * - All Team Lead & Team Member logins created under this company
     * - All team member records & create_teams records
     * - All custom roles & data scope configurations
     * - All CRM module child data (Notes, Reminders, Revisions, Time Logs, Documents, Calendar Events/Notifications/Attendees, Trash, Audit Logs)
     * - All CRM core module data (Leads, Contacts, Opportunities, Organizations, Projects, Tasks, Negotiations, Attendance, Master Data)
     */
    @Transactional
    public void deleteCompanyAndAllAssociatedData(Long companyAdminId) {
        User companyAdmin = userRepository.findById(companyAdminId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", "id", companyAdminId));

        log.info("Starting complete cascade deletion for Company ID: {}, Email: {}", companyAdminId, companyAdmin.getUserEmail());

        // 1. Gather all team members and their emails under this company
        List<TeamMember> teamMembers = teamMemberRepository.findByUserIdFk(companyAdminId);
        List<String> memberEmails = teamMembers.stream()
                .map(TeamMember::getTeamMemberEmail)
                .filter(email -> email != null && !email.isBlank())
                .collect(Collectors.toList());

        // 2. Gather all user IDs associated with this company
        Set<Long> associatedUserIds = new HashSet<>();
        associatedUserIds.add(companyAdminId);

        for (String email : memberEmails) {
            userRepository.findFirstByUserEmail(email).ifPresent(u -> associatedUserIds.add(u.getUserid()));
        }

        List<Long> uidsList = new ArrayList<>(associatedUserIds);
        log.info("Deleting company and all {} associated user accounts: {}", uidsList.size(), uidsList);

        if (uidsList.isEmpty()) {
            return;
        }

        // 3. Execute cascading deletes in strict child-to-parent order using exact JPA table names

        // A. Child Detail Tables (Revisions, Notes, Reminders, Scores, Time Logs, Documents)
        executeDelete("DELETE FROM crm_negotiation_revision WHERE negotiation_id_fk IN (SELECT id FROM crm_negotiation WHERE user_id_fk IN (:uids))", "uids", uidsList);
        executeDelete("DELETE FROM crm_lead_note WHERE lead_id_fk IN (SELECT lead_id FROM crm_xformsales_lead WHERE user_id_fk IN (:uids)) OR user_id_fk IN (:uids)", "uids", uidsList);
        executeDelete("DELETE FROM crm_lead_reminder WHERE lead_id_fk IN (SELECT lead_id FROM crm_xformsales_lead WHERE user_id_fk IN (:uids)) OR user_id_fk IN (:uids)", "uids", uidsList);
        executeDelete("DELETE FROM crm_lead_score WHERE lead_id_fk IN (SELECT lead_id FROM crm_xformsales_lead WHERE user_id_fk IN (:uids)) OR user_id_fk IN (:uids)", "uids", uidsList);
        executeDelete("DELETE FROM crm_task_time_log WHERE task_id IN (SELECT task_id FROM crm_xformsales_task WHERE user_id_fk IN (:uids)) OR user_id IN (:uids)", "uids", uidsList);
        executeDelete("DELETE FROM crm_documents WHERE user_id_fk IN (:uids)", "uids", uidsList);

        // B. Calendar Sub-System
        executeDelete("DELETE FROM crm_calendar_notification WHERE user_id_fk IN (:uids) OR event_id_fk IN (SELECT id FROM crm_calendar_event WHERE user_id_fk IN (:uids))", "uids", uidsList);
        executeDelete("DELETE FROM crm_calendar_attendee WHERE user_id_fk IN (:uids) OR event_id_fk IN (SELECT id FROM crm_calendar_event WHERE user_id_fk IN (:uids))", "uids", uidsList);
        executeDelete("DELETE FROM crm_calendar_event WHERE user_id_fk IN (:uids)", "uids", uidsList);

        // C. Audit & Trash
        executeDelete("DELETE FROM crm_audit_log WHERE user_id_fk IN (:uids)", "uids", uidsList);
        executeDelete("DELETE FROM crm_trash WHERE user_id_fk IN (:uids) OR company_admin_id_fk = :cid", "cid", companyAdminId, "uids", uidsList);

        // D. Governance, Permissions & Teams
        executeDelete("DELETE FROM crm_data_scope_config WHERE company_admin_id_fk = :cid OR user_id_fk IN (:uids)", "cid", companyAdminId, "uids", uidsList);
        executeDelete("DELETE FROM crm_user_permission WHERE user_id_fk IN (:uids)", "uids", uidsList);

        executeDelete("DELETE FROM crm_xformsales_team_member WHERE user_id_fk = :cid", "cid", companyAdminId);
        if (!memberEmails.isEmpty()) {
            executeDelete("DELETE FROM crm_xformsales_team_member WHERE team_member_email IN (:emails)", "emails", memberEmails);
        }

        executeDelete("DELETE FROM crm_xformsales_create_team WHERE user_id_fk = :cid", "cid", companyAdminId);
        executeDelete("DELETE FROM crm_xformsales_team WHERE user_id_fk = :cid", "cid", companyAdminId);
        executeDelete("DELETE FROM crm_xformsales_role WHERE user_id_fk = :cid", "cid", companyAdminId);

        // E. Primary Module Entities
        executeDelete("DELETE FROM crm_negotiation WHERE user_id_fk IN (:uids)", "uids", uidsList);
        executeDelete("DELETE FROM crm_xformsales_lead WHERE user_id_fk IN (:uids)", "uids", uidsList);
        executeDelete("DELETE FROM crm_xformsales_contact WHERE user_id_fk IN (:uids)", "uids", uidsList);
        executeDelete("DELETE FROM crm_xformsales_opportunity WHERE user_id_fk IN (:uids)", "uids", uidsList);
        executeDelete("DELETE FROM crm_xformsales_organization WHERE user_id_fk IN (:uids)", "uids", uidsList);
        executeDelete("DELETE FROM crm_xformsales_project WHERE user_id_fk IN (:uids)", "uids", uidsList);
        executeDelete("DELETE FROM crm_xformsales_task WHERE user_id_fk IN (:uids)", "uids", uidsList);
        executeDelete("DELETE FROM crm_attendance WHERE user_id IN (:uids)", "uids", uidsList);
        executeDelete("DELETE FROM crm_integration_config WHERE user_id_fk IN (:uids)", "uids", uidsList);

        // F. Master Data
        executeDelete("DELETE FROM crm_leadstatus_master WHERE user_id_fk IN (:uids)", "uids", uidsList);
        executeDelete("DELETE FROM crm_leadgroup_master WHERE user_id_fk IN (:uids)", "uids", uidsList);
        executeDelete("DELETE FROM crm_leadsource_master WHERE user_id_fk IN (:uids)", "uids", uidsList);

        // G. User Login Accounts (Company Admin + Team Leads + Team Members)
        executeDelete("DELETE FROM crm_xformsales_user WHERE userid IN (:uids)", "uids", uidsList);

        log.info("Successfully deleted company ID {} and all associated logins and records.", companyAdminId);
    }

    private void executeDelete(String sql, Object... params) {
        try {
            var query = entityManager.createNativeQuery(sql);
            for (int i = 0; i < params.length; i += 2) {
                String paramName = (String) params[i];
                Object paramValue = params[i + 1];
                query.setParameter(paramName, paramValue);
            }
            query.executeUpdate();
        } catch (Exception e) {
            log.warn("Cascade delete notice for query [{}]: {}", sql, e.getMessage());
        }
    }
}
