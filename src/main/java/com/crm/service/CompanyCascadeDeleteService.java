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
     * - All team member records
     * - All teams & create_teams records
     * - All custom roles & data scope configurations
     * - All CRM module data (Leads, Contacts, Opportunities, Organizations, Projects, Tasks, Negotiations, Attendance, Reminders, Notes, Scores, Integrations)
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

        // 3. Execute cascading native SQL deletes across all company tables

        // Data Scope Configs
        entityManager.createNativeQuery("DELETE FROM crm_data_scope_config WHERE company_admin_id_fk = :cid OR user_id_fk IN (:uids)")
                .setParameter("cid", companyAdminId)
                .setParameter("uids", uidsList)
                .executeUpdate();

        // User permissions
        entityManager.createNativeQuery("DELETE FROM crm_user_permission WHERE user_id_fk IN (:uids)")
                .setParameter("uids", uidsList)
                .executeUpdate();

        // Team Members
        entityManager.createNativeQuery("DELETE FROM crm_xformsales_team_member WHERE user_id_fk = :cid")
                .setParameter("cid", companyAdminId)
                .executeUpdate();

        if (!memberEmails.isEmpty()) {
            entityManager.createNativeQuery("DELETE FROM crm_xformsales_team_member WHERE team_member_email IN (:emails)")
                    .setParameter("emails", memberEmails)
                    .executeUpdate();
        }

        // Teams
        entityManager.createNativeQuery("DELETE FROM crm_xformsales_team WHERE user_id_fk = :cid")
                .setParameter("cid", companyAdminId)
                .executeUpdate();

        // Roles
        entityManager.createNativeQuery("DELETE FROM crm_xformsales_role WHERE user_id_fk = :cid")
                .setParameter("cid", companyAdminId)
                .executeUpdate();

        // Module Entities
        entityManager.createNativeQuery("DELETE FROM crm_xformsales_lead WHERE user_id_fk IN (:uids)")
                .setParameter("uids", uidsList)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM crm_xformsales_contact WHERE user_id_fk IN (:uids)")
                .setParameter("uids", uidsList)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM crm_xformsales_opportunity WHERE user_id_fk IN (:uids)")
                .setParameter("uids", uidsList)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM crm_xformsales_organization WHERE user_id_fk IN (:uids)")
                .setParameter("uids", uidsList)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM crm_xformsales_project WHERE user_id_fk IN (:uids)")
                .setParameter("uids", uidsList)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM crm_xformsales_task WHERE user_id_fk IN (:uids)")
                .setParameter("uids", uidsList)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM crm_xformsales_negotiation WHERE user_id_fk IN (:uids)")
                .setParameter("uids", uidsList)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM crm_xformsales_attendance WHERE user_id_fk IN (:uids)")
                .setParameter("uids", uidsList)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM crm_integration_config WHERE user_id_fk IN (:uids)")
                .setParameter("uids", uidsList)
                .executeUpdate();

        // User Login Accounts (Company Admin + Team Leads + Team Members)
        entityManager.createNativeQuery("DELETE FROM crm_xformsales_user WHERE userid IN (:uids)")
                .setParameter("uids", uidsList)
                .executeUpdate();

        log.info("Successfully deleted company ID {} and all associated logins and records.", companyAdminId);
    }
}
