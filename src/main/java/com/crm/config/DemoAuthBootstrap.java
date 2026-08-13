package com.crm.config;

import com.crm.entity.Permission;
import com.crm.entity.Role;
import com.crm.entity.User;
import com.crm.repository.PermissionRepository;
import com.crm.repository.RoleRepository;
import com.crm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.crm.entity.Lead;
import com.crm.entity.LeadStatusMaster;
import com.crm.repository.LeadRepository;
import com.crm.repository.LeadStatusRepository;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DemoAuthBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final LeadRepository leadRepository;
    private final LeadStatusRepository leadStatusRepository;
    private final com.crm.repository.LeadSourceRepository leadSourceRepository;
    private final com.crm.repository.LeadGroupRepository leadGroupRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final String DEMO_PASSWORD = "Admin@123";

    @Override
    public void run(String... args) {
        alterTablesForAutoIncrement();
        runInTransaction();
    }

    @Transactional
    public void runInTransaction() {
        ensureRoles();
        ensurePermissions();
        ensureDemoUsers();
        ensureDemoLeads();
        ensureLeadStatuses();
        ensureLeadSources();
        ensureLeadGroups();
    }

    private void alterTablesForAutoIncrement() {
        // Alter Team table for team_lead_id_fk
        try {
            jdbcTemplate.execute("ALTER TABLE crm_xformsales_team ADD COLUMN team_lead_id_fk BIGINT");
            System.out.println("Added column team_lead_id_fk to crm_xformsales_team");
        } catch (Exception e) {
            System.out.println("Add team_lead_id_fk column failed: " + e.getMessage());
        }

        // Alter Permission table
        try {
            jdbcTemplate.execute("ALTER TABLE crm_xformsales_permission ADD PRIMARY KEY (permission_id)");
            System.out.println("Added primary key to crm_xformsales_permission");
        } catch (Exception e) {
            System.out.println("Add primary key to permission failed (might already exist): " + e.getMessage());
        }
        try {
            jdbcTemplate.execute("ALTER TABLE crm_xformsales_permission MODIFY COLUMN permission_id BIGINT AUTO_INCREMENT");
            System.out.println("Altered crm_xformsales_permission successfully to AUTO_INCREMENT");
        } catch (Exception e) {
            System.out.println("Alter table permission failed: " + e.getMessage());
        }

        // Alter Role table
        try {
            jdbcTemplate.execute("ALTER TABLE crm_xformsales_role ADD PRIMARY KEY (role_id)");
            System.out.println("Added primary key to crm_xformsales_role");
        } catch (Exception e) {
            System.out.println("Add primary key to role failed (might already exist): " + e.getMessage());
        }
        try {
            jdbcTemplate.execute("ALTER TABLE crm_xformsales_role MODIFY COLUMN role_id BIGINT AUTO_INCREMENT");
            System.out.println("Altered crm_xformsales_role successfully to AUTO_INCREMENT");
        } catch (Exception e) {
            System.out.println("Alter table role failed: " + e.getMessage());
        }

        // Alter User table
        try {
            jdbcTemplate.execute("ALTER TABLE crm_xformsales_user ADD PRIMARY KEY (userid)");
            System.out.println("Added primary key to crm_xformsales_user");
        } catch (Exception e) {
            System.out.println("Add primary key to user failed (might already exist): " + e.getMessage());
        }
        try {
            jdbcTemplate.execute("ALTER TABLE crm_xformsales_user MODIFY COLUMN userid BIGINT AUTO_INCREMENT");
            System.out.println("Altered crm_xformsales_user successfully to AUTO_INCREMENT");
        } catch (Exception e) {
            System.out.println("Alter table user failed: " + e.getMessage());
        }

        try {
            jdbcTemplate.execute("DELETE FROM crm_xformsales_role WHERE role_name IN ('Akash Kore', 'Meghraj', 'Suhas', 'Karthik', 'Suraj')");
        } catch (Exception e) {
            // Cleanup legacy roles if present
        }
    }

    private void ensureRoles() {
        List<String> roles = List.of(
                "SUPER_ADMIN",
                "ADMIN",
                "Team Lead",
                "Sales Manager",
                "Sales Executive",
                "Lead Qualifier",
                "Account Manager",
                "Support Executive"
        );

        List<Role> existingRoles = roleRepository.findAll();
        for (String roleName : roles) {
            boolean exists = existingRoles.stream()
                    .anyMatch(r -> r.getRoleName() != null && r.getRoleName().equalsIgnoreCase(roleName));
            if (!exists) {
                roleRepository.save(Role.builder().roleName(roleName).build());
            }
        }
    }

    private void ensurePermissions() {
        List<String> allPermissions = List.of(
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
                "trash.view", "trash.restore", "trash.delete",
                "data_access.view", "data_access.edit"
        );

        Map<String, List<String>> permsByRole = Map.of(
                "SUPER_ADMIN", allPermissions,
                "ADMIN", allPermissions,
                "Team Lead", List.of(
                        "dashboard.view", "leads.view", "leads.create", "leads.edit",
                        "opportunities.view", "opportunities.create", "opportunities.edit",
                        "projects.view", "projects.create", "projects.edit",
                        "tasks.view", "tasks.create", "tasks.edit", "tasks.delete",
                        "contacts.view", "contacts.create", "contacts.edit",
                        "organizations.view", "teams.view", "teams.create", "teams.edit",
                        "users.view", "users.create", "users.edit",
                        "reports.view", "calendar.view", "calendar.create", "calendar.edit",
                        "attendance.view", "activities.view", "emails.view", "analytics.view"
                ),
                "Sales Manager", List.of(
                        "dashboard.view", "leads.view", "leads.create", "leads.edit",
                        "opportunities.view", "opportunities.create", "projects.view", "tasks.view",
                        "activities.view", "emails.view", "calendar.view", "attendance.view",
                        "teams.view", "users.view", "analytics.view", "automation.view"
                ),
                "Sales Executive", List.of(
                        "dashboard.view", "leads.view", "leads.create", "opportunities.view", "tasks.view",
                        "activities.view", "emails.view", "calendar.view", "attendance.view"
                )
        );

        List<Role> allRoles = roleRepository.findAll();
        for (Map.Entry<String, List<String>> entry : permsByRole.entrySet()) {
            List<Role> matchingRoles = allRoles.stream()
                    .filter(r -> r.getRoleName() != null && r.getRoleName().equalsIgnoreCase(entry.getKey()))
                    .collect(Collectors.toList());

            for (Role role : matchingRoles) {
                Long roleId = role.getRoleId();
                for (String permission : entry.getValue()) {
                    if (!permissionRepository.existsByRoleIdFkAndGrpPerm(roleId, permission)) {
                        permissionRepository.save(Permission.builder()
                                 .roleIdFk(roleId)
                                 .grpPerm(permission)
                                 .build());
                    }
                }
            }
        }
    }

    private void ensureDemoUsers() {
        ensureUser("superadmin@crm.local", "superadmin", "SUPER_ADMIN");
        ensureUser("admin@crm.local", "admin", "ADMIN");
        ensureUser("manager.demo@crm.local", "manager.demo", "Sales Manager");
        ensureUser("executive.demo@crm.local", "executive.demo", "Sales Executive");
    }

    private void ensureUser(String email, String username, String role) {
        User user = userRepository.findByUserEmail(email)
                .orElseGet(() -> User.builder()
                        .userEmail(email)
                        .username(username)
                        .role(role)
                        .createdDate(LocalDate.now())
                        .build());

        user.setUsername(username);
        user.setRole(role);
        if (user.getCreatedDate() == null) {
            user.setCreatedDate(LocalDate.now());
        }

        if ("ADMIN".equals(role)) {
            user.setIntegrationsAccess(true);
            if (user.getPlanName() == null) {
                user.setPlanName("Enterprise Premium Plan");
            }
            if (user.getPlanPrice() == null) {
                user.setPlanPrice("$199/month");
            }
            if (user.getPlanValidity() == null) {
                user.setPlanValidity(LocalDate.now().plusYears(1));
            }
            if (user.getSubscriptionStatus() == null) {
                user.setSubscriptionStatus("Active");
            }
        }

        if (user.getPassword() == null || !passwordEncoder.matches(DEMO_PASSWORD, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        }

        userRepository.save(user);
    }

    private void ensureDemoLeads() {
        if (leadRepository.count() > 0) {
            return;
        }

        // Find default admin user to associate the leads with
        Optional<User> adminOpt = userRepository.findByUserEmail("admin@crm.local");
        Long adminId = adminOpt.map(User::getUserid).orElse(1L);

        // Lead 1: New Lead
        Lead lead1 = Lead.builder()
                .leadFirstName("John")
                .leadLastName("Doe")
                .leadTitle("TechCorp Solutions")
                .leadOrganisationName("TechCorp Solutions")
                .leadMobileNo("9876543210")
                .leadEmail("john.doe@techcorp.com")
                .leadSource("IndiaMART")
                .leadStatus("New Lead")
                .inquiryDate(LocalDate.now().minusDays(2))
                .leadCreatedDate(LocalDateTime.now().minusDays(2))
                .userIdFk(adminId)
                .build();
        leadRepository.save(lead1);

        // Lead 2: Qualified Lead
        Lead lead2 = Lead.builder()
                .leadFirstName("Sarah")
                .leadLastName("Connor")
                .leadTitle("Fintech Global CRM Project")
                .leadOrganisationName("Fintech Global")
                .leadMobileNo("9876543211")
                .leadEmail("sarah@fintech.com")
                .leadSource("Website Enquiry")
                .leadStatus("Qualified")
                .enquiryType("Qualified")
                .enquiryDescription("Looking for customized cloud CRM integration with local servers.")
                .companyContactPersonName("Sarah Connor")
                .inquiryDate(LocalDate.now().minusDays(5))
                .leadCreatedDate(LocalDateTime.now().minusDays(5))
                .userIdFk(adminId)
                .build();
        leadRepository.save(lead2);

        // Lead 3: Ongoing Lead - Priority B (Most Important)
        Lead lead3 = Lead.builder()
                .leadFirstName("Amit")
                .leadLastName("Patel")
                .leadTitle("Retail Inventory CRM Setup")
                .leadOrganisationName("Retail Builders")
                .leadMobileNo("9876543212")
                .leadEmail("amit@retailbuilders.com")
                .leadSource("Referral")
                .leadStatus("Ongoing")
                .enquiryType("Qualified")
                .enquiryDescription("Needs custom inventory management linked to CRM.")
                .companyContactPersonName("Amit Patel")
                .quotationNumber("Q-2026-001")
                .quotationDate(LocalDate.now().minusDays(10))
                .quotationAmount(new BigDecimal("320000.00"))
                .ongoingPriority("B")
                .inquiryDate(LocalDate.now().minusDays(12))
                .leadCreatedDate(LocalDateTime.now().minusDays(12))
                .userIdFk(adminId)
                .build();
        leadRepository.save(lead3);

        // Lead 4: Ongoing Lead - Priority A (Important)
        Lead lead4 = Lead.builder()
                .leadFirstName("Rajesh")
                .leadLastName("Kumar")
                .leadTitle("Logistics Custom Reporting Dashboard")
                .leadOrganisationName("Logistics Pro")
                .leadMobileNo("9876543213")
                .leadEmail("rajesh@logisticspro.in")
                .leadSource("Social Media")
                .leadStatus("Ongoing")
                .enquiryType("Qualified")
                .enquiryDescription("Wants driver scheduling and trip sheet reporting.")
                .companyContactPersonName("Rajesh Kumar")
                .quotationNumber("Q-2026-002")
                .quotationDate(LocalDate.now().minusDays(8))
                .quotationAmount(new BigDecimal("180000.00"))
                .ongoingPriority("A")
                .inquiryDate(LocalDate.now().minusDays(9))
                .leadCreatedDate(LocalDateTime.now().minusDays(9))
                .userIdFk(adminId)
                .build();
        leadRepository.save(lead4);

        // Lead 5: Won Lead
        Lead lead5 = Lead.builder()
                .leadFirstName("Sunita")
                .leadLastName("Rao")
                .leadTitle("Global Education Lead Portal")
                .leadOrganisationName("Global Education Inc")
                .leadMobileNo("9876543214")
                .leadEmail("sunita@globaledu.org")
                .leadSource("Cold Call")
                .leadStatus("Won")
                .enquiryType("Qualified")
                .enquiryDescription("Lead management system for student enquiries.")
                .companyContactPersonName("Sunita Rao")
                .quotationNumber("Q-2026-003")
                .quotationDate(LocalDate.now().minusDays(15))
                .quotationAmount(new BigDecimal("450000.00"))
                .inquiryDate(LocalDate.now().minusDays(16))
                .leadCreatedDate(LocalDateTime.now().minusDays(16))
                .userIdFk(adminId)
                .build();
        leadRepository.save(lead5);

        // Lead 6: Disqualified Lead
        Lead lead6 = Lead.builder()
                .leadFirstName("David")
                .leadLastName("Miller")
                .leadTitle("Consulting CRM Request")
                .leadOrganisationName("Consulting Solutions")
                .leadMobileNo("9876543215")
                .leadEmail("david@consultingsol.com")
                .leadSource("Other")
                .leadStatus("Disqualified")
                .enquiryType("Disqualified")
                .enquiryDescription("Requested personal CRM with no budget.")
                .companyContactPersonName("David Miller")
                .leadReason("No budget, looking for free tool")
                .inquiryDate(LocalDate.now().minusDays(20))
                .leadCreatedDate(LocalDateTime.now().minusDays(20))
                .userIdFk(adminId)
                .build();
        leadRepository.save(lead6);
    }

    private void ensureLeadStatuses() {
        if (leadStatusRepository.count() > 0) return;
        List<String> defaultStatuses = List.of("Open", "Negotiation", "Won", "Closed", "Qualified", "Disqualified");
        for (String s : defaultStatuses) {
            LeadStatusMaster st = new LeadStatusMaster();
            st.setStatusName(s);
            st.setDescription("Default system lead status");
            st.setActive(true);
            leadStatusRepository.save(st);
        }
        System.out.println("Seeded default LeadStatusMaster records successfully.");
    }

    private void ensureLeadSources() {
        if (leadSourceRepository.count() > 0) return;
        List<String> defaultSources = List.of("RRW", "Website", "IndiaMart", "TradeIndia", "Referral", "Cold Call", "Social Media", "Direct");
        for (String src : defaultSources) {
            com.crm.entity.LeadSourceMaster ls = new com.crm.entity.LeadSourceMaster();
            ls.setSourceName(src);
            ls.setActive(true);
            leadSourceRepository.save(ls);
        }
        System.out.println("Seeded default LeadSourceMaster records successfully.");
    }

    private void ensureLeadGroups() {
        if (leadGroupRepository.count() > 0) return;
        List<String> defaultGroups = List.of("Dosing Trading", "Sandur", "Dosing System", "Agitator System", "STP & WTP");
        for (String grp : defaultGroups) {
            com.crm.entity.LeadGroupsMaster lg = new com.crm.entity.LeadGroupsMaster();
            lg.setGroupName(grp);
            lg.setActive(true);
            leadGroupRepository.save(lg);
        }
        System.out.println("Seeded default LeadGroupsMaster records successfully.");
    }
}