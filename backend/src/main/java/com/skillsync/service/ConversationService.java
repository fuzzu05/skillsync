package com.skillsync.service;

import com.skillsync.dto.message.ConversationSummaryResponse;
import com.skillsync.dto.message.MessageResponse;
import com.skillsync.dto.message.SendMessageRequest;
import com.skillsync.dto.user.UserProfileResponse;
import com.skillsync.entity.Conversation;
import com.skillsync.entity.Message;
import com.skillsync.entity.User;
import com.skillsync.enums.NotificationType;
import com.skillsync.exception.AccessDeniedException;
import com.skillsync.exception.BadRequestException;
import com.skillsync.exception.ResourceNotFoundException;
import com.skillsync.repository.ConversationRepository;
import com.skillsync.repository.MessageRepository;
import com.skillsync.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ConversationService(ConversationRepository conversationRepository,
                               MessageRepository messageRepository,
                               UserRepository userRepository,
                               NotificationService notificationService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * Rule: Canonical 1-to-1 conversation guarantee.
     * User with lower ID is always user1 to eliminate duplicate threads.
     */
    @Transactional
    public ConversationSummaryResponse getOrCreateDirectConversation(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new BadRequestException("You cannot start a direct conversation with yourself");
        }

        userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        Long u1Id = Math.min(currentUserId, targetUserId);
        Long u2Id = Math.max(currentUserId, targetUserId);

        Conversation conversation = conversationRepository.findByUser1IdAndUser2Id(u1Id, u2Id)
                .orElseGet(() -> {
                    User u1 = userRepository.findById(u1Id)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", u1Id));
                    User u2 = userRepository.findById(u2Id)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", u2Id));
                    return conversationRepository.save(new Conversation(u1, u2));
                });

        return buildSummary(conversation, currentUserId);
    }

    /**
     * Rule: Enforce conversation-participant authorization for every messaging operation.
     */
    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getUserConversations(Long currentUserId) {
        List<Conversation> conversations = conversationRepository.findUserConversations(currentUserId);
        List<ConversationSummaryResponse> summaries = new ArrayList<>();

        for (Conversation c : conversations) {
            summaries.add(buildSummary(c, currentUserId));
        }

        return summaries;
    }

    /**
     * Rule: Enforce conversation-participant authorization for reading messages.
     */
    @Transactional(readOnly = true)
    public Page<MessageResponse> getConversationMessages(Long conversationId, Long currentUserId, Pageable pageable) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        if (!conversation.isParticipant(currentUserId)) {
            throw new AccessDeniedException("You are not authorized to view messages in this conversation");
        }

        return messageRepository.findByConversationId(conversationId, pageable)
                .map(MessageResponse::fromEntity);
    }

    /**
     * Rule: Enforce conversation-participant authorization for sending messages.
     * Persists message, updates conversation last_message_at, sender last_read_at, and notifies recipient.
     */
    @Transactional
    public MessageResponse sendMessage(Long conversationId, Long senderId, SendMessageRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        if (!conversation.isParticipant(senderId)) {
            throw new AccessDeniedException("You are not authorized to send messages in this conversation");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", senderId));

        Message message = new Message(conversation, sender, request.content().trim());
        Message saved = messageRepository.save(message);

        Instant now = Instant.now();
        conversation.setLastMessageAt(now);
        conversation.setLastReadAtForUser(senderId, now);
        conversationRepository.save(conversation);

        // Notify the other participant
        User recipient = conversation.getOtherParticipant(senderId);
        String preview = request.content().length() > 60
                ? request.content().substring(0, 57) + "..."
                : request.content();

        notificationService.createNotification(
                recipient,
                sender,
                NotificationType.NEW_MESSAGE,
                "New message from " + sender.getFullName(),
                preview,
                conversation.getId()
        );

        return MessageResponse.fromEntity(saved);
    }

    /**
     * Rule: Enforce conversation-participant authorization for read receipts.
     */
    @Transactional
    public void markConversationAsRead(Long conversationId, Long currentUserId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

        if (!conversation.isParticipant(currentUserId)) {
            throw new AccessDeniedException("You are not a participant in this conversation");
        }

        conversation.setLastReadAtForUser(currentUserId, Instant.now());
        conversationRepository.save(conversation);
    }

    private ConversationSummaryResponse buildSummary(Conversation c, Long currentUserId) {
        User partner = c.getOtherParticipant(currentUserId);
        UserProfileResponse partnerDto = UserProfileResponse.fromEntity(partner, List.of());

        Optional<Message> latestOpt = messageRepository.findTopByConversationIdOrderBySentAtDesc(c.getId());
        String lastContent = latestOpt.map(Message::getContent).orElse(null);
        Instant lastSentAt = latestOpt.map(Message::getSentAt).orElse(c.getLastMessageAt());

        Instant myLastRead = c.getLastReadAtForUser(currentUserId);
        long unreadCount = messageRepository.countUnreadMessages(c.getId(), currentUserId, myLastRead);

        return new ConversationSummaryResponse(
                c.getId(),
                partnerDto,
                lastContent,
                lastSentAt,
                unreadCount,
                myLastRead
        );
    }
}
