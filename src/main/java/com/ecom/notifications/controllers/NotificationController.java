package com.ecom.notifications.controllers;

import com.ecom.notifications.auth.AuthInterceptor;
import com.ecom.notifications.auth.AuthUserDto;
import com.ecom.notifications.dtos.NotificationResponseDto;
import com.ecom.notifications.services.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    NotificationService notificationService;

    NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** The caller's own notifications, newest first (an in-app inbox) */
    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getNotifications(
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) AuthUserDto user) {
        List<NotificationResponseDto> notifications = notificationService.getNotifications(user.getId()).stream()
                .map(NotificationResponseDto::from)
                .toList();
        return new ResponseEntity<>(notifications, HttpStatus.OK);
    }
}
