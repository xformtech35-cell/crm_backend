package com.crm.repository;

import com.crm.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    List<CalendarEvent> findByUserIdFk(Long userIdFk);

    @Query("SELECT e FROM CalendarEvent e WHERE e.userIdFk = :companyAdminId " +
           "AND e.startDatetime <= :end AND e.endDatetime >= :start " +
           "AND (e.status IS NULL OR e.status != 'CANCELLED')")
    List<CalendarEvent> findEventsInRange(
            @Param("companyAdminId") Long companyAdminId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT e FROM CalendarEvent e WHERE e.startDatetime <= :end AND e.endDatetime >= :start " +
           "AND (e.status IS NULL OR e.status != 'CANCELLED')")
    List<CalendarEvent> findAllEventsInRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    List<CalendarEvent> findByUserIdFkAndLeadIdFk(Long userIdFk, Long leadIdFk);
    List<CalendarEvent> findByUserIdFkAndOpportunityIdFk(Long userIdFk, Long opportunityIdFk);
    List<CalendarEvent> findByUserIdFkAndTaskIdFk(Long userIdFk, Long taskIdFk);

    List<CalendarEvent> findByRecurrenceParentId(Long recurrenceParentId);
}
