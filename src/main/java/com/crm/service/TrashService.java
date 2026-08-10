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
        fetchModuleTrash(items, "crm_xformsales_lead", "lead_id", "CONCAT(COALESCE(lead_first_name, ''), ' ', COALESCE(lead_last_name, ''))", "Lead", "leads", companyAdminId);

        // 2. Contacts
        fetchModuleTrash(items, "crm_xformsales_contact", "contact_id", "contact_name", "Contact", "contacts", companyAdminId);

        // 3. Opportunities
        fetchModuleTrash(items, "crm_xformsales_opportunity", "opp_id", "COALESCE(opp_title, opp_name)", "Opportunity", "opportunities", companyAdminId);

        // 4. Organizations
        fetchModuleTrash(items, "crm_xformsales_organization", "organization_id", "organization_name", "Organization", "organizations", companyAdminId);

        // 5. Projects
        fetchModuleTrash(items, "crm_xformsales_project", "project_id", "project_name", "Project", "projects", companyAdminId);

        // 6. Tasks
        fetchModuleTrash(items, "crm_xformsales_task", "task_id", "task_name", "Task", "tasks", companyAdminId);

        return items;
    }

    @SuppressWarnings("unchecked")
    private void fetchModuleTrash(List<TrashItemResponse> list, String tableName, String idCol, String nameCol, String itemType, String moduleKey, Long companyAdminId) {
        try {
            String sql = "SELECT " + idCol + ", " + nameCol + " AS item_name, deleted_at FROM " + tableName + " WHERE is_deleted = 1";
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
                String deletedAt = row[2] != null ? row[2].toString() : null;

                list.add(TrashItemResponse.builder()
                        .id(moduleKey + "_" + id)
                        .itemType(itemType)
                        .recordId(id)
                        .name(name)
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
