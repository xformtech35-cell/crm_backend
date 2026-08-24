package com.crm.service;

import com.crm.entity.Task;
import com.crm.entity.User;
import com.crm.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    /**
     * Send email notification when a task is assigned or reassigned to a user.
     */
    @Async
    public void sendTaskAssignmentEmail(Task task, Long assignedUserId, String assignerName) {
        if (assignedUserId == null) return;
        try {
            Optional<User> userOpt = userRepository.findById(assignedUserId);
            if (userOpt.isEmpty()) return;
            User assignedUser = userOpt.get();
            String recipientEmail = assignedUser.getUserEmail();
            if (recipientEmail == null || recipientEmail.trim().isEmpty()) return;

            String taskTitle = task.getTaskName() != null ? task.getTaskName() : "Untitled Task";
            String priority = task.getTaskPriority() != null ? task.getTaskPriority() : "Medium";
            String dueDate = task.getTaskDueDate() != null ? task.getTaskDueDate() : "Not specified";
            String relatedTo = task.getTaskRelatedTo() != null ? task.getTaskRelatedTo() : "N/A";
            String assignedBy = (assignerName != null && !assignerName.trim().isEmpty()) ? assignerName : "Admin/System";
            String taskIdStr = "TK-" + String.format("%03d", task.getTaskId() != null ? task.getTaskId() : 0);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipientEmail);
            helper.setSubject("New Task Assigned: " + taskTitle);

            String htmlContent = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>"
                    + "body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 20px; }"
                    + ".card { max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 12px; padding: 24px; border: 1px solid #e2e8f0; box-shadow: 0 4px 6px rgba(0,0,0,0.05); }"
                    + ".header { font-size: 20px; font-weight: bold; color: #4f46e5; margin-bottom: 16px; border-bottom: 2px solid #e0e7ff; padding-bottom: 10px; }"
                    + ".detail-row { display: flex; margin-bottom: 10px; font-size: 14px; color: #334155; }"
                    + ".label { font-weight: bold; width: 130px; color: #64748b; }"
                    + ".badge { display: inline-block; padding: 3px 10px; border-radius: 12px; font-weight: bold; font-size: 12px; background: #e0e7ff; color: #4338ca; }"
                    + ".btn { display: inline-block; background: #4f46e5; color: #ffffff !important; padding: 12px 24px; border-radius: 8px; font-weight: bold; text-decoration: none; margin-top: 20px; text-align: center; }"
                    + ".footer { font-size: 12px; color: #94a3b8; margin-top: 24px; text-align: center; }"
                    + "</style></head><body>"
                    + "<div class=\"card\">"
                    + "<div class=\"header\">📋 New Task Assignment</div>"
                    + "<p>Hi <strong>" + (assignedUser.getUsername() != null ? assignedUser.getUsername() : "Team Member") + "</strong>,</p>"
                    + "<p>You have been assigned a new task in Xform CRM:</p>"
                    + "<div style=\"background: #f8fafc; padding: 16px; border-radius: 8px; margin: 16px 0;\">"
                    + "<div className=\"detail-row\"><span className=\"label\">Task ID:</span> <strong>" + taskIdStr + "</strong></div>"
                    + "<div className=\"detail-row\"><span className=\"label\">Task Name:</span> <strong>" + taskTitle + "</strong></div>"
                    + "<div className=\"detail-row\"><span className=\"label\">Priority:</span> <span className=\"badge\">" + priority + "</span></div>"
                    + "<div className=\"detail-row\"><span className=\"label\">Due Date:</span> " + dueDate + "</div>"
                    + "<div className=\"detail-row\"><span className=\"label\">Linked Entity:</span> " + relatedTo + "</div>"
                    + "<div className=\"detail-row\"><span className=\"label\">Assigned By:</span> " + assignedBy + "</div>"
                    + "</div>"
                    + "<a href=\"http://localhost:3000/task\" class=\"btn\">View Task in CRM</a>"
                    + "<div class=\"footer\">Xform CRM — Revenue Control Center</div>"
                    + "</div></body></html>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Task assignment email sent successfully to {} for task {}", recipientEmail, task.getTaskId());
        } catch (Exception e) {
            log.error("Failed to send task assignment email to user {}: {}", assignedUserId, e.getMessage());
        }
    }

    /**
     * Send email escalation notification for overdue tasks to Team Lead / Admin.
     */
    @Async
    public void sendTaskOverdueEscalationEmail(String recipientEmail, String recipientName, List<Task> overdueTasks) {
        if (recipientEmail == null || recipientEmail.trim().isEmpty() || overdueTasks == null || overdueTasks.isEmpty()) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipientEmail);
            helper.setSubject("⚠️ Overdue Task Alert: " + overdueTasks.size() + " tasks need attention");

            StringBuilder rowsHtml = new StringBuilder();
            for (Task t : overdueTasks) {
                String taskIdStr = "TK-" + String.format("%03d", t.getTaskId() != null ? t.getTaskId() : 0);
                String taskName = t.getTaskName() != null ? t.getTaskName() : "Untitled";
                String dueDate = t.getTaskDueDate() != null ? t.getTaskDueDate() : "Overdue";
                String related = t.getTaskRelatedTo() != null ? t.getTaskRelatedTo() : "N/A";
                rowsHtml.append("<tr style=\"border-bottom: 1px solid #e2e8f0;\">")
                        .append("<td style=\"padding: 8px;\"><strong>").append(taskIdStr).append("</strong></td>")
                        .append("<td style=\"padding: 8px;\">").append(taskName).append("</td>")
                        .append("<td style=\"padding: 8px; color: #dc2626; font-weight: bold;\">").append(dueDate).append("</td>")
                        .append("<td style=\"padding: 8px;\">").append(related).append("</td>")
                        .append("</tr>");
            }

            String htmlContent = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>"
                    + "body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8fafc; padding: 20px; }"
                    + ".card { max-width: 650px; margin: 0 auto; background: #ffffff; border-radius: 12px; padding: 24px; border: 1px solid #e2e8f0; }"
                    + ".header { font-size: 20px; font-weight: bold; color: #dc2626; margin-bottom: 16px; }"
                    + "table { width: 100%; border-collapse: collapse; font-size: 14px; text-align: left; margin-top: 16px; }"
                    + "th { background: #fee2e2; color: #991b1b; padding: 8px; font-weight: bold; }"
                    + ".btn { display: inline-block; background: #dc2626; color: #ffffff !important; padding: 10px 20px; border-radius: 8px; font-weight: bold; text-decoration: none; margin-top: 20px; }"
                    + "</style></head><body>"
                    + "<div class=\"card\">"
                    + "<div class=\"header\">⚠️ Escalation Alert: Overdue Tasks</div>"
                    + "<p>Hi <strong>" + (recipientName != null ? recipientName : "Manager") + "</strong>,</p>"
                    + "<p>The following <strong>" + overdueTasks.size() + "</strong> task(s) are overdue by 2+ days and require immediate attention:</p>"
                    + "<table><thead><tr><th>ID</th><th>Task Name</th><th>Due Date</th><th>Linked Entity</th></tr></thead>"
                    + "<tbody>" + rowsHtml.toString() + "</tbody></table>"
                    + "<a href=\"http://localhost:3000/task\" class=\"btn\">Open Task Command Center</a>"
                    + "</div></body></html>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Overdue escalation email sent to {} for {} overdue tasks", recipientEmail, overdueTasks.size());
        } catch (Exception e) {
            log.error("Failed to send overdue escalation email to {}: {}", recipientEmail, e.getMessage());
        }
    }
}
