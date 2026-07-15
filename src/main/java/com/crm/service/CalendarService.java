package com.crm.service;

import com.crm.entity.LeadReminder;
import com.crm.entity.Task;
import com.crm.repository.LeadReminderRepository;
import com.crm.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final TaskRepository taskRepository;
    private final LeadReminderRepository leadReminderRepository;
    private final com.crm.util.AuthUtil authUtil;

    public Map<String, Object> getAllCalendarEvents(Long companyAdminId, String role) {
        List<Task> tasks;
        List<LeadReminder> reminders;
        if (authUtil.isSuperAdmin(role)) {
            tasks = taskRepository.findAll();
            reminders = leadReminderRepository.findAll();
        } else {
            tasks = taskRepository.findByUserIdFk(companyAdminId);
            reminders = leadReminderRepository.findByUserIdFk(companyAdminId);
        }
        return buildCalendarResponse(tasks, reminders);
    }

    public Map<String, Object> getCalendarEvents(String date, Long companyAdminId, String role) {
        LocalDate localDate;
        try {
            localDate = LocalDate.parse(date);
        } catch (Exception e) {
            localDate = LocalDate.now();
        }
        List<Task> tasks;
        List<LeadReminder> reminders;
        if (authUtil.isSuperAdmin(role)) {
            tasks = taskRepository.findByTaskDueDate(localDate);
            reminders = leadReminderRepository.findByReminderDateOn(localDate);
        } else {
            tasks = taskRepository.findByUserIdFkAndTaskDueDate(companyAdminId, localDate);
            reminders = leadReminderRepository.findByUserIdFkAndReminderDateOn(companyAdminId, localDate);
        }

        return buildCalendarResponse(tasks, reminders);
    }

    private Map<String, Object> buildCalendarResponse(List<Task> tasks, List<LeadReminder> reminders) {
        List<Map<String, Object>> events = new ArrayList<>();

        tasks.forEach(t -> {
            Map<String, Object> event = new HashMap<>();
            event.put("type", "task");
            event.put("id", t.getTaskId());
            event.put("title", t.getTaskName());
            event.put("date", t.getTaskDueDate() != null ? t.getTaskDueDate().toString() : null);
            event.put("priority", t.getTaskPriority());
            event.put("status", t.getTaskPercentageCompleted());
            event.put("note", t.getTaskDescription());
            event.put("owner", t.getTaskAssign());
            events.add(event);
        });

        reminders.forEach(r -> {
            Map<String, Object> event = new HashMap<>();
            event.put("type", "reminder");
            event.put("id", r.getLeadReminderId());
            event.put("title", r.getReminderText());
            event.put("date", r.getReminderDate() != null ? r.getReminderDate().toString() : null);
            event.put("leadId", r.getLeadIdFk());
            events.add(event);
        });

        Map<String, Object> result = new HashMap<>();
        result.put("events", events);
        result.put("tasks", tasks);
        result.put("reminders", reminders);
        return result;
    }
}
