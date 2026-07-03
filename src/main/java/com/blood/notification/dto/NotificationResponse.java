package com.blood.notification.dto;

import com.blood.notification.model.Notification;

public record NotificationResponse(
        Long id,
        String recipient,
        String message,
        String status,
        String sentAt,
        String type,
        Long donorId,
        Long transferId,
        boolean read
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getRecipient(),
                notification.getMessage(),
                notification.getStatus() != null ? notification.getStatus().name() : null,
                notification.getSentAt() != null ? notification.getSentAt().toString() : null,
                notification.getType(),
                notification.getDonorId(),
                notification.getTransferId(),
                notification.isRead()
        );
    }
}
