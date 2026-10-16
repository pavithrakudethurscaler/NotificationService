package com.ecom.notifications.models;

public enum NotificationStatus {
    /** Stored, not delivered yet */
    PENDING,
    SENT,
    /** Delivery failed; retried by the scheduler until the attempt limit is reached */
    FAILED
}
