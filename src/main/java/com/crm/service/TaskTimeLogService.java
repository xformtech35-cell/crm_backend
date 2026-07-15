package com.crm.service;

import com.crm.entity.Task;
import com.crm.entity.TaskTimeLog;
import com.crm.repository.TaskRepository;
import com.crm.repository.TaskTimeLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TaskTimeLogService {

    @Autowired
    private TaskTimeLogRepository taskTimeLogRepository;
    
    @Autowired
    private TaskRepository taskRepository;

    public TaskTimeLog startTimer(Long taskId, Long userId, Long userIdFk, String note) {
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
        TaskTimeLog log = taskTimeLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Log not found"));
        log.setEndTime(LocalDateTime.now());
        long minutes = ChronoUnit.MINUTES.between(log.getStartTime(), log.getEndTime());
        log.setDurationMinutes((int) minutes);
        TaskTimeLog saved = taskTimeLogRepository.save(log);
        
        Task task = taskRepository.findById(log.getTaskId()).orElse(null);
        if (task != null) {
            int current = task.getTaskTimeSpentMinutes() == null ? 0 : task.getTaskTimeSpentMinutes();
            task.setTaskTimeSpentMinutes(current + (int)minutes);
            taskRepository.save(task);
        }
        
        return saved;
    }

    public List<TaskTimeLog> getLogsByTaskId(Long taskId) {
        return taskTimeLogRepository.findByTaskId(taskId);
    }

    public List<TaskTimeLog> getLogsByUserId(Long userId) {
        return taskTimeLogRepository.findByUserId(userId);
    }
}
