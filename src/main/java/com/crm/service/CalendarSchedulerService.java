package com.crm.service;

import com.crm.entity.CalendarNotification;
import com.crm.repository.CalendarNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarSchedulerService {

    private final CalendarNotificationRepository calendarNotificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedRate = 15000) // Check every 15 seconds
    @Transactional
    public void processPendingNotifications() {
        LocalDateTime now = LocalDateTime.now();
        List<CalendarNotification> dueNotifications = calendarNotificationRepository.findPendingNotificationsDue(now);

        if (dueNotifications.isEmpty()) {
            return;
        }

        log.info("Processing {} due calendar notifications at {}", dueNotifications.size(), now);

        for (CalendarNotification notif : dueNotifications) {
            notif.setStatus("SENT");
            notif.setSentAt(now);
            calendarNotificationRepository.save(notif);

            // Payload for WebSocket / Front-end listener
            Map<String, Object> payload = new HashMap<>();
            payload.put("notificationId", notif.getId());
            payload.put("eventId", notif.getEventIdFk());
            payload.put("userId", notif.getUserIdFk());
            payload.put("title", notif.getTitle());
            payload.put("message", notif.getMessage());
            payload.put("scheduledAt", notif.getScheduledAt());
            payload.put("type", "CALENDAR_REMINDER");

            try {
                // Send STOMP payload to specific user topic
                messagingTemplate.convertAndSend("/topic/notifications/" + notif.getUserIdFk(), payload);
            } catch (Exception e) {
                log.warn("WebSocket dispatch failed for user {}: {}", notif.getUserIdFk(), e.getMessage());
            }
        }
    }
}
