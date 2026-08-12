package com.crm.repository;

import com.crm.entity.CalendarNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CalendarNotificationRepository extends JpaRepository<CalendarNotification, Long> {
    List<CalendarNotification> findByUserIdFkAndStatus(Long userIdFk, String status);

    @Query("SELECT n FROM CalendarNotification n WHERE n.status = 'PENDING' AND n.scheduledAt <= :now")
    List<CalendarNotification> findPendingNotificationsDue(@Param("now") LocalDateTime now);

    List<CalendarNotification> findByUserIdFkOrderByScheduledAtDesc(Long userIdFk);
    List<CalendarNotification> findByEventIdFk(Long eventIdFk);
}
