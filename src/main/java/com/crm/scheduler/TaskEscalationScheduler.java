package com.crm.scheduler;

import com.crm.entity.Task;
import com.crm.entity.User;
import com.crm.repository.TaskRepository;
import com.crm.repository.UserRepository;
import com.crm.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskEscalationScheduler {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * Fix 5: Morning cron job running daily at 9:00 AM.
     * Finds all tasks where taskDueDate < TODAY - 2 days and status != 'Done',
     * then group and emails escalation alerts to Team Leads and Admins.
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void processOverdueEscalations() {
        log.info("Running daily 9:00 AM Overdue Task Escalation job");
        List<Task> allTasks = taskRepository.findAll();
        LocalDate twoDaysAgo = LocalDate.now().minusDays(2);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        Map<Long, List<Task>> overdueByOwner = new HashMap<>();

        for (Task task : allTasks) {
            if ("Done".equalsIgnoreCase(task.getTaskAssign())) continue;
            String dueDateStr = task.getTaskDueDate();
            if (dueDateStr == null || dueDateStr.trim().isEmpty()) continue;

            try {
                String cleanDate = dueDateStr.trim().split("T")[0].split(" ")[0];
                LocalDate dueDate = LocalDate.parse(cleanDate, formatter);
                if (dueDate.isBefore(twoDaysAgo)) {
                    Long ownerId = task.getUserIdFk() != null ? task.getUserIdFk() : task.getTaskAssignedMember();
                    if (ownerId != null) {
                        overdueByOwner.computeIfAbsent(ownerId, k -> new ArrayList<>()).add(task);
                    }
                }
            } catch (Exception e) {
                // Ignore parse errors for irregular date formats
            }
        }

        if (overdueByOwner.isEmpty()) {
            log.info("No tasks overdue by 2+ days found.");
            return;
        }

        for (Map.Entry<Long, List<Task>> entry : overdueByOwner.entrySet()) {
            Long userId = entry.getKey();
            List<Task> overdueList = entry.getValue();
            User manager = userRepository.findById(userId).orElse(null);
            if (manager != null && manager.getUserEmail() != null) {
                emailService.sendTaskOverdueEscalationEmail(manager.getUserEmail(), manager.getUsername(), overdueList);
            }
        }
    }
}
