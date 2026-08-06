package com.crm.scheduler;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Component
public class TrashCleanupScheduler {

    @PersistenceContext
    private EntityManager entityManager;

    // Run every day at midnight (00:00:00)
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void purgeExpiredTrash() {
        LocalDateTime threshold = LocalDateTime.now().minusMonths(6);
        
        System.out.println("Starting scheduled trash cleanup. Threshold: " + threshold);

        try {
            // Delete from task time log first to avoid FK constraint with task
            entityManager.createNativeQuery("DELETE FROM crm_task_time_log WHERE task_id_fk IN (SELECT task_id FROM crm_xformsales_task WHERE is_deleted = true AND deleted_at < :threshold)")
                    .setParameter("threshold", threshold)
                    .executeUpdate();

            // Delete from crm_xformsales_task
            int tasks = entityManager.createNativeQuery("DELETE FROM crm_xformsales_task WHERE is_deleted = true AND deleted_at < :threshold")
                    .setParameter("threshold", threshold)
                    .executeUpdate();

            // Delete from crm_xformsales_project
            int projects = entityManager.createNativeQuery("DELETE FROM crm_xformsales_project WHERE is_deleted = true AND deleted_at < :threshold")
                    .setParameter("threshold", threshold)
                    .executeUpdate();

            // Delete from crm_xformsales_opportunity
            int opps = entityManager.createNativeQuery("DELETE FROM crm_xformsales_opportunity WHERE is_deleted = true AND deleted_at < :threshold")
                    .setParameter("threshold", threshold)
                    .executeUpdate();

            // Delete from crm_xformsales_contact
            int contacts = entityManager.createNativeQuery("DELETE FROM crm_xformsales_contact WHERE is_deleted = true AND deleted_at < :threshold")
                    .setParameter("threshold", threshold)
                    .executeUpdate();

            // Delete lead child records
            entityManager.createNativeQuery("DELETE FROM crm_lead_reminder WHERE lead_id_fk IN (SELECT lead_id FROM crm_xformsales_lead WHERE is_deleted = true AND deleted_at < :threshold)")
                    .setParameter("threshold", threshold)
                    .executeUpdate();

            entityManager.createNativeQuery("DELETE FROM crm_lead_note WHERE lead_id_fk IN (SELECT lead_id FROM crm_xformsales_lead WHERE is_deleted = true AND deleted_at < :threshold)")
                    .setParameter("threshold", threshold)
                    .executeUpdate();

            entityManager.createNativeQuery("DELETE FROM crm_xformsales_lead_score WHERE lead_id_fk IN (SELECT lead_id FROM crm_xformsales_lead WHERE is_deleted = true AND deleted_at < :threshold)")
                    .setParameter("threshold", threshold)
                    .executeUpdate();

            entityManager.createNativeQuery("DELETE FROM crm_documents WHERE negotiation_revision_id IN (SELECT id FROM crm_negotiation_revision WHERE lead_id_fk IN (SELECT lead_id FROM crm_xformsales_lead WHERE is_deleted = true AND deleted_at < :threshold))")
                    .setParameter("threshold", threshold)
                    .executeUpdate();

            entityManager.createNativeQuery("DELETE FROM crm_negotiation_revision WHERE lead_id_fk IN (SELECT lead_id FROM crm_xformsales_lead WHERE is_deleted = true AND deleted_at < :threshold)")
                    .setParameter("threshold", threshold)
                    .executeUpdate();

            entityManager.createNativeQuery("DELETE FROM crm_negotiation WHERE lead_id_fk IN (SELECT lead_id FROM crm_xformsales_lead WHERE is_deleted = true AND deleted_at < :threshold)")
                    .setParameter("threshold", threshold)
                    .executeUpdate();

            // Delete from crm_xformsales_lead
            int leads = entityManager.createNativeQuery("DELETE FROM crm_xformsales_lead WHERE is_deleted = true AND deleted_at < :threshold")
                    .setParameter("threshold", threshold)
                    .executeUpdate();

            // Delete from crm_xformsales_team_member
            int members = entityManager.createNativeQuery("DELETE FROM crm_xformsales_team_member WHERE is_deleted = true AND deleted_at < :threshold")
                    .setParameter("threshold", threshold)
                    .executeUpdate();

            // Delete from crm_xformsales_team
            int teams = entityManager.createNativeQuery("DELETE FROM crm_xformsales_team WHERE is_deleted = true AND deleted_at < :threshold")
                    .setParameter("threshold", threshold)
                    .executeUpdate();

            // Delete from crm_xformsales_organization
            int orgs = entityManager.createNativeQuery("DELETE FROM crm_xformsales_organization WHERE is_deleted = true AND deleted_at < :threshold")
                    .setParameter("threshold", threshold)
                    .executeUpdate();

            System.out.println(String.format("Scheduled trash cleanup completed. Purged: %d leads, %d opportunities, %d projects, %d tasks, %d contacts, %d members, %d teams, %d organizations.",
                    leads, opps, projects, tasks, contacts, members, teams, orgs));
        } catch (Exception e) {
            System.err.println("Error during scheduled trash cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
