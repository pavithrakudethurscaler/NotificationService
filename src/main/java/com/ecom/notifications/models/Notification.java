package com.ecom.notifications.models;

import com.ecom.common.models.JpaBaseModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Notification extends JpaBaseModel {
    /** Producer's event id; unique so a repeated event is never delivered twice */
    @Column(unique = true, nullable = false)
    private String eventId;
    @Enumerated(EnumType.STRING)
    private EventType eventType;
    /** Recipient's id in the User Service */
    private Long userId;
    private String recipientEmail;
    private String subject;
    @Column(length = 4000)
    private String body;
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;
    private int attempts;
    @Column(length = 1000)
    private String lastError;
    private long createdAt;
    private Long sentAt;
}
