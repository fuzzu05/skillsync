package com.skillsync.repository;

import com.skillsync.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);

    @Query("SELECT c FROM Conversation c " +
           "JOIN FETCH c.user1 " +
           "JOIN FETCH c.user2 " +
           "WHERE c.user1.id = :userId OR c.user2.id = :userId " +
           "ORDER BY c.lastMessageAt DESC")
    List<Conversation> findUserConversations(@Param("userId") Long userId);
}
