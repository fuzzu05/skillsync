package com.skillsync.service;

import com.skillsync.entity.Notification;
import com.skillsync.entity.User;
import com.skillsync.enums.NotificationType;
import com.skillsync.exception.AccessDeniedException;
import com.skillsync.exception.ResourceNotFoundException;
import com.skillsync.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User recipient;
    private User sender;
    private Notification notification;

    @BeforeEach
    void setUp() {
        recipient = new User("recipient@university.edu", "hash", "Recipient User", "CS", 2);
        recipient.setId(10L);

        sender = new User("sender@university.edu", "hash", "Sender User", "IT", 3);
        sender.setId(20L);

        notification = new Notification(
                recipient,
                sender,
                NotificationType.APPLICATION_ACCEPTED,
                "Accepted!",
                "You are accepted",
                100L
        );
        notification.setId(1L);
    }

    @Test
    @DisplayName("Should suppress notification when recipient is identical to sender")
    void testCreateNotificationAvoidsSelfNotification() {
        Notification result = notificationService.createNotification(
                recipient,
                recipient, // self
                NotificationType.TASK_ASSIGNED,
                "Self assigned",
                "Self assigned message",
                50L
        );

        assertThat(result).isNull();
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully create and persist notification for recipient")
    void testCreateNotificationSavesProperly() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        Notification result = notificationService.createNotification(
                recipient,
                sender,
                NotificationType.NEW_MESSAGE,
                "New Message",
                "Hello!",
                200L
        );

        assertThat(result).isNotNull();
        assertThat(result.getRecipient().getId()).isEqualTo(10L);
        assertThat(result.getSender().getId()).isEqualTo(20L);
        assertThat(result.getType()).isEqualTo(NotificationType.NEW_MESSAGE);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should reject mark-as-read when caller is not the recipient")
    void testMarkAsReadRejectsUnauthorizedUser() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 999L)) // unauthorized caller
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("do not have access to this notification");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should mark notification as read when caller is recipient")
    void testMarkAsReadSucceedsForRecipient() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.markAsRead(1L, 10L); // recipient caller

        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("Should delegate mark-all-as-read to repository and return count")
    void testMarkAllAsReadDelegatesToRepository() {
        when(notificationRepository.markAllAsRead(10L)).thenReturn(4);

        int updatedCount = notificationService.markAllAsRead(10L);

        assertThat(updatedCount).isEqualTo(4);
        verify(notificationRepository).markAllAsRead(10L);
    }

    @Test
    @DisplayName("Should delegate unread count to repository")
    void testGetUnreadCountDelegatesToRepository() {
        when(notificationRepository.countByRecipientIdAndReadFalse(10L)).thenReturn(3L);

        long unreadCount = notificationService.getUnreadCount(10L);

        assertThat(unreadCount).isEqualTo(3L);
        verify(notificationRepository).countByRecipientIdAndReadFalse(10L);
    }
}
