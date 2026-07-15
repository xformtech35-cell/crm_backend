package com.crm.service;

import com.crm.entity.Lead;
import com.crm.entity.LeadReminder;
import com.crm.entity.User;
import com.crm.repository.LeadRepository;
import com.crm.repository.LeadReminderRepository;
import com.crm.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadReminderEmailService {

    private final LeadReminderRepository leadReminderRepository;
    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    // Runs every 1 minute
    @Scheduled(fixedRate = 60000)
    public void processReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<LeadReminder> pendingReminders = leadReminderRepository.findPendingReminders(now);
        
        if (pendingReminders.isEmpty()) {
            return;
        }

        log.info("Found {} pending lead reminders to send", pendingReminders.size());

        for (LeadReminder reminder : pendingReminders) {
            try {
                sendReminderEmail(reminder);
                reminder.setSent(true);
                leadReminderRepository.save(reminder);
            } catch (Exception e) {
                log.error("Failed to send email for reminder {}: {}", reminder.getLeadReminderId(), e.getMessage());
            }
        }
    }

    public void sendReminderEmailManual(Long reminderId) throws Exception {
        Optional<LeadReminder> reminderOpt = leadReminderRepository.findById(reminderId);
        if (reminderOpt.isPresent()) {
            LeadReminder reminder = reminderOpt.get();
            sendReminderEmail(reminder);
            reminder.setSent(true);
            leadReminderRepository.save(reminder);
        } else {
            throw new RuntimeException("Reminder not found with ID " + reminderId);
        }
    }

    private void sendReminderEmail(LeadReminder reminder) throws Exception {
        Optional<User> userOpt = userRepository.findById(reminder.getUserIdFk());
        if (userOpt.isEmpty()) {
            log.warn("User not found for ID: {} in reminder: {}", reminder.getUserIdFk(), reminder.getLeadReminderId());
            return;
        }
        User user = userOpt.get();
        String agentEmail = user.getUserEmail();
        if (agentEmail == null || agentEmail.trim().isEmpty()) {
            log.warn("User email is empty for ID: {}", user.getUserid());
            return;
        }

        Optional<Lead> leadOpt = leadRepository.findById(reminder.getLeadIdFk());
        if (leadOpt.isEmpty()) {
            log.warn("Lead not found for ID: {} in reminder: {}", reminder.getLeadIdFk(), reminder.getLeadReminderId());
            return;
        }
        Lead lead = leadOpt.get();

        String agentName = user.getUsername() != null ? user.getUsername() : "Representative";
        String leadName = (lead.getLeadFirstName() != null ? lead.getLeadFirstName() : "") + " " +
                           (lead.getLeadLastName() != null ? lead.getLeadLastName() : "");
        leadName = leadName.trim().isEmpty() ? "Valued Lead" : leadName;
        String leadOrg = lead.getLeadOrganisationName() != null ? lead.getLeadOrganisationName() : "N/A";
        String leadEmail = lead.getLeadEmail() != null ? lead.getLeadEmail() : "N/A";
        String leadPhone = lead.getLeadMobileNo() != null ? lead.getLeadMobileNo() : "N/A";
        String leadStatus = lead.getLeadStatus() != null ? lead.getLeadStatus() : "N/A";
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        String reminderTime = reminder.getReminderDate() != null ? reminder.getReminderDate().format(formatter) : "N/A";
        String reminderText = reminder.getReminderText() != null ? reminder.getReminderText() : "No message provided.";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(agentEmail);
        helper.setSubject("CRM Reminder: Follow up with " + leadName + " (" + leadOrg + ")");
        
        String htmlContent = getEmailTemplate(agentName, leadName, leadOrg, leadEmail, leadPhone, leadStatus, reminderTime, reminderText);
        helper.setText(htmlContent, true);

        mailSender.send(message);
        log.info("Reminder email sent successfully to {} for lead {}", agentEmail, leadName);
    }

    private String getEmailTemplate(String agentName, String leadName, String leadOrg, String leadEmail, 
                                     String leadPhone, String leadStatus, String reminderTime, String reminderText) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta charset=\"utf-8\">\n" +
                "  <style>\n" +
                "    body {\n" +
                "      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
                "      background-color: #f8fafc;\n" +
                "      color: #1e293b;\n" +
                "      margin: 0;\n" +
                "      padding: 0;\n" +
                "    }\n" +
                "    .container {\n" +
                "      max-width: 600px;\n" +
                "      margin: 40px auto;\n" +
                "      background: #ffffff;\n" +
                "      border-radius: 16px;\n" +
                "      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);\n" +
                "      overflow: hidden;\n" +
                "      border: 1px solid #e2e8f0;\n" +
                "    }\n" +
                "    .header {\n" +
                "      background: linear-gradient(135deg, #4f46e5, #7c3aed);\n" +
                "      padding: 32px;\n" +
                "      text-align: center;\n" +
                "      color: #ffffff;\n" +
                "    }\n" +
                "    .header h1 {\n" +
                "      margin: 0;\n" +
                "      font-size: 24px;\n" +
                "      font-weight: 800;\n" +
                "      letter-spacing: -0.025em;\n" +
                "    }\n" +
                "    .content {\n" +
                "      padding: 32px;\n" +
                "    }\n" +
                "    .welcome {\n" +
                "      font-size: 16px;\n" +
                "      line-height: 24px;\n" +
                "      margin-bottom: 24px;\n" +
                "    }\n" +
                "    .reminder-box {\n" +
                "      background-color: #f1f5f9;\n" +
                "      border-left: 4px solid #4f46e5;\n" +
                "      padding: 20px;\n" +
                "      border-radius: 8px;\n" +
                "      margin-bottom: 24px;\n" +
                "    }\n" +
                "    .reminder-text {\n" +
                "      font-size: 16px;\n" +
                "      font-style: italic;\n" +
                "      color: #334155;\n" +
                "      margin: 0;\n" +
                "    }\n" +
                "    .details-table {\n" +
                "      width: 100%;\n" +
                "      border-collapse: collapse;\n" +
                "      margin-bottom: 24px;\n" +
                "    }\n" +
                "    .details-table th, .details-table td {\n" +
                "      padding: 12px 0;\n" +
                "      border-bottom: 1px solid #f1f5f9;\n" +
                "      text-align: left;\n" +
                "      font-size: 14px;\n" +
                "    }\n" +
                "    .details-table th {\n" +
                "      color: #64748b;\n" +
                "      font-weight: 600;\n" +
                "      width: 35%;\n" +
                "    }\n" +
                "    .details-table td {\n" +
                "      color: #0f172a;\n" +
                "      font-weight: 500;\n" +
                "    }\n" +
                "    .footer {\n" +
                "      background-color: #f8fafc;\n" +
                "      padding: 24px;\n" +
                "      text-align: center;\n" +
                "      font-size: 12px;\n" +
                "      color: #64748b;\n" +
                "      border-top: 1px solid #e2e8f0;\n" +
                "    }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"container\">\n" +
                "    <div class=\"header\">\n" +
                "      <h1>Lead Follow-up Reminder</h1>\n" +
                "    </div>\n" +
                "    <div class=\"content\">\n" +
                "      <p class=\"welcome\">Hello <strong>" + agentName + "</strong>,</p>\n" +
                "      <p>This is a scheduled reminder to follow up with the following lead:</p>\n" +
                "      \n" +
                "      <div class=\"reminder-box\">\n" +
                "        <p class=\"reminder-text\">\"" + reminderText + "\"</p>\n" +
                "      </div>\n" +
                "\n" +
                "      <table class=\"details-table\">\n" +
                "        <tr>\n" +
                "          <th>Lead Name</th>\n" +
                "          <td>" + leadName + "</td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "          <th>Organisation</th>\n" +
                "          <td>" + leadOrg + "</td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "          <th>Email</th>\n" +
                "          <td><a href=\"mailto:" + leadEmail + "\" style=\"color: #4f46e5; text-decoration: none;\">" + leadEmail + "</a></td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "          <th>Phone</th>\n" +
                "          <td>" + leadPhone + "</td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "          <th>Status</th>\n" +
                "          <td>" + leadStatus + "</td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "          <th>Scheduled Time</th>\n" +
                "          <td>" + reminderTime + "</td>\n" +
                "        </tr>\n" +
                "      </table>\n" +
                "\n" +
                "      <p style=\"font-size: 14px; color: #64748b; margin-top: 32px;\">\n" +
                "        Please log in to the CRM workspace to update follow-up notes and outcome status.\n" +
                "      </p>\n" +
                "    </div>\n" +
                "    <div class=\"footer\">\n" +
                "      This is an automated notification from your CRM system.\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>";
    }
}
