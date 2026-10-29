package com.skillsync.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "conversations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_conversation_participants", columnNames = {"user1_id", "user2_id"})
        }
)
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    @Column(name = "user1_last_read_at")
    private Instant user1LastReadAt;

    @Column(name = "user2_last_read_at")
    private Instant user2LastReadAt;

    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Message> messages = new ArrayList<>();

    public Conversation() {
    }

    public Conversation(User user1, User user2) {
        if (user1.getId() >= user2.getId()) {
            throw new IllegalArgumentException("Canonical ordering violation: user1 ID must be strictly less than user2 ID");
        }
        this.user1 = user1;
        this.user2 = user2;
        this.lastMessageAt = Instant.now();
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (lastMessageAt == null) lastMessageAt = Instant.now();
    }

    public boolean isParticipant(Long userId) {
        return (user1 != null && user1.getId().equals(userId)) ||
               (user2 != null && user2.getId().equals(userId));
    }

    public User getOtherParticipant(Long currentUserId) {
        if (user1.getId().equals(currentUserId)) {
            return user2;
        } else if (user2.getId().equals(currentUserId)) {
            return user1;
        }
        throw new IllegalArgumentException("User " + currentUserId + " is not a participant in conversation " + id);
    }

    public Instant getLastReadAtForUser(Long currentUserId) {
        if (user1.getId().equals(currentUserId)) {
            return user1LastReadAt;
        } else if (user2.getId().equals(currentUserId)) {
            return user2LastReadAt;
        }
        return null;
    }

    public void setLastReadAtForUser(Long currentUserId, Instant timestamp) {
        if (user1.getId().equals(currentUserId)) {
            this.user1LastReadAt = timestamp;
        } else if (user2.getId().equals(currentUserId)) {
            this.user2LastReadAt = timestamp;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser1() {
        return user1;
    }

    public void setUser1(User user1) {
        this.user1 = user1;
    }

    public User getUser2() {
        return user2;
    }

    public void setUser2(User user2) {
        this.user2 = user2;
    }

    public Instant getUser1LastReadAt() {
        return user1LastReadAt;
    }

    public void setUser1LastReadAt(Instant user1LastReadAt) {
        this.user1LastReadAt = user1LastReadAt;
    }

    public Instant getUser2LastReadAt() {
        return user2LastReadAt;
    }

    public void setUser2LastReadAt(Instant user2LastReadAt) {
        this.user2LastReadAt = user2LastReadAt;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
