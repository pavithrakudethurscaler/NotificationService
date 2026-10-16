package com.ecom.notifications.services;

import com.ecom.notifications.clients.UserClient;
import com.ecom.notifications.dtos.EventDto;
import com.ecom.notifications.exceptions.InvalidEventException;
import com.ecom.notifications.models.EventType;
import com.ecom.notifications.models.Notification;
import com.ecom.notifications.models.NotificationStatus;
import com.ecom.notifications.repos.NotificationRepo;
import com.ecom.notifications.senders.EmailSender;
import com.ecom.notifications.templates.NotificationTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    /** A PENDING notification older than this was interrupted (e.g. by a restart) and is picked up by the retry job */
    private static final long STUCK_PENDING_MS = 60_000;

    private final NotificationRepo notificationRepo;
    private final NotificationTemplates templates;
    private final EmailSender emailSender;
    private final UserClient userClient;
    private final int maxAttempts;

    public NotificationService(NotificationRepo notificationRepo, NotificationTemplates templates,
                               EmailSender emailSender, UserClient userClient,
                               @Value("${notification.retry.max-attempts:5}") int maxAttempts) {
        this.notificationRepo = notificationRepo;
        this.templates = templates;
        this.emailSender = emailSender;
        this.userClient = userClient;
        this.maxAttempts = maxAttempts;
    }

    /*
     1. Validate the envelope and drop duplicates (same eventId)
     2. Find the recipient: from the event (User Service events) or by looking up the user id
     3. Render the message and store it as PENDING
     4. Try to send it; on failure it stays FAILED and the retry job tries again
     @return false if the event was a duplicate
     */
    public boolean handleEvent(EventDto event) throws InvalidEventException {
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            throw new InvalidEventException("eventId is required");
        }
        EventType type = parseType(event.getType());
        Map<String, Object> data = event.getData() == null ? Map.of() : event.getData();
        Long userId = parseUserId(data.get("userId"));

        if (notificationRepo.existsByEventId(event.getEventId())) {
            return false;
        }

        String email = stringOrNull(data.get("email"));
        String name = stringOrNull(data.get("name"));
        if (email == null) {
            UserClient.UserContact user = userClient.getUser(userId);
            email = user.email();
            name = user.name();
        }
        NotificationTemplates.Message message = templates.render(type, name, data);

        Notification notification = new Notification();
        notification.setEventId(event.getEventId());
        notification.setEventType(type);
        notification.setUserId(userId);
        notification.setRecipientEmail(email);
        notification.setSubject(message.subject());
        notification.setBody(message.body());
        notification.setStatus(NotificationStatus.PENDING);
        notification.setCreatedAt(System.currentTimeMillis());
        try {
            notification = notificationRepo.save(notification);
        } catch (DataIntegrityViolationException e) {
            // The same event arrived twice at the same moment; the other request stored it
            return false;
        }
        deliver(notification);
        return true;
    }

    public List<Notification> getNotifications(Long userId) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** Retries failed and interrupted notifications until they reach the attempt limit */
    @Scheduled(fixedDelayString = "${notification.retry.interval-ms:60000}",
            initialDelayString = "${notification.retry.interval-ms:60000}")
    public void retryUndelivered() {
        long stuckBefore = System.currentTimeMillis() - STUCK_PENDING_MS;
        notificationRepo.findByStatusAndAttemptsLessThan(NotificationStatus.FAILED, maxAttempts).forEach(this::deliver);
        notificationRepo.findByStatusAndAttemptsLessThan(NotificationStatus.PENDING, maxAttempts).stream()
                .filter(n -> n.getCreatedAt() < stuckBefore)
                .forEach(this::deliver);
    }

    private void deliver(Notification notification) {
        notification.setAttempts(notification.getAttempts() + 1);
        try {
            emailSender.send(notification.getRecipientEmail(), notification.getSubject(), notification.getBody());
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(System.currentTimeMillis());
            notification.setLastError(null);
        } catch (RuntimeException e) {
            notification.setStatus(NotificationStatus.FAILED);
            String error = String.valueOf(e.getMessage());
            notification.setLastError(error.length() > 1000 ? error.substring(0, 1000) : error);
            log.warn("Notification {} ({}) attempt {} failed: {}", notification.getId(), notification.getEventType(),
                    notification.getAttempts(), error);
        }
        notificationRepo.save(notification);
    }

    private static EventType parseType(String type) throws InvalidEventException {
        try {
            return EventType.valueOf(type);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidEventException("Unknown event type: " + type);
        }
    }

    private static Long parseUserId(Object value) throws InvalidEventException {
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new InvalidEventException("data.userId is required");
        }
    }

    private static String stringOrNull(Object value) {
        return value == null || value.toString().isBlank() ? null : value.toString();
    }
}
