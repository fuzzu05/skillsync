package com.skillsync.service;

import com.skillsync.dto.message.ConversationSummaryResponse;
import com.skillsync.dto.message.MessageResponse;
import com.skillsync.dto.message.SendMessageRequest;
import com.skillsync.entity.Conversation;
import com.skillsync.entity.Message;
import com.skillsync.entity.User;
import com.skillsync.exception.AccessDeniedException;
import com.skillsync.exception.BadRequestException;
import com.skillsync.repository.ConversationRepository;
import com.skillsync.repository.MessageRepository;
import com.skillsync.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ConversationService conversationService;

    private User user1;
    private User user2;
    private User intruder;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        user1 = new User("user1@university.edu", "hash", "User One", "CS", 1);
        user1.setId(10L);

        user2 = new User("user2@university.edu", "hash", "User Two", "IT", 2);
        user2.setId(20L);

        intruder = new User("intruder@university.edu", "hash", "Intruder", "Mech", 3);
        intruder.setId(99L);

        conversation = new Conversation(user1, user2);
        conversation.setId(500L);
    }

    @Test
    @DisplayName("Rule 2: Should enforce canonical ordering when creating direct conversation")
    void testCanonicalUserOrdering() {
        // user2 (20L) calls to message user1 (10L)
        when(userRepository.findById(10L)).thenReturn(Optional.of(user1));
        when(conversationRepository.findByUser1IdAndUser2Id(10L, 20L)).thenReturn(Optional.empty());
        when(userRepository.findById(10L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(20L)).thenReturn(Optional.of(user2));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        ConversationSummaryResponse response = conversationService.getOrCreateDirectConversation(20L, 10L);

        assertThat(response).isNotNull();
        // Conversation repository must be queried with u1=10L, u2=20L
        verify(conversationRepository).findByUser1IdAndUser2Id(10L, 20L);
    }

    @Test
    @DisplayName("Should prevent user from starting a conversation with themselves")
    void testCannotMessageSelf() {
        assertThatThrownBy(() -> conversationService.getOrCreateDirectConversation(10L, 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("conversation with yourself");
    }

    @Test
    @DisplayName("Rule 2: Should throw AccessDeniedException if non-participant tries to read messages")
    void testNonParticipantCannotReadMessages() {
        when(conversationRepository.findById(500L)).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> conversationService.getConversationMessages(500L, 99L, Pageable.unpaged()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not authorized to view messages");
    }

    @Test
    @DisplayName("Rule 2: Should throw AccessDeniedException if non-participant tries to send message")
    void testNonParticipantCannotSendMessage() {
        when(conversationRepository.findById(500L)).thenReturn(Optional.of(conversation));
        SendMessageRequest request = new SendMessageRequest("Secret message");

        assertThatThrownBy(() -> conversationService.sendMessage(500L, 99L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not authorized to send messages");

        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rule 2: Participant can send message and triggers notification to the other participant")
    void testParticipantSendsMessage() {
        when(conversationRepository.findById(500L)).thenReturn(Optional.of(conversation));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user1));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(1001L);
            return m;
        });

        SendMessageRequest request = new SendMessageRequest("Hello from Alice!");
        MessageResponse sent = conversationService.sendMessage(500L, 10L, request);

        assertThat(sent.content()).isEqualTo("Hello from Alice!");
        assertThat(sent.senderId()).isEqualTo(10L);

        // Verify notification sent to user2 (recipient)
        verify(notificationService).createNotification(
                eq(user2), eq(user1), any(), any(), any(), eq(500L)
        );
    }
}
