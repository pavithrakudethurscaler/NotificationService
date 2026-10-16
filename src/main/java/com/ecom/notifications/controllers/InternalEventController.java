package com.ecom.notifications.controllers;

import com.ecom.notifications.dtos.EventDto;
import com.ecom.notifications.exceptions.InvalidEventException;
import com.ecom.notifications.services.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Event intake for other services, protected by X-Internal-Token.
 * TODO: replace with Kafka consumers calling the same NotificationService.handleEvent (HLD 3.6)
 */
@RestController
@RequestMapping("/internal/api/v1/events")
public class InternalEventController {
    NotificationService notificationService;

    InternalEventController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<String> receiveEvent(@RequestBody EventDto event) throws InvalidEventException {
        boolean created = notificationService.handleEvent(event);
        return new ResponseEntity<>(created ? "Accepted" : "Duplicate event ignored", HttpStatus.ACCEPTED);
    }
}
