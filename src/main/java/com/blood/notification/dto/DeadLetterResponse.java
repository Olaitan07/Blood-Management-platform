package com.blood.notification.dto;

import com.blood.notification.model.NotificationDeadLetter;

public record DeadLetterResponse(
        Long id,
        Long notificationId,
        String recipient,
        String message,
        String reason,
        String createdAt
) {

    public static DeadLetterResponse from(NotificationDeadLetter dl) {
        return new DeadLetterResponse(
                dl.getId(),
                dl.getNotification().getId(),
                dl.getNotification().getRecipient(),
                dl.getNotification().getMessage(),
                dl.getReason(),
                dl.getCreatedAt() != null ? dl.getCreatedAt().toString() : null
        );
    }
}
