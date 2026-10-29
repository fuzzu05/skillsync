package com.skillsync.controller;

import com.skillsync.dto.message.ConversationSummaryResponse;
import com.skillsync.dto.message.MessageResponse;
import com.skillsync.dto.message.SendMessageRequest;
import com.skillsync.security.UserPrincipal;
import com.skillsync.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
@Tag(name = "Direct Messaging", description = "Strict 1-on-1 direct conversations, persistent messaging, and read receipts")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/direct/{recipientId}")
    @Operation(summary = "Get or create direct conversation", description = "Returns the single unique 1-to-1 conversation thread with another student. Enforces canonical user ordering.")
    public ResponseEntity<ConversationSummaryResponse> getOrCreateConversation(
            @PathVariable Long recipientId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ConversationSummaryResponse response = conversationService.getOrCreateDirectConversation(
                principal.getId(), recipientId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List my conversations", description = "Retrieve all 1-to-1 conversation threads for current user with unread counts and latest message preview.")
    public ResponseEntity<List<ConversationSummaryResponse>> getMyConversations(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ConversationSummaryResponse> conversations = conversationService.getUserConversations(principal.getId());
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Get conversation messages", description = "Fetch chronological messages in a conversation. Strictly verifies caller is a participant.")
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        Page<MessageResponse> messages = conversationService.getConversationMessages(id, principal.getId(), pageable);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Send message", description = "Sends and persists a direct message, updates last read timestamp, and triggers an activity notification to recipient.")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SendMessageRequest request
    ) {
        MessageResponse sent = conversationService.sendMessage(id, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(sent);
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark conversation as read", description = "Updates caller's last read receipt timestamp in the conversation.")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        conversationService.markConversationAsRead(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
