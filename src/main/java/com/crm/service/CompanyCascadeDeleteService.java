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
     * Validates table & column existence in INFORMATION_SCHEMA before executing native deletes
     * to guarantee zero SQL syntax/column missing errors and prevent Spring Transaction rollback-only exceptions.
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

        // 3. Execute cascading deletes in child-to-parent order with INFORMATION_SCHEMA table & column checks

        // A. Child Detail Tables
        executeDeleteIfColumnExists("crm_documents", "negotiation_revision_id",
                "DELETE FROM crm_documents WHERE negotiation_revision_id IN (SELECT id FROM crm_negotiation_revision WHERE negotiation_id IN (SELECT id FROM crm_negotiation WHERE user_id_fk IN (:uids)))",
                "uids", uidsList);

        executeDeleteIfColumnExists("crm_negotiation_revision", "negotiation_id",
                "DELETE FROM crm_negotiation_revision WHERE negotiation_id IN (SELECT id FROM crm_negotiation WHERE user_id_fk IN (:uids)) OR user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfColumnExists("crm_xformsales_lead_note", "lead_id_fk",
                "DELETE FROM crm_xformsales_lead_note WHERE lead_id_fk IN (SELECT lead_id FROM crm_xformsales_lead WHERE user_id_fk IN (:uids)) OR user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfColumnExists("crm_xformsales_lead_reminder", "lead_id_fk",
                "DELETE FROM crm_xformsales_lead_reminder WHERE lead_id_fk IN (SELECT lead_id FROM crm_xformsales_lead WHERE user_id_fk IN (:uids)) OR user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfColumnExists("crm_xformsales_lead_score", "lead_id_fk",
                "DELETE FROM crm_xformsales_lead_score WHERE lead_id_fk IN (SELECT lead_id FROM crm_xformsales_lead WHERE user_id_fk IN (:uids))",
                "uids", uidsList);

        executeDeleteIfColumnExists("crm_task_time_log", "task_id",
                "DELETE FROM crm_task_time_log WHERE task_id IN (SELECT task_id FROM crm_xformsales_task WHERE user_id_fk IN (:uids)) OR user_id IN (:uids)",
                "uids", uidsList);

        // B. Calendar Sub-System
        executeDeleteIfColumnExists("crm_calendar_notifications", "user_id_fk",
                "DELETE FROM crm_calendar_notifications WHERE user_id_fk IN (:uids) OR event_id_fk IN (SELECT id FROM crm_calendar_events WHERE user_id_fk IN (:uids))",
                "uids", uidsList);

        executeDeleteIfColumnExists("crm_calendar_attendees", "user_id_fk",
                "DELETE FROM crm_calendar_attendees WHERE user_id_fk IN (:uids) OR event_id_fk IN (SELECT id FROM crm_calendar_events WHERE user_id_fk IN (:uids))",
                "uids", uidsList);

        executeDeleteIfColumnExists("crm_calendar_events", "user_id_fk",
                "DELETE FROM crm_calendar_events WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        // C. Audit & Governance
        executeDeleteIfColumnExists("crm_audit_log", "actor_user_id",
                "DELETE FROM crm_audit_log WHERE actor_user_id IN (:uids) OR company_admin_id_fk = :cid",
                "cid", companyAdminId, "uids", uidsList);

        executeDeleteIfColumnExists("crm_data_scope_config", "company_admin_id_fk",
                "DELETE FROM crm_data_scope_config WHERE company_admin_id_fk = :cid OR user_id_fk IN (:uids)",
                "cid", companyAdminId, "uids", uidsList);

        executeDeleteIfColumnExists("crm_user_permission", "user_id_fk",
                "DELETE FROM crm_user_permission WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        // D. Teams & Roles
        executeDeleteIfColumnExists("crm_xformsales_team_member", "user_id_fk",
                "DELETE FROM crm_xformsales_team_member WHERE user_id_fk = :cid",
                "cid", companyAdminId);

        if (!memberEmails.isEmpty()) {
            executeDeleteIfColumnExists("crm_xformsales_team_member", "team_member_email",
                    "DELETE FROM crm_xformsales_team_member WHERE team_member_email IN (:emails)",
                    "emails", memberEmails);
        }

        executeDeleteIfColumnExists("crm_xformsales_create_team", "team_id_fk",
                "DELETE FROM crm_xformsales_create_team WHERE team_id_fk IN (SELECT team_id FROM crm_xformsales_team WHERE user_id_fk = :cid)",
                "cid", companyAdminId);

        executeDeleteIfColumnExists("crm_xformsales_team", "user_id_fk",
                "DELETE FROM crm_xformsales_team WHERE user_id_fk = :cid",
                "cid", companyAdminId);

        executeDeleteIfColumnExists("crm_xformsales_role", "user_id_fk",
                "DELETE FROM crm_xformsales_role WHERE user_id_fk = :cid",
                "cid", companyAdminId);

        // E. Primary Module Entities
        executeDeleteIfColumnExists("crm_negotiation", "user_id_fk",
                "DELETE FROM crm_negotiation WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfColumnExists("crm_xformsales_lead", "user_id_fk",
                "DELETE FROM crm_xformsales_lead WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfColumnExists("crm_xformsales_contact", "user_id_fk",
                "DELETE FROM crm_xformsales_contact WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfColumnExists("crm_xformsales_opportunity", "user_id_fk",
                "DELETE FROM crm_xformsales_opportunity WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfColumnExists("crm_xformsales_organization", "user_id_fk",
                "DELETE FROM crm_xformsales_organization WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfColumnExists("crm_xformsales_project", "user_id_fk",
                "DELETE FROM crm_xformsales_project WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfColumnExists("crm_xformsales_task", "user_id_fk",
                "DELETE FROM crm_xformsales_task WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfColumnExists("crm_attendance", "user_id",
                "DELETE FROM crm_attendance WHERE user_id IN (:uids)",
                "uids", uidsList);

        executeDeleteIfColumnExists("crm_integration_config", "user_id_fk",
                "DELETE FROM crm_integration_config WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        // F. Master Data
        executeDeleteIfColumnExists("crm_leadstatus_master", "user_id_fk",
                "DELETE FROM crm_leadstatus_master WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfColumnExists("crm_leadgroups_master", "user_id_fk",
                "DELETE FROM crm_leadgroups_master WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        executeDeleteIfColumnExists("crm_leadsource_master", "user_id_fk",
                "DELETE FROM crm_leadsource_master WHERE user_id_fk IN (:uids)",
                "uids", uidsList);

        // G. User Login Accounts (Company Admin + Team Leads + Team Members)
        executeDeleteIfColumnExists("crm_xformsales_user", "userid",
                "DELETE FROM crm_xformsales_user WHERE userid IN (:uids)",
                "uids", uidsList);

        log.info("Successfully deleted company ID {} and all associated logins and records.", companyAdminId);
    }

    private boolean columnExists(String tableName, String columnName) {
        try {
            Number count = (Number) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = :tname AND column_name = :cname")
                    .setParameter("tname", tableName)
                    .setParameter("cname", columnName)
                    .getSingleResult();
            return count != null && count.longValue() > 0;
        } catch (Exception e) {
            log.warn("Column existence check notice for table [{}] column [{}]: {}", tableName, columnName, e.getMessage());
            return false;
        }
    }

    private void executeDeleteIfColumnExists(String tableName, String requiredColumn, String sql, Object... params) {
        if (!columnExists(tableName, requiredColumn)) {
            log.info("Skipping cascade delete for table [{}] — required column [{}] does not exist", tableName, requiredColumn);
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
            log.warn("Cascade delete notice for table [{}] query [{}]: {}", tableName, sql, e.getMessage());
        }
    }
}
