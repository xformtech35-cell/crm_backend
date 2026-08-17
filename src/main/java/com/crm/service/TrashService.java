package com.crm.service;

import com.crm.dto.response.TrashItemResponse;
import com.crm.entity.Role;
import com.crm.entity.User;
import com.crm.repository.PermissionRepository;
import com.crm.repository.RoleRepository;
import com.crm.util.AuthUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TrashService {

    @PersistenceContext
    private final EntityManager entityManager;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final AuthUtil authUtil;

    private boolean userHasPermission(User user, String permissionKey) {
        if (user == null) return false;
        if (authUtil.isSuperAdmin(user.getRole())) {
            return true;
        }
        if (authUtil.isAdmin(user.getRole())) {
            Optional<Role> companyAdminRole = roleRepository.findByUserIdFk(user.getUserid())
                    .stream()
                    .filter(r -> "ADMIN".equalsIgnoreCase(r.getRoleName()))
                    .findFirst();

            if (companyAdminRole.isPresent()) {
                return permissionRepository.existsByRoleIdFkAndGrpPerm(companyAdminRole.get().getRoleId(), permissionKey);
            }
            return false;
        }
        if (user.getRole() == null) return false;
        try {
            Long roleId = Long.parseLong(user.getRole());
            return permissionRepository.existsByRoleIdFkAndGrpPerm(roleId, permissionKey);
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public List<TrashItemResponse> getAllTrashItems(User currentUser) {
        if (!userHasPermission(currentUser, "trash.view")) {
            throw new AccessDeniedException("Access denied: You do not have permission to view Trash");
        }

        Long companyAdminId = authUtil.getCompanyAdminId(currentUser);
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
                companyAdminId);

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
                companyAdminId);

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
                companyAdminId);

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
                companyAdminId);

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
                companyAdminId);

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
                companyAdminId);

        return items;
    }

    @SuppressWarnings("unchecked")
    private void fetchModuleTrash(List<TrashItemResponse> list, String tableName, String idCol, String nameExpr, String emailExpr, String phoneExpr, String orgExpr, String statusExpr, String detailsExpr, String itemType, String moduleKey, Long companyAdminId) {
        try {
            String sql = "SELECT " + idCol + ", " 
                    + nameExpr + " AS item_name, " 
                    + emailExpr + " AS item_email, " 
                    + phoneExpr + " AS item_phone, " 
                    + orgExpr + " AS item_org, " 
                    + statusExpr + " AS item_status, " 
                    + detailsExpr + " AS item_details, " 
                    + "deleted_at FROM " + tableName + " WHERE is_deleted = 1";

            if (companyAdminId != null) {
                sql += " AND (user_id_fk = " + companyAdminId + " OR user_id_fk IS NULL)";
            }
            sql += " ORDER BY deleted_at DESC";

            Query query = entityManager.createNativeQuery(sql);
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
            System.err.println("Error fetching trash for table " + tableName + ": " + e.getMessage());
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
    }

    @Transactional
    public void deletePermanently(String moduleKey, Long recordId, User currentUser) {
        if (!userHasPermission(currentUser, "trash.delete")) {
            throw new AccessDeniedException("Access denied: You do not have permission to permanently delete items");
        }

        String tableName = getTableName(moduleKey);
        String idCol = getIdColumn(moduleKey);

        String sql = "DELETE FROM " + tableName + " WHERE " + idCol + " = :id";
        entityManager.createNativeQuery(sql)
                .setParameter("id", recordId)
                .executeUpdate();
    }

    private String getTableName(String moduleKey) {
        return switch (moduleKey.toLowerCase()) {
            case "leads" -> "crm_xformsales_lead";
            case "contacts" -> "crm_xformsales_contact";
            case "opportunities" -> "crm_xformsales_opportunity";
            case "organizations" -> "crm_xformsales_organization";
            case "projects" -> "crm_xformsales_project";
            case "tasks" -> "crm_xformsales_task";
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
            default -> throw new IllegalArgumentException("Unknown module key: " + moduleKey);
        };
    }
}
