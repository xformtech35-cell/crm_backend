package com.crm.repository;

import com.crm.entity.CalendarAttendee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarAttendeeRepository extends JpaRepository<CalendarAttendee, Long> {
    List<CalendarAttendee> findByEventIdFk(Long eventIdFk);
    List<CalendarAttendee> findByUserIdFk(Long userIdFk);
    Optional<CalendarAttendee> findByEventIdFkAndUserIdFk(Long eventIdFk, Long userIdFk);
    void deleteByEventIdFk(Long eventIdFk);
}
