package com.ecom.notifications.dtos;

import com.ecom.notifications.models.EventType;
import com.ecom.notifications.models.Notification;
import com.ecom.notifications.models.NotificationStatus;

/** What a customer sees in their notification list. The body is included; the email address is not. */
public record NotificationResponseDto(Long id, EventType type, String subject, String body,
                                      NotificationStatus status, long createdAt, Long sentAt) {
    public static NotificationResponseDto from(Notification n) {
        return new NotificationResponseDto(n.getId(), n.getEventType(), n.getSubject(), n.getBody(),
                n.getStatus(), n.getCreatedAt(), n.getSentAt());
    }
}
