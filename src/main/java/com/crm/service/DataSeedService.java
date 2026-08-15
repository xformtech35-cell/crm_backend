package com.crm.service;

import com.crm.entity.DataScopeConfig;
import com.crm.entity.Role;
import com.crm.entity.TeamMember;
import com.crm.repository.CreateTeamRepository;
import com.crm.repository.DataScopeConfigRepository;
import com.crm.repository.RoleRepository;
import com.crm.repository.TeamMemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Runs once on application startup (after Hibernate DDL completes) to:
 *  1. Capture pre-seed row counts (Part 0.3)
 *  2. Mark ADMIN and Team Lead roles as is_system_role = true (idempotent)
 *  3. Seed DEFAULT scope rows for Admin/Team Lead/other roles if missing (idempotent)
 *  4. Back-populate TeamMember.team_id_fk from crm_xformsales_create_team (idempotent)
 *  5. Verify row counts did not decrease — abort startup if they did (Part 0.3)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataSeedService {

    private final RoleRepository roleRepository;
    private final DataScopeConfigRepository dataScopeConfigRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CreateTeamRepository createTeamRepository;
    private final LeadService leadService;

    @PersistenceContext
    private EntityManager em;

    private static final List<String> BUSINESS_MODULES = List.of(
        "LEADS", "OPPORTUNITIES", "TASKS", "PROJECTS", "CONTACTS", "ORGANIZATIONS"
    );

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void runSeed() {
        log.info("=== DataSeedService starting ===");

        // Step 1: Capture pre-seed row counts
        Map<String, Long> before = captureRowCounts();
        log.info("Row counts BEFORE seed: {}", before);

        try {
            // Step 2: Mark system roles
            seedSystemRoleFlags();

            // Step 3: Seed default scope rows
            seedDefaultScopeRows();

            // Step 4: Back-populate TeamMember.team_id_fk
            backPopulateTeamIdFk();

            // Step 5: Sync all historical Leads to Contacts, Organizations, and Opportunities
            leadService.syncAllExistingLeadsToEntities();

        } catch (Exception e) {
            log.error("DataSeedService error during seed operations: {}", e.getMessage(), e);
            throw e; // Re-throw so startup is aware
        }

        // Step 5: Verify row counts
        Map<String, Long> after = captureRowCounts();
        log.info("Row counts AFTER seed: {}", after);
        verifyRowCounts(before, after);

        log.info("=== DataSeedService completed successfully ===");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Step 2 — Mark system roles (idempotent)
    // ─────────────────────────────────────────────────────────────────────────
    private void seedSystemRoleFlags() {
        List<Role> allRoles = roleRepository.findAll();
        int updated = 0;
        for (Role role : allRoles) {
            boolean shouldBeSystem = isSystemRoleName(role.getRoleName());
            if (shouldBeSystem && !Boolean.TRUE.equals(role.getIsSystemRole())) {
                role.setIsSystemRole(true);
                if (role.getRoleLevel() == null) {
                    role.setRoleLevel("ADMIN".equalsIgnoreCase(role.getRoleName()) ? 1 : 2);
                }
                roleRepository.save(role);
                updated++;
            }
        }
        log.info("System role flags seeded/confirmed for {} roles", updated);
    }

    private boolean isSystemRoleName(String name) {
        if (name == null) return false;
        return "ADMIN".equalsIgnoreCase(name)
            || "Team Lead".equalsIgnoreCase(name)
            || "TEAM_LEAD".equalsIgnoreCase(name);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Step 3 — Seed default scope rows (idempotent — find-or-create per Part 0.9)
    // ─────────────────────────────────────────────────────────────────────────
    private void seedDefaultScopeRows() {
        List<Role> allRoles = roleRepository.findAll();
        int seeded = 0;
        for (Role role : allRoles) {
            String defaultScope = resolveDefaultScope(role);
            Long companyAdminId = role.getUserIdFk(); // null for global system roles
            for (String module : BUSINESS_MODULES) {
                // Find existing row using the repository's existing method
                Optional<DataScopeConfig> existing = companyAdminId != null
                    ? dataScopeConfigRepository.findByCompanyAdminIdFkAndRoleIdFkAndModuleName(companyAdminId, role.getRoleId(), module)
                    : dataScopeConfigRepository.findByRoleIdFkAndModuleName(role.getRoleId(), module);

                if (existing.isEmpty()) {
                    dataScopeConfigRepository.save(DataScopeConfig.builder()
                        .companyAdminIdFk(companyAdminId)
                        .roleIdFk(role.getRoleId())
                        .moduleName(module)
                        .scopeMode(defaultScope)
                        .build());
                    seeded++;
                }
            }
        }
        log.info("Scope rows seeded (new rows created): {}", seeded);
    }

    private String resolveDefaultScope(Role role) {
        if (role.getRoleName() == null) return "OWN_DATA_ONLY";
        String name = role.getRoleName();
        if ("ADMIN".equalsIgnoreCase(name)) return "ALL_DATA";
        if ("Team Lead".equalsIgnoreCase(name) || "TEAM_LEAD".equalsIgnoreCase(name)) return "TEAM_DATA";
        return "OWN_DATA_ONLY";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Step 4 — Back-populate TeamMember.team_id_fk (read-only from create_team, Part 0.5)
    // ─────────────────────────────────────────────────────────────────────────
    private void backPopulateTeamIdFk() {
        int updated = 0;
        var ctList = createTeamRepository.findAll();
        for (var ct : ctList) {
            if (ct.getTeamMemberIdFk() == null || ct.getTeamIdFk() == null) continue;
            Optional<TeamMember> tmOpt = teamMemberRepository.findById(ct.getTeamMemberIdFk());
            if (tmOpt.isPresent()) {
                TeamMember tm = tmOpt.get();
                if (tm.getTeamIdFk() == null) {
                    tm.setTeamIdFk(ct.getTeamIdFk());
                    teamMemberRepository.save(tm);
                    updated++;
                }
            }
        }
        log.info("TeamMember.team_id_fk back-populated for {} members", updated);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Row-count verification (Part 0.3)
    // ─────────────────────────────────────────────────────────────────────────
    private Map<String, Long> captureRowCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("crm_xformsales_role",        queryCount("SELECT COUNT(*) FROM crm_xformsales_role"));
        counts.put("crm_xformsales_team_member",  queryCount("SELECT COUNT(*) FROM crm_xformsales_team_member"));
        counts.put("crm_data_scope_config",       queryCount("SELECT COUNT(*) FROM crm_data_scope_config"));
        counts.put("crm_user_permission",         queryCount("SELECT COUNT(*) FROM crm_user_permission"));
        counts.put("crm_xformsales_permission",   queryCount("SELECT COUNT(*) FROM crm_xformsales_permission"));
        counts.put("crm_xformsales_create_team",  queryCount("SELECT COUNT(*) FROM crm_xformsales_create_team"));
        return counts;
    }

    private long queryCount(String sql) {
        try {
            Object result = em.createNativeQuery(sql).getSingleResult();
            return ((Number) result).longValue();
        } catch (Exception e) {
            log.warn("Row count query failed for '{}': {}", sql, e.getMessage());
            return -1L; // -1 signals unknown; will not trigger false abort
        }
    }

    private void verifyRowCounts(Map<String, Long> before, Map<String, Long> after) {
        for (String table : before.keySet()) {
            long beforeCount = before.get(table);
            long afterCount  = after.getOrDefault(table, -1L);
            if (beforeCount >= 0 && afterCount >= 0 && afterCount < beforeCount) {
                String msg = String.format(
                    "DATA SAFETY VIOLATION: table '%s' row count decreased from %d to %d. " +
                    "Aborting to prevent data loss.", table, beforeCount, afterCount);
                log.error(msg);
                throw new IllegalStateException(msg);
            }
        }
        log.info("Row-count verification PASSED.");
    }
}
