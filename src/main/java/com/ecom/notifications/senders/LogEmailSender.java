package com.ecom.notifications.senders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Development sender: writes emails to the log instead of sending them.
 * Not for production: emails can contain password reset links.
 */
@Component
@ConditionalOnProperty(name = "notification.email.sender", havingValue = "log", matchIfMissing = true)
public class LogEmailSender implements EmailSender {
    private static final Logger log = LoggerFactory.getLogger(LogEmailSender.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("[DEV EMAIL] to={} subject=\"{}\"\n{}", to, subject, body);
    }
}
