package com.skillsync.dto.message;

import com.skillsync.dto.user.UserProfileResponse;

import java.time.Instant;

public record ConversationSummaryResponse(
        Long conversationId,
        UserProfileResponse partner,
        String lastMessageContent,
        Instant lastMessageSentAt,
        long unreadCount,
        Instant myLastReadAt
) {
}
