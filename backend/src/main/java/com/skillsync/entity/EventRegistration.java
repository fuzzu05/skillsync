package com.skillsync.entity;

import com.skillsync.enums.EventRegistrationStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "event_registrations")
public class EventRegistration {

    @EmbeddedId
    private EventRegistrationId id = new EventRegistrationId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("eventId")
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventRegistrationStatus status = EventRegistrationStatus.REGISTERED;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt = Instant.now();

    public EventRegistration() {
    }

    public EventRegistration(Event event, User user) {
        this.event = event;
        this.user = user;
        this.id = new EventRegistrationId(event.getId(), user.getId());
        this.status = EventRegistrationStatus.REGISTERED;
        this.registeredAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (registeredAt == null) registeredAt = Instant.now();
    }

    // Getters and Setters
    public EventRegistrationId getId() {
        return id;
    }

    public void setId(EventRegistrationId id) {
        this.id = id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
        if (event != null && event.getId() != null) {
            this.id.setEventId(event.getId());
        }
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null && user.getId() != null) {
            this.id.setUserId(user.getId());
        }
    }

    public EventRegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(EventRegistrationStatus status) {
        this.status = status;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }
}
