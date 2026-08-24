package com.crm.service;

import com.crm.entity.Task;
import com.crm.entity.TaskTimeLog;
import com.crm.repository.TaskRepository;
import com.crm.repository.TaskTimeLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class TaskTimeLogService {

    @Autowired
    private TaskTimeLogRepository taskTimeLogRepository;
    
    @Autowired
    private TaskRepository taskRepository;

    public TaskTimeLog startTimer(Long taskId, Long userId, Long userIdFk, String note) {
        // Fix 3: Auto-close any existing open session for this user before starting a new one
        if (userId != null) {
            List<TaskTimeLog> openLogs = taskTimeLogRepository.findByUserIdAndEndTimeIsNull(userId);
            for (TaskTimeLog openLog : openLogs) {
                try {
                    log.info("Auto-closing previous open timer session {} for user {}", openLog.getTimeLogId(), userId);
                    stopTimer(openLog.getTimeLogId());
                } catch (Exception e) {
                    log.error("Failed to auto-close open log {}: {}", openLog.getTimeLogId(), e.getMessage());
                }
            }
        }

        TaskTimeLog log = TaskTimeLog.builder()
                .taskId(taskId)
                .userId(userId)
                .userIdFk(userIdFk)
                .startTime(LocalDateTime.now())
                .note(note)
                .build();
        return taskTimeLogRepository.save(log);
    }

    public TaskTimeLog stopTimer(Long logId) {
        TaskTimeLog logEntry = taskTimeLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Log not found"));
        if (logEntry.getEndTime() != null) {
            return logEntry; // Already stopped
        }
        logEntry.setEndTime(LocalDateTime.now());
        long minutes = ChronoUnit.MINUTES.between(logEntry.getStartTime(), logEntry.getEndTime());
        logEntry.setDurationMinutes((int) minutes);
        TaskTimeLog saved = taskTimeLogRepository.save(logEntry);
        
        Task task = taskRepository.findById(logEntry.getTaskId()).orElse(null);
        if (task != null) {
            int current = task.getTaskTimeSpentMinutes() == null ? 0 : task.getTaskTimeSpentMinutes();
            task.setTaskTimeSpentMinutes(current + (int)minutes);
            taskRepository.save(task);
        }
        
        return saved;
    }

    /**
     * Fix 3: Scheduled job running every hour to auto-stop sessions left running for > 2 hours.
     */
    @Scheduled(fixedRate = 3600000)
    public void autoStopStaleTimers() {
        List<TaskTimeLog> openLogs = taskTimeLogRepository.findByEndTimeIsNull();
        LocalDateTime now = LocalDateTime.now();
        for (TaskTimeLog openLog : openLogs) {
            if (openLog.getStartTime() != null && ChronoUnit.MINUTES.between(openLog.getStartTime(), now) >= 120) {
                log.info("Auto-stopping stale timer {} running since {}", openLog.getTimeLogId(), openLog.getStartTime());
                openLog.setEndTime(openLog.getStartTime().plusHours(2));
                openLog.setDurationMinutes(120);
                openLog.setNote(openLog.getNote() != null ? openLog.getNote() + " (Auto-stopped by system after 2 hours)" : "Auto-stopped by system after 2 hours");
                taskTimeLogRepository.save(openLog);

                Task task = taskRepository.findById(openLog.getTaskId()).orElse(null);
                if (task != null) {
                    int current = task.getTaskTimeSpentMinutes() == null ? 0 : task.getTaskTimeSpentMinutes();
                    task.setTaskTimeSpentMinutes(current + 120);
                    taskRepository.save(task);
                }
            }
        }
    }

    public List<TaskTimeLog> getLogsByTaskId(Long taskId) {
        return taskTimeLogRepository.findByTaskId(taskId);
    }

    public List<TaskTimeLog> getLogsByUserId(Long userId) {
        return taskTimeLogRepository.findByUserId(userId);
    }

    public TaskTimeLog getActiveTimerForUser(Long userId) {
        if (userId == null) return null;
        List<TaskTimeLog> openLogs = taskTimeLogRepository.findByUserIdAndEndTimeIsNull(userId);
        return openLogs.isEmpty() ? null : openLogs.get(0);
    }
}


