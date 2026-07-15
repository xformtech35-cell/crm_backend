package com.crm.repository;

import com.crm.entity.LeadReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadReminderRepository extends JpaRepository<LeadReminder, Long> {
    List<LeadReminder> findByLeadIdFkOrderByReminderDate(Long leadIdFk);

    long countByLeadIdFk(Long leadIdFk);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM LeadReminder r WHERE CAST(r.reminderDate AS date) = :date")
    List<LeadReminder> findByReminderDateOn(@org.springframework.data.repository.query.Param("date") java.time.LocalDate date);

    void deleteByLeadIdFk(Long leadIdFk);

    List<LeadReminder> findByUserIdFk(Long userIdFk);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM LeadReminder r WHERE r.userIdFk = :userId AND CAST(r.reminderDate AS date) = :date")
    List<LeadReminder> findByUserIdFkAndReminderDateOn(@org.springframework.data.repository.query.Param("userId") Long userId, @org.springframework.data.repository.query.Param("date") java.time.LocalDate date);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM LeadReminder r WHERE (r.sent IS NULL OR r.sent = false) AND r.reminderDate <= :now")
    List<LeadReminder> findPendingReminders(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);
}

