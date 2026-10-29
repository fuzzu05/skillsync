package com.skillsync.repository;

import com.skillsync.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.sender " +
           "WHERE n.recipient.id = :recipientId " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientId(@Param("recipientId") Long recipientId, Pageable pageable);

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.sender " +
           "WHERE n.recipient.id = :recipientId AND n.read = false " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findUnreadByRecipientId(@Param("recipientId") Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadFalse(Long recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipient.id = :recipientId AND n.read = false")
    int markAllAsRead(@Param("recipientId") Long recipientId);
}
