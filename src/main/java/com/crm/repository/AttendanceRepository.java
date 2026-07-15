package com.crm.repository;

import com.crm.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByUserIdFk(Long userIdFk);
    List<Attendance> findByUserId(Long userId);
    Optional<Attendance> findByUserIdAndDate(Long userId, LocalDate date);
}
