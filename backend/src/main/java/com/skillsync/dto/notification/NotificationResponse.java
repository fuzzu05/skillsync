package com.skillsync.dto.notification;

import com.skillsync.entity.Notification;
import com.skillsync.enums.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        Long referenceId,
        boolean isRead,
        Instant createdAt,
        Long senderId,
        String senderName
) {
    public static NotificationResponse fromEntity(Notification notification) {
        Long senderId = notification.getSender() != null ? notification.getSender().getId() : null;
        String senderName = notification.getSender() != null ? notification.getSender().getFullName() : null;

        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getCreatedAt(),
                senderId,
                senderName
        );
    }
}
