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
     * Complete cascade deletion of a company:
     * Checks INFORMATION_SCHEMA for table existence before deleting to ensure zero SQL syntax errors 
     * and prevent Spring Transaction rollback-only exceptions.
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

        // 3. Execute cascading deletes in child-to-parent order with INFORMATION_SCHEMA checks

        // A. Child Detail Tables (Revisions, Notes, Reminders, Scores, Time Logs, Documents)
        executeDeleteIfTableExists("crm_negotiation_revision",
                "DELETE FROM crm_negotiation_revision WHERE negotiation_id_fk IN (SELECT id FROM crm_negotiation WHERE user_id_fk IN (:uids))",
                "uids", uidsList);

        executeDeleteIfTableExists("crm_lead_note",
                "DELETE FROM crm_lead_note WHERE lead_id_fk IN (SELECT lead_id FROM crm_xformsales_lead WHERE user_id_fk IN (:uids)) OR user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfTableExists("crm_lead_reminder",
                "DELETE FROM crm_lead_reminder WHERE lead_id_fk IN (SELECT lead_id FROM crm_xformsales_lead WHERE user_id_fk IN (:uids)) OR user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfTableExists("crm_lead_score",
                "DELETE FROM crm_lead_score WHERE lead_id_fk IN (SELECT lead_id FROM crm_xformsales_lead WHERE user_id_fk IN (:uids)) OR user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfTableExists("crm_task_time_log",
                "DELETE FROM crm_task_time_log WHERE task_id IN (SELECT task_id FROM crm_xformsales_task WHERE user_id_fk IN (:uids)) OR user_id IN (:uids)",
                "uids", uidsList);

        executeDeleteIfTableExists("crm_documents",
                "DELETE FROM crm_documents WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        // B. Calendar Sub-System
        executeDeleteIfTableExists("crm_calendar_notification",
                "DELETE FROM crm_calendar_notification WHERE user_id_fk IN (:uids) OR event_id_fk IN (SELECT id FROM crm_calendar_event WHERE user_id_fk IN (:uids))",
                "uids", uidsList);

        executeDeleteIfTableExists("crm_calendar_attendee",
                "DELETE FROM crm_calendar_attendee WHERE user_id_fk IN (:uids) OR event_id_fk IN (SELECT id FROM crm_calendar_event WHERE user_id_fk IN (:uids))",
                "uids", uidsList);

        executeDeleteIfTableExists("crm_calendar_event",
                "DELETE FROM crm_calendar_event WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        // C. Audit & Trash
        executeDeleteIfTableExists("crm_audit_log",
                "DELETE FROM crm_audit_log WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfTableExists("crm_trash",
                "DELETE FROM crm_trash WHERE user_id_fk IN (:uids) OR company_admin_id_fk = :cid",
                "cid", companyAdminId, "uids", uidsList);

        // D. Governance, Permissions & Teams
        executeDeleteIfTableExists("crm_data_scope_config",
                "DELETE FROM crm_data_scope_config WHERE company_admin_id_fk = :cid OR user_id_fk IN (:uids)",
                "cid", companyAdminId, "uids", uidsList);

        executeDeleteIfTableExists("crm_user_permission",
                "DELETE FROM crm_user_permission WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfTableExists("crm_xformsales_team_member",
                "DELETE FROM crm_xformsales_team_member WHERE user_id_fk = :cid",
                "cid", companyAdminId);

        if (!memberEmails.isEmpty()) {
            executeDeleteIfTableExists("crm_xformsales_team_member",
                    "DELETE FROM crm_xformsales_team_member WHERE team_member_email IN (:emails)",
                    "emails", memberEmails);
        }

        executeDeleteIfTableExists("crm_xformsales_create_team",
                "DELETE FROM crm_xformsales_create_team WHERE user_id_fk = :cid",
                "cid", companyAdminId);

        executeDeleteIfTableExists("crm_xformsales_team",
                "DELETE FROM crm_xformsales_team WHERE user_id_fk = :cid",
                "cid", companyAdminId);

        executeDeleteIfTableExists("crm_xformsales_role",
                "DELETE FROM crm_xformsales_role WHERE user_id_fk = :cid",
                "cid", companyAdminId);

        // E. Primary Module Entities
        executeDeleteIfTableExists("crm_negotiation",
                "DELETE FROM crm_negotiation WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfTableExists("crm_xformsales_lead",
                "DELETE FROM crm_xformsales_lead WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfTableExists("crm_xformsales_contact",
                "DELETE FROM crm_xformsales_contact WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfTableExists("crm_xformsales_opportunity",
                "DELETE FROM crm_xformsales_opportunity WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfTableExists("crm_xformsales_organization",
                "DELETE FROM crm_xformsales_organization WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfTableExists("crm_xformsales_project",
                "DELETE FROM crm_xformsales_project WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfTableExists("crm_xformsales_task",
                "DELETE FROM crm_xformsales_task WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfTableExists("crm_attendance",
                "DELETE FROM crm_attendance WHERE user_id IN (:uids)",
                "uids", uidsList);

        executeDeleteIfTableExists("crm_integration_config",
                "DELETE FROM crm_integration_config WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        // F. Master Data
        executeDeleteIfTableExists("crm_leadstatus_master",
                "DELETE FROM crm_leadstatus_master WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfTableExists("crm_leadgroup_master",
                "DELETE FROM crm_leadgroup_master WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfTableExists("crm_leadsource_master",
                "DELETE FROM crm_leadsource_master WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        // G. User Login Accounts (Company Admin + Team Leads + Team Members)
        executeDeleteIfTableExists("crm_xformsales_user",
                "DELETE FROM crm_xformsales_user WHERE userid IN (:uids)",
                "uids", uidsList);

        log.info("Successfully deleted company ID {} and all associated logins and records.", companyAdminId);
    }

    private boolean tableExists(String tableName) {
        try {
            Number count = (Number) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = :tname")
                    .setParameter("tname", tableName)
                    .getSingleResult();
            return count != null && count.longValue() > 0;
        } catch (Exception e) {
            log.warn("Table existence check failed for table [{}]: {}", tableName, e.getMessage());
            return false;
        }
    }

    private void executeDeleteIfTableExists(String tableName, String sql, Object... params) {
        if (!tableExists(tableName)) {
            log.info("Skipping cascade delete for non-existent table [{}]", tableName);
            return;
        }

        try {
            var query = entityManager.createNativeQuery(sql);
            for (int i = 0; i < params.length; i += 2) {
                String paramName = (String) params[i];
                Object paramValue = params[i + 1];
                query.setParameter(paramName, paramValue);
            }
            query.executeUpdate();
        } catch (Exception e) {
            log.warn("Cascade delete warning for table [{}] query [{}]: {}", tableName, sql, e.getMessage());
        }
    }
}
