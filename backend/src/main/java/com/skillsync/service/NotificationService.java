package com.skillsync.service;

import com.skillsync.dto.notification.NotificationResponse;
import com.skillsync.entity.Notification;
import com.skillsync.entity.User;
import com.skillsync.enums.NotificationType;
import com.skillsync.exception.AccessDeniedException;
import com.skillsync.exception.ResourceNotFoundException;
import com.skillsync.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification createNotification(User recipient, User sender, NotificationType type,
                                           String title, String message, Long referenceId) {
        // Do not notify self
        if (sender != null && recipient.getId().equals(sender.getId())) {
            return null;
        }

        Notification notification = new Notification(
                recipient,
                sender,
                type,
                title,
                message,
                referenceId
        );

        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(Long userId, Boolean unreadOnly, Pageable pageable) {
        if (Boolean.TRUE.equals(unreadOnly)) {
            return notificationRepository.findUnreadByRecipientId(userId, pageable)
                    .map(NotificationResponse::fromEntity);
        }
        return notificationRepository.findByRecipientId(userId, pageable)
                .map(NotificationResponse::fromEntity);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have access to this notification");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }
}
