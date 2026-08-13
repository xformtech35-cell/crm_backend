package com.crm.config;

import com.crm.entity.*;
import com.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Auto-provisions "UWS Enviro-tech Private Limited" company on WAR startup.
 * Creates company admin, teams (Dosing & WTP), team leads, members, roles,
 * permissions, data scopes, and imports 299 leads from bundled Excel file.
 * Runs only once - skips if admin@uwsenviro.com already exists.
 */
@Component
@RequiredArgsConstructor
@Order(2) // Run after DemoAuthBootstrap (default order)
public class UwsEnviroBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CreateTeamRepository createTeamRepository;
    private final LeadRepository leadRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    private static final String ADMIN_EMAIL = "admin@uwsenviro.com";
    private static final String PASSWORD = "Admin@123";

    private static final List<String> ALL_PERMISSIONS = List.of(
        "dashboard.view", "roles.view", "settings.view",
        "leads.view", "leads.create", "leads.edit", "leads.delete", "leads.import",
        "opportunities.view", "opportunities.create", "opportunities.edit", "opportunities.delete",
        "projects.view", "projects.create", "projects.edit", "projects.delete",
        "tasks.view", "tasks.create", "tasks.edit", "tasks.delete",
        "contacts.view", "contacts.create", "contacts.edit", "contacts.delete",
        "organizations.view", "organizations.create", "organizations.edit", "organizations.delete",
        "teams.view", "teams.create", "teams.edit", "teams.delete",
        "users.view", "users.create", "users.edit", "users.delete",
        "reports.view", "calendar.view", "calendar.create", "calendar.edit", "calendar.delete",
        "attendance.view", "attendance.edit", "integrations.view", "integrations.edit",
        "companies.view", "companies.create", "companies.edit", "companies.delete", "audit.view",
        "activities.view", "emails.view", "analytics.view", "automation.view",
        "trash.view", "trash.restore", "trash.delete", "data_access.view", "data_access.edit"
    );

    @Override
    public void run(String... args) {
        // Skip if company already provisioned
        if (userRepository.findByUserEmail(ADMIN_EMAIL).isPresent()) {
            System.out.println("[UWS Bootstrap] UWS Enviro-tech already provisioned. Skipping.");
            return;
        }
        System.out.println("[UWS Bootstrap] =========================================================");
        System.out.println("[UWS Bootstrap] AUTO-PROVISIONING 'UWS Enviro-tech Private Limited'");
        System.out.println("[UWS Bootstrap] =========================================================");
        try {
            provision();
        } catch (Exception e) {
            System.err.println("[UWS Bootstrap] ERROR during provisioning: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public void provision() throws Exception {
        String hashedPassword = passwordEncoder.encode(PASSWORD);

        // 1. Create Company Admin User
        User adminUser = User.builder()
                .username("UWS Enviro-tech Private Limited")
                .userEmail(ADMIN_EMAIL)
                .password(hashedPassword)
                .role("ADMIN")
                .createdDate(LocalDate.now())
                .integrationsAccess(true)
                .planName("Enterprise Premium Plan")
                .planPrice("$199/month")
                .planValidity(LocalDate.now().plusYears(1))
                .subscriptionStatus("Active")
                .build();
        adminUser = userRepository.save(adminUser);
        Long uwsAdminId = adminUser.getUserid();
        System.out.println("[UWS Bootstrap] Created Admin User ID: " + uwsAdminId);

        // 2. Create Roles
        Long adminRoleId = ensureRole("ADMIN", uwsAdminId, true, 1);
        Long teamLeadRoleId = ensureRole("Team Lead", uwsAdminId, true, 2);
        Long salesExecRoleId = ensureRole("Sales Executive", uwsAdminId, false, 2);

        // 3. Grant 59 permissions to ADMIN role
        for (String perm : ALL_PERMISSIONS) {
            if (!permissionRepository.existsByRoleIdFkAndGrpPerm(adminRoleId, perm)) {
                permissionRepository.save(Permission.builder()
                        .roleIdFk(adminRoleId)
                        .grpPerm(perm)
                        .build());
            }
        }
        System.out.println("[UWS Bootstrap] Seeded " + ALL_PERMISSIONS.size() + " permissions for ADMIN role");

        // 4. Create Team Lead & Member User Accounts
        User mukundUser = createUser("Mukund Jadhav (Team Lead - Dosing)", "teamlead.dosing@uwsenviro.com", hashedPassword, "Team Lead");
        User dosingExecUser = createUser("VG (Sales Executive - Dosing)", "member.dosing@uwsenviro.com", hashedPassword, "Sales Executive");
        User poojaUser = createUser("Pooja Mandmule (Team Lead - WTP)", "teamlead.wtp@uwsenviro.com", hashedPassword, "Team Lead");
        User wtpExecUser = createUser("ANUJ (Sales Executive - WTP)", "member.wtp@uwsenviro.com", hashedPassword, "Sales Executive");

        // 5. Create Department Teams
        Long dosingTeamId = ensureTeam("Dosing Department", uwsAdminId);
        Long wtpTeamId = ensureTeam("WTP Department", uwsAdminId);

        // 6. Create Team Members
        Long mukundTmId = ensureTeamMember("Mukund Jadhav", "teamlead.dosing@uwsenviro.com", uwsAdminId, dosingTeamId, String.valueOf(teamLeadRoleId), null);
        Long dosingExecTmId = ensureTeamMember("VG (Sales Executive - Dosing)", "member.dosing@uwsenviro.com", uwsAdminId, dosingTeamId, String.valueOf(salesExecRoleId), mukundTmId);
        Long poojaTmId = ensureTeamMember("Pooja Mandmule", "teamlead.wtp@uwsenviro.com", uwsAdminId, wtpTeamId, String.valueOf(teamLeadRoleId), null);
        Long wtpExecTmId = ensureTeamMember("ANUJ (Sales Executive - WTP)", "member.wtp@uwsenviro.com", uwsAdminId, wtpTeamId, String.valueOf(salesExecRoleId), poojaTmId);

        // 7. Set Team Leads
        jdbcTemplate.update("UPDATE crm_xformsales_team SET team_lead_id_fk = ? WHERE team_id = ?", mukundTmId, dosingTeamId);
        jdbcTemplate.update("UPDATE crm_xformsales_team SET team_lead_id_fk = ? WHERE team_id = ?", poojaTmId, wtpTeamId);

        // 8. Create Team Mappings
        ensureCreateTeamMapping(dosingTeamId, mukundTmId, teamLeadRoleId, uwsAdminId);
        ensureCreateTeamMapping(dosingTeamId, dosingExecTmId, salesExecRoleId, uwsAdminId);
        ensureCreateTeamMapping(wtpTeamId, poojaTmId, teamLeadRoleId, uwsAdminId);
        ensureCreateTeamMapping(wtpTeamId, wtpExecTmId, salesExecRoleId, uwsAdminId);

        System.out.println("[UWS Bootstrap] Dosing Team ID: " + dosingTeamId + " (Lead TM: " + mukundTmId + ")");
        System.out.println("[UWS Bootstrap] WTP Team ID   : " + wtpTeamId + " (Lead TM: " + poojaTmId + ")");

        // 9. Configure Data Scopes
        seedDataScopes(uwsAdminId, teamLeadRoleId, salesExecRoleId);

        // 10. Import Excel Leads
        importExcelLeads(uwsAdminId, dosingTeamId, dosingExecTmId, wtpTeamId, wtpExecTmId);

        System.out.println("[UWS Bootstrap] =========================================================");
        System.out.println("[UWS Bootstrap] UWS ENVIRO-TECH PRIVATE LIMITED - LIVE & READY!");
        System.out.println("[UWS Bootstrap] =========================================================");
    }

    private User createUser(String name, String email, String hashedPass, String role) {
        return userRepository.findByUserEmail(email).orElseGet(() ->
            userRepository.save(User.builder()
                .username(name)
                .userEmail(email)
                .password(hashedPass)
                .role(role)
                .createdDate(LocalDate.now())
                .build())
        );
    }

    private Long ensureRole(String name, Long companyAdminId, boolean isSystem, int level) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT role_id FROM crm_xformsales_role WHERE role_name = ? AND user_id_fk = ?", name, companyAdminId);
            if (!rows.isEmpty()) return ((Number) rows.get(0).get("role_id")).longValue();
        } catch (Exception ignored) {}

        jdbcTemplate.update(
            "INSERT INTO crm_xformsales_role (role_name, user_id_fk, is_system_role, role_level) VALUES (?, ?, ?, ?)",
            name, companyAdminId, isSystem, level);

        return jdbcTemplate.queryForObject(
            "SELECT role_id FROM crm_xformsales_role WHERE role_name = ? AND user_id_fk = ?",
            Long.class, name, companyAdminId);
    }

    private Long ensureTeam(String teamName, Long companyAdminId) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT team_id FROM crm_xformsales_team WHERE team_name = ? AND user_id_fk = ?", teamName, companyAdminId);
            if (!rows.isEmpty()) return ((Number) rows.get(0).get("team_id")).longValue();
        } catch (Exception ignored) {}

        jdbcTemplate.update(
            "INSERT INTO crm_xformsales_team (team_name, user_id_fk, is_deleted) VALUES (?, ?, false)", teamName, companyAdminId);

        return jdbcTemplate.queryForObject(
            "SELECT team_id FROM crm_xformsales_team WHERE team_name = ? AND user_id_fk = ?",
            Long.class, teamName, companyAdminId);
    }

    private Long ensureTeamMember(String name, String email, Long companyAdminId, Long teamId, String roleStr, Long reportingTo) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT team_member_id FROM crm_xformsales_team_member WHERE team_member_email = ?", email);
            if (!rows.isEmpty()) return ((Number) rows.get(0).get("team_member_id")).longValue();
        } catch (Exception ignored) {}

        jdbcTemplate.update(
            "INSERT INTO crm_xformsales_team_member (team_member_name, team_member_email, user_id_fk, team_id_fk, team_member_role, reporting_to_fk, is_deleted) VALUES (?, ?, ?, ?, ?, ?, false)",
            name, email, companyAdminId, teamId, roleStr, reportingTo);

        return jdbcTemplate.queryForObject(
            "SELECT team_member_id FROM crm_xformsales_team_member WHERE team_member_email = ?",
            Long.class, email);
    }

    private void ensureCreateTeamMapping(Long teamId, Long tmId, Long roleId, Long companyAdminId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT create_team_id FROM crm_xformsales_create_team WHERE team_id_fk = ? AND team_member_id_fk = ?", teamId, tmId);
        if (rows.isEmpty()) {
            jdbcTemplate.update(
                "INSERT INTO crm_xformsales_create_team (team_id_fk, team_member_id_fk, role_id_fk, user_id_fk) VALUES (?, ?, ?, ?)",
                teamId, tmId, roleId, companyAdminId);
        }
    }

    private void seedDataScopes(Long companyAdminId, Long teamLeadRoleId, Long salesExecRoleId) {
        String[] modules = {"LEADS", "TASKS", "OPPORTUNITIES", "PROJECTS", "CONTACTS", "ORGANIZATIONS", "REPORTS", "NEGOTIATION"};
        for (String m : modules) {
            jdbcTemplate.update("INSERT INTO crm_data_scope_config (company_admin_id_fk, role_id_fk, module_name, scope_mode) VALUES (?, NULL, ?, 'ALL_DATA')", companyAdminId, m);
            jdbcTemplate.update("INSERT INTO crm_data_scope_config (company_admin_id_fk, role_id_fk, module_name, scope_mode) VALUES (?, ?, ?, 'TEAM_DATA')", companyAdminId, teamLeadRoleId, m);
            jdbcTemplate.update("INSERT INTO crm_data_scope_config (company_admin_id_fk, role_id_fk, module_name, scope_mode) VALUES (?, ?, ?, 'TEAM_DATA')", companyAdminId, salesExecRoleId, m);
        }
        System.out.println("[UWS Bootstrap] Seeded " + (modules.length * 3) + " data scope config rules");
    }

    private void importExcelLeads(Long companyAdminId, Long dosingTeamId, Long dosingMemberId, Long wtpTeamId, Long wtpMemberId) throws Exception {
        ClassPathResource resource = new ClassPathResource("uws_inquiry_data.xlsx");
        if (!resource.exists()) {
            System.out.println("[UWS Bootstrap] WARNING: uws_inquiry_data.xlsx not found in classpath. Skipping lead import.");
            return;
        }

        try (InputStream is = resource.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            // Sheet 1: Dosing Department
            Sheet dosingSheet = workbook.getSheet("Dosing Department ");
            if (dosingSheet == null) dosingSheet = workbook.getSheetAt(0);
            int dosingCount = importSheet(dosingSheet, companyAdminId, dosingTeamId, dosingMemberId, true);
            System.out.println("[UWS Bootstrap] Imported " + dosingCount + " Dosing Department leads");

            // Sheet 2: WTP Department
            Sheet wtpSheet = workbook.getSheet("WTP Department ");
            if (wtpSheet == null) wtpSheet = workbook.getSheetAt(1);
            int wtpCount = importSheet(wtpSheet, companyAdminId, wtpTeamId, wtpMemberId, false);
            System.out.println("[UWS Bootstrap] Imported " + wtpCount + " WTP Department leads");

            System.out.println("[UWS Bootstrap] TOTAL LEADS IMPORTED: " + (dosingCount + wtpCount));
        }
    }

    private int importSheet(Sheet sheet, Long companyAdminId, Long teamId, Long memberId, boolean isDosing) {
        int count = 0;
        String insertSql = "INSERT INTO crm_xformsales_lead (lead_ref, company_contact_person_name, lead_group, inquiry_date, lead_outcome_status, " +
            "enquiry_description, lead_organisation_name, lead_mobile_no, enquiry_status, quotation_number, " +
            "quotation_date, quotation_amount, lead_rating, remarks, user_id_fk, lead_assigned_team, " +
            "lead_assigned_member, lead_source, lead_created_date, created_by, is_deleted) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Direct', NOW(), 'Admin', false)";

        for (int r = 2; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String party, details, qtnNoStr;

            if (isDosing) {
                // Dosing: Col 0=Sr, 1=Ref, 2=Ref2, 3=Group, 4=Date, 5=Status, 6=Details, 7=Party, 8=Contact, 9=QtnStatus, 10=QtnNo, 11=QtnDate, 12=QtnAmt, 13=Imp, 14=FollowUp
                party = getCellString(row, 7);
                details = getCellString(row, 6);
                qtnNoStr = getCellString(row, 10);
            } else {
                // WTP: Col 0=Sr, 1=SubmittedBy, 2=Group, 3=Date, 4=Status, 5=Details, 6=Party, 7=Contact, 8=QtnStatus, 9=QtnNo, 10=QtnDate, 11=QtnAmt, 12=Imp, 13=FollowUp
                party = getCellString(row, 6);
                details = getCellString(row, 5);
                qtnNoStr = getCellString(row, 9);
            }

            if (isEmpty(party) && isEmpty(details) && isEmpty(qtnNoStr)) continue;

            String leadRef, contactPerson, group, statusRaw, orgName, enqDetails, mobileNo, qtnStatus, qtnNo, remarks;
            LocalDate inquiryDate, qtnDate;
            double qtnAmt;
            int rating;

            if (isDosing) {
                leadRef = defaultIfEmpty(getCellString(row, 2), "MJ");
                contactPerson = defaultIfEmpty(getCellString(row, 1), "MJ");
                group = defaultIfEmpty(getCellString(row, 3), "Dosing Department");
                inquiryDate = getCellDate(row, 4);
                statusRaw = getCellString(row, 5);
                enqDetails = defaultIfEmpty(getCellString(row, 6), "");
                orgName = defaultIfEmpty(getCellString(row, 7), "Unnamed Organisation");
                mobileNo = defaultIfEmpty(getCellString(row, 8), "");
                qtnStatus = defaultIfEmpty(getCellString(row, 9), "");
                qtnNo = defaultIfEmpty(getCellString(row, 10), "");
                qtnDate = getCellDate(row, 11);
                qtnAmt = getCellNumber(row, 12);
                rating = parseRating(getCellString(row, 13));
                remarks = defaultIfEmpty(getCellString(row, 14), "");
            } else {
                leadRef = defaultIfEmpty(getCellString(row, 1), "Pooja Mandmule");
                contactPerson = defaultIfEmpty(getCellString(row, 1), "Pooja Mandmule");
                group = defaultIfEmpty(getCellString(row, 2), "WTP Department");
                inquiryDate = getCellDate(row, 3);
                statusRaw = getCellString(row, 4);
                enqDetails = defaultIfEmpty(getCellString(row, 5), "");
                orgName = defaultIfEmpty(getCellString(row, 6), "Unnamed Organisation");
                mobileNo = defaultIfEmpty(getCellString(row, 7), "");
                qtnStatus = defaultIfEmpty(getCellString(row, 8), "");
                qtnNo = defaultIfEmpty(getCellString(row, 9), "");
                qtnDate = getCellDate(row, 10);
                qtnAmt = getCellNumber(row, 11);
                rating = parseRating(getCellString(row, 12));
                remarks = defaultIfEmpty(getCellString(row, 13), "");
            }

            String outcomeStatus = mapStatus(statusRaw);

            jdbcTemplate.update(insertSql,
                leadRef, contactPerson, group, inquiryDate != null ? java.sql.Date.valueOf(inquiryDate) : null, outcomeStatus,
                enqDetails, orgName, mobileNo, qtnStatus, qtnNo,
                qtnDate != null ? java.sql.Date.valueOf(qtnDate) : null, qtnAmt, rating, remarks,
                companyAdminId, teamId, memberId);
            count++;
        }
        return count;
    }

    // --- Helper methods ---

    private String mapStatus(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "Open";
        String upper = raw.trim().toUpperCase();
        if (upper.equals("CONVERTED") || upper.equals("WON")) return "Won";
        if (upper.equals("CLOSED") || upper.equals("LOST")) return "Closed";
        if (upper.equals("BUDGETORY") || upper.equals("BUDGETARY")) return "Budgetory";
        if (upper.equals("HOLD") || upper.equals("ON HOLD")) return "On Hold";
        if (upper.equals("QUALIFIED")) return "Qualified";
        if (upper.equals("NEGOTIATION")) return "Negotiation";
        return "Open";
    }

    private int parseRating(String val) {
        if (val == null) return 1;
        String s = val.trim();
        if (s.contains("***") || s.equals("3")) return 3;
        if (s.contains("**") || s.equals("2")) return 2;
        if (s.contains("*") || s.equals("1")) return 1;
        return 1;
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) return cell.getLocalDateTimeCellValue().toString();
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d)) return String.valueOf((long) d);
                return String.valueOf(d);
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try { return cell.getStringCellValue(); } catch (Exception e) {
                    try { return String.valueOf(cell.getNumericCellValue()); } catch (Exception e2) { return null; }
                }
            default: return null;
        }
    }

    private LocalDate getCellDate(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private double getCellNumber(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return 0.0;
        try {
            if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
            if (cell.getCellType() == CellType.STRING) {
                String s = cell.getStringCellValue().replaceAll("[^0-9.]", "");
                return s.isEmpty() ? 0.0 : Double.parseDouble(s);
            }
        } catch (Exception ignored) {}
        return 0.0;
    }

    private boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }
    private String defaultIfEmpty(String s, String def) { return isEmpty(s) ? def : s.trim(); }
}
