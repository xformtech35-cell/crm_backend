package com.crm.service;

import com.crm.entity.Attendance;
import com.crm.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    public Attendance punchIn(Long userId, Long userIdFk, String location, String status) {
        LocalDate today = LocalDate.now();
        Optional<Attendance> existing = attendanceRepository.findByUserIdAndDate(userId, today);
        if (existing.isPresent()) {
            throw new RuntimeException("Already punched in today");
        }
        Attendance attendance = Attendance.builder()
                .userId(userId)
                .userIdFk(userIdFk)
                .date(today)
                .checkIn(LocalDateTime.now())
                .location(location)
                .status(status)
                .build();
        return attendanceRepository.save(attendance);
    }

    public Attendance punchOut(Long userId) {
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByUserIdAndDate(userId, today)
                .orElseThrow(() -> new RuntimeException("No punch-in found for today"));
        
        attendance.setCheckOut(LocalDateTime.now());
        long minutes = ChronoUnit.MINUTES.between(attendance.getCheckIn(), attendance.getCheckOut());
        attendance.setDurationMinutes((int) minutes);
        return attendanceRepository.save(attendance);
    }

    public Attendance getTodayAttendance(Long userId) {
        return attendanceRepository.findByUserIdAndDate(userId, LocalDate.now()).orElse(null);
    }

    public List<Attendance> getHistoryByUser(Long userId) {
        return attendanceRepository.findByUserId(userId);
    }
    
    public List<Attendance> getAllHistoryByUserIdFk(Long userIdFk) {
        return attendanceRepository.findByUserIdFk(userIdFk);
    }
    
    public List<Attendance> getAll() {
        return attendanceRepository.findAll();
    }
}
