package com.skillsync.repository;

import com.skillsync.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m JOIN FETCH m.sender " +
           "WHERE m.conversation.id = :conversationId " +
           "ORDER BY m.sentAt ASC")
    Page<Message> findByConversationId(@Param("conversationId") Long conversationId, Pageable pageable);

    Optional<Message> findTopByConversationIdOrderBySentAtDesc(Long conversationId);

    long countByConversationIdAndSenderIdNot(Long conversationId, Long senderId);

    long countByConversationIdAndSenderIdNotAndSentAtAfter(Long conversationId, Long senderId, Instant sentAt);

    default long countUnreadMessages(Long conversationId, Long userId, Instant lastReadAt) {
        if (lastReadAt == null) {
            return countByConversationIdAndSenderIdNot(conversationId, userId);
        }
        return countByConversationIdAndSenderIdNotAndSentAtAfter(conversationId, userId, lastReadAt);
    }
}
