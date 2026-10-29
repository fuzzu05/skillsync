package com.skillsync.controller;

import com.skillsync.dto.notification.NotificationResponse;
import com.skillsync.security.UserPrincipal;
import com.skillsync.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "User activity alerts, team invites, task assignments, and read status")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List notifications", description = "View notifications for current user with optional unreadOnly filter.")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @RequestParam(required = false) Boolean unreadOnly,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(
                principal.getId(), unreadOnly, pageable
        );
        return ResponseEntity.ok(notifications);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Mark a specific notification as read.")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        notificationService.markAsRead(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read", description = "Mark all pending notifications for current user as read.")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        int count = notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.ok(Map.of("markedReadCount", count));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread count badge", description = "Returns total count of unread notifications for badge display.")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        long count = notificationService.getUnreadCount(principal.getId());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
}
