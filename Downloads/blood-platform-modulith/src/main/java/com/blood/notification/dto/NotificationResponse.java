package com.blood.notification.dto;

import com.blood.notification.model.Notification;

public record NotificationResponse(
        Long id,
        String message,
        String sentAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getMessage(),
                notification.getSentAt() != null ? notification.getSentAt().toString() : null
        );
    }
}
