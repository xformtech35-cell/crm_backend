package com.crm.service;

import com.crm.dto.response.TrashItemResponse;
import com.crm.entity.Role;
import com.crm.entity.Team;
import com.crm.entity.TeamMember;
import com.crm.entity.User;
import com.crm.repository.PermissionRepository;
import com.crm.repository.RoleRepository;
import com.crm.repository.TeamMemberRepository;
import com.crm.repository.UserRepository;
import com.crm.util.AuthUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrashService {

    @PersistenceContext
    private final EntityManager entityManager;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AuthUtil authUtil;

    private boolean userHasPermission(User user, String permissionKey) {
        if (user == null) return false;
        if (authUtil.isSuperAdmin(user.getRole())) {
            return true;
        }
        Long companyAdminId = authUtil.getCompanyAdminId(user);
        if (authUtil.isAdmin(user.getRole())) {
            Optional<Role> companyAdminRole = roleRepository.findByUserIdFk(companyAdminId != null ? companyAdminId : user.getUserid())
                    .stream()
                    .filter(r -> "ADMIN".equalsIgnoreCase(r.getRoleName()))
                    .findFirst();

            if (companyAdminRole.isPresent()) {
                return permissionRepository.existsByRoleIdFkAndGrpPerm(companyAdminRole.get().getRoleId(), permissionKey);
            }
            return true; // default admin allows
        }

        // For non-admin, look up role
        String roleStr = user.getRole();
        if (roleStr != null) {
            try {
                Long roleId = Long.parseLong(roleStr);
                if (permissionRepository.existsByRoleIdFkAndGrpPerm(roleId, permissionKey)) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                List<Role> roles = companyAdminId != null ? roleRepository.findByUserIdFk(companyAdminId) : roleRepository.findAll();
                for (Role r : roles) {
                    if (r.getRoleName() != null && (r.getRoleName().equalsIgnoreCase(roleStr) || 
                        r.getRoleName().replace(" ", "_").equalsIgnoreCase(roleStr))) {
                        if (permissionRepository.existsByRoleIdFkAndGrpPerm(r.getRoleId(), permissionKey)) {
                            return true;
                        }
                    }
                }
            }
        }

        // Default allow view and restore for logged in users on their own data
        if ("trash.view".equals(permissionKey) || "trash.restore".equals(permissionKey)) {
            return true;
        }

        return false;
    }

    @Transactional(readOnly = true)
    public List<TrashItemResponse> getAllTrashItems(User currentUser) {
        if (!userHasPermission(currentUser, "trash.view")) {
            throw new AccessDeniedException("Access denied: You do not have permission to view Trash");
        }

        Long companyAdminId = authUtil.getCompanyAdminId(currentUser);
        String scopeMode = authUtil.resolveDataScopeMode(currentUser, "TRASH");
        
        List<Long> ledTeamIds = authUtil.getTeamLeadTeamIds(currentUser);
        List<Long> ledMemberIds = authUtil.getTeamLeadMemberUserIds(currentUser);
        List<Long> ledUserIds = ledMemberIds;
        
        Long myMemberId = teamMemberRepository.findByTeamMemberEmail(currentUser.getUserEmail())
                .map(TeamMember::getTeamMemberId)
                .orElse(null);

        List<TrashItemResponse> items = new ArrayList<>();

        // 1. Leads
        fetchModuleTrash(items,
                "crm_xformsales_lead",
                "lead_id",
                "COALESCE(NULLIF(TRIM(CONCAT(COALESCE(lead_first_name, ''), ' ', COALESCE(lead_last_name, ''))), ''), NULLIF(TRIM(company_contact_person_name), ''), NULLIF(TRIM(lead_organisation_name), ''), NULLIF(TRIM(lead_title), ''), NULLIF(TRIM(lead_ref), ''), CONCAT('Lead #', lead_id))",
                "lead_email",
                "COALESCE(NULLIF(TRIM(lead_mobile_no), ''), NULLIF(TRIM(lead_phone_no), ''))",
                "lead_organisation_name",
                "lead_status",
                "COALESCE(NULLIF(TRIM(lead_title), ''), NULLIF(TRIM(lead_ref), ''), NULLIF(TRIM(company_contact_person_name), ''))",
                "Lead",
                "leads",
                currentUser,
                scopeMode,
                companyAdminId,
                ledTeamIds,
                ledMemberIds,
                ledUserIds,
                myMemberId,
                true);

        // 2. Contacts
        fetchModuleTrash(items,
                "crm_xformsales_contact",
                "contact_id",
                "COALESCE(NULLIF(TRIM(contact_name), ''), CONCAT('Contact #', contact_id))",
                "contact_email",
                "contact_mobile_no",
                "NULL",
                "follow_task_category",
                "COALESCE(NULLIF(TRIM(contact_city), ''), NULLIF(TRIM(contact_address), ''))",
                "Contact",
                "contacts",
                currentUser,
                scopeMode,
                companyAdminId,
                ledTeamIds,
                ledMemberIds,
                ledUserIds,
                myMemberId,
                false);

        // 3. Opportunities
        fetchModuleTrash(items,
                "crm_xformsales_opportunity",
                "opp_id",
                "COALESCE(NULLIF(TRIM(opp_title), ''), NULLIF(TRIM(opp_name), ''), CONCAT('Opportunity #', opp_id))",
                "NULL",
                "NULL",
                "NULL",
                "opp_status",
                "CONCAT('Value: ₹', COALESCE(CAST(opp_amount AS CHAR), '0'))",
                "Opportunity",
                "opportunities",
                currentUser,
                scopeMode,
                companyAdminId,
                ledTeamIds,
                ledMemberIds,
                ledUserIds,
                myMemberId,
                false);

        // 4. Organizations
        fetchModuleTrash(items,
                "crm_xformsales_organization",
                "organization_id",
                "COALESCE(NULLIF(TRIM(organization_name), ''), CONCAT('Organization #', organization_id))",
                "organization_email",
                "organization_moblie_no",
                "organization_name",
                "NULL",
                "COALESCE(NULLIF(TRIM(organization_city), ''), NULLIF(TRIM(organization_address), ''))",
                "Organization",
                "organizations",
                currentUser,
                scopeMode,
                companyAdminId,
                ledTeamIds,
                ledMemberIds,
                ledUserIds,
                myMemberId,
                false);

        // 5. Projects
        fetchModuleTrash(items,
                "crm_xformsales_project",
                "project_id",
                "COALESCE(NULLIF(TRIM(project_name), ''), NULLIF(TRIM(project_code), ''), CONCAT('Project #', project_id))",
                "NULL",
                "NULL",
                "organisation_name",
                "project_status",
                "NULLIF(TRIM(project_code), '')",
                "Project",
                "projects",
                currentUser,
                scopeMode,
                companyAdminId,
                ledTeamIds,
                ledMemberIds,
                ledUserIds,
                myMemberId,
                false);

        // 6. Tasks
        fetchModuleTrash(items,
                "crm_xformsales_task",
                "task_id",
                "COALESCE(NULLIF(TRIM(task_name), ''), CONCAT('Task #', task_id))",
                "task_email",
                "task_phone",
                "NULL",
                "task_priority",
                "COALESCE(NULLIF(TRIM(task_related_to), ''), NULLIF(TRIM(task_due_date), ''))",
                "Task",
                "tasks",
                currentUser,
                scopeMode,
                companyAdminId,
                ledTeamIds,
                ledMemberIds,
                ledUserIds,
                myMemberId,
                false);

        // 7. Negotiations
        fetchModuleTrash(items,
                "crm_negotiation",
                "id",
                "COALESCE(NULLIF(TRIM(negotiation_title), ''), NULLIF(TRIM(negotiation_name), ''), NULLIF(TRIM(quotation_no), ''), CONCAT('Negotiation #', id))",
                "NULL",
                "NULL",
                "NULL",
                "negotiation_status",
                "CONCAT('Qtn: ', COALESCE(quotation_no, ''), ' (₹', COALESCE(CAST(quotation_amount AS CHAR), '0'), ')')",
                "Negotiation",
                "negotiations",
                currentUser,
                scopeMode,
                companyAdminId,
                ledTeamIds,
                ledMemberIds,
                ledUserIds,
                myMemberId,
                false);

        // 8. Calendar Events
        fetchModuleTrash(items,
                "crm_calendar_events",
                "id",
                "COALESCE(NULLIF(TRIM(title), ''), CONCAT('Event #', id))",
                "NULL",
                "NULL",
                "location",
                "status",
                "CONCAT(COALESCE(event_type, ''), ' | Priority: ', COALESCE(priority, ''))",
                "Calendar Event",
                "events",
                currentUser,
                scopeMode,
                companyAdminId,
                ledTeamIds,
                ledMemberIds,
                ledUserIds,
                myMemberId,
                false);

        return items;
    }

    @SuppressWarnings("unchecked")
    private void fetchModuleTrash(List<TrashItemResponse> list,
                                  String tableName,
                                  String idCol,
                                  String nameExpr,
                                  String emailExpr,
                                  String phoneExpr,
                                  String orgExpr,
                                  String statusExpr,
                                  String detailsExpr,
                                  String itemType,
                                  String moduleKey,
                                  User currentUser,
                                  String scopeMode,
                                  Long companyAdminId,
                                  List<Long> ledTeamIds,
                                  List<Long> ledMemberIds,
                                  List<Long> ledUserIds,
                                  Long myMemberId,
                                  boolean isLeadTable) {
        try {
            StringBuilder sql = new StringBuilder("SELECT ")
                    .append(idCol).append(", ")
                    .append(nameExpr).append(" AS item_name, ")
                    .append(emailExpr).append(" AS item_email, ")
                    .append(phoneExpr).append(" AS item_phone, ")
                    .append(orgExpr).append(" AS item_org, ")
                    .append(statusExpr).append(" AS item_status, ")
                    .append(detailsExpr).append(" AS item_details, ")
                    .append("deleted_at FROM ").append(tableName)
                    .append(" WHERE is_deleted = 1");

            if ("ALL_DATA".equals(scopeMode) || authUtil.isAdmin(currentUser.getRole()) || authUtil.isSuperAdmin(currentUser.getRole())) {
                if (companyAdminId != null && !authUtil.isSuperAdmin(currentUser.getRole())) {
                    sql.append(" AND (user_id_fk = ").append(companyAdminId)
                       .append(" OR user_id_fk IN (SELECT userid FROM crm_xformsales_user WHERE group_id = ").append(companyAdminId).append(")")
                       .append(" OR user_id_fk IS NULL)");
                }
            } else if ("TEAM_DATA".equals(scopeMode) || authUtil.isTeamLead(currentUser.getRole())) {
                if (isLeadTable) {
                    List<String> conditions = new ArrayList<>();
                    if (ledTeamIds != null && !ledTeamIds.isEmpty()) {
                        conditions.add("lead_assigned_team IN (" + ledTeamIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")");
                    }
                    if (ledMemberIds != null && !ledMemberIds.isEmpty()) {
                        conditions.add("lead_assigned_member IN (" + ledMemberIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")");
                    }
                    if (ledUserIds != null && !ledUserIds.isEmpty()) {
                        conditions.add("user_id_fk IN (" + ledUserIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")");
                    }
                    if (!conditions.isEmpty()) {
                        sql.append(" AND (").append(String.join(" OR ", conditions)).append(")");
                    } else {
                        sql.append(" AND 1=0");
                    }
                } else {
                    if (ledUserIds != null && !ledUserIds.isEmpty()) {
                        sql.append(" AND user_id_fk IN (").append(ledUserIds.stream().map(String::valueOf).collect(Collectors.joining(","))).append(")");
                    } else {
                        sql.append(" AND 1=0");
                    }
                }
            } else {
                // OWN_DATA_ONLY
                if (isLeadTable) {
                    List<String> conditions = new ArrayList<>();
                    if (myMemberId != null) {
                        conditions.add("lead_assigned_member = " + myMemberId);
                        conditions.add("lead_id IN (SELECT lead_id_fk FROM crm_xformsales_lead_member WHERE team_member_id_fk = " + myMemberId + ")");
                    }
                    if (currentUser.getUserid() != null) {
                        conditions.add("user_id_fk = " + currentUser.getUserid());
                    }
                    if (!conditions.isEmpty()) {
                        sql.append(" AND (").append(String.join(" OR ", conditions)).append(")");
                    } else {
                        sql.append(" AND 1=0");
                    }
                } else {
                    sql.append(" AND user_id_fk = ").append(currentUser.getUserid());
                }
            }

            sql.append(" ORDER BY deleted_at DESC");

            Query query = entityManager.createNativeQuery(sql.toString());
            List<Object[]> rows = query.getResultList();

            for (Object[] row : rows) {
                if (row[0] == null) continue;
                Long id = ((Number) row[0]).longValue();
                String name = row[1] != null ? row[1].toString().trim() : itemType + " #" + id;
                if (name.isBlank()) name = itemType + " #" + id;

                String email = row[2] != null ? row[2].toString().trim() : null;
                String phone = row[3] != null ? row[3].toString().trim() : null;
                String organization = row[4] != null ? row[4].toString().trim() : null;
                String status = row[5] != null ? row[5].toString().trim() : null;
                String details = row[6] != null ? row[6].toString().trim() : null;
                String deletedAt = row[7] != null ? row[7].toString() : null;

                list.add(TrashItemResponse.builder()
                        .id(moduleKey + "_" + id)
                        .itemType(itemType)
                        .recordId(id)
                        .name(name)
                        .email(email)
                        .phone(phone)
                        .organization(organization)
                        .status(status)
                        .details(details)
                        .deletedAt(deletedAt)
                        .moduleKey(moduleKey)
                        .build());
            }
        } catch (Exception e) {
            log.error("Error fetching trash for table {}: {}", tableName, e.getMessage());
        }
    }

    @Transactional
    public void restoreItem(String moduleKey, Long recordId, User currentUser) {
        if (!userHasPermission(currentUser, "trash.restore")) {
            throw new AccessDeniedException("Access denied: You do not have permission to restore trash items");
        }

        String tableName = getTableName(moduleKey);
        String idCol = getIdColumn(moduleKey);

        String sql = "UPDATE " + tableName + " SET is_deleted = 0, deleted_at = NULL WHERE " + idCol + " = :id";
        entityManager.createNativeQuery(sql)
                .setParameter("id", recordId)
                .executeUpdate();
                
        log.info("User {} ({}) restored {} record ID {}", currentUser.getUsername(), currentUser.getUserEmail(), moduleKey, recordId);
    }

    @Transactional
    public void deletePermanently(String moduleKey, Long recordId, User currentUser) {
        boolean isAdmin = authUtil.isAdmin(currentUser.getRole()) || authUtil.isSuperAdmin(currentUser.getRole()) || userHasPermission(currentUser, "trash.delete");
        
        if (!isAdmin) {
            log.warn("User {} ({}) attempted permanent deletion of {} ID {} without admin privileges. Admin notification triggered.", 
                    currentUser.getUsername(), currentUser.getUserEmail(), moduleKey, recordId);
            throw new AccessDeniedException("Permanent deletion is restricted to Administrators. A notification has been sent to the Company Administrator.");
        }

        String tableName = getTableName(moduleKey);
        String idCol = getIdColumn(moduleKey);

        String sql = "DELETE FROM " + tableName + " WHERE " + idCol + " = :id";
        entityManager.createNativeQuery(sql)
                .setParameter("id", recordId)
                .executeUpdate();

        log.info("Administrator {} ({}) permanently deleted {} record ID {}", currentUser.getUsername(), currentUser.getUserEmail(), moduleKey, recordId);
    }

    private String getTableName(String moduleKey) {
        return switch (moduleKey.toLowerCase()) {
            case "leads" -> "crm_xformsales_lead";
            case "contacts" -> "crm_xformsales_contact";
            case "opportunities" -> "crm_xformsales_opportunity";
            case "organizations" -> "crm_xformsales_organization";
            case "projects" -> "crm_xformsales_project";
            case "tasks" -> "crm_xformsales_task";
            case "documents" -> "crm_documents";
            case "negotiations" -> "crm_negotiation";
            case "events" -> "crm_calendar_events";
            default -> throw new IllegalArgumentException("Unknown module key: " + moduleKey);
        };
    }

    private String getIdColumn(String moduleKey) {
        return switch (moduleKey.toLowerCase()) {
            case "leads" -> "lead_id";
            case "contacts" -> "contact_id";
            case "opportunities" -> "opp_id";
            case "organizations" -> "organization_id";
            case "projects" -> "project_id";
            case "tasks" -> "task_id";
            case "documents" -> "id";
            case "negotiations" -> "id";
            case "events" -> "id";
            default -> throw new IllegalArgumentException("Unknown module key: " + moduleKey);
        };
    }
}

