package com.ecom.notifications.senders;

public interface EmailSender {
    /** Throws on failure so the notification can be marked FAILED and retried */
    void send(String to, String subject, String body);
}
