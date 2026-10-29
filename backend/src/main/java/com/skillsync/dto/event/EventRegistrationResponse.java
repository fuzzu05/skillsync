package com.skillsync.dto.event;

import com.skillsync.entity.EventRegistration;
import com.skillsync.enums.EventRegistrationStatus;

import java.time.Instant;

public record EventRegistrationResponse(
        Long eventId,
        String eventTitle,
        Long userId,
        String userFullName,
        EventRegistrationStatus status,
        Instant registeredAt
) {
    public static EventRegistrationResponse fromEntity(EventRegistration reg) {
        return new EventRegistrationResponse(
                reg.getEvent().getId(),
                reg.getEvent().getTitle(),
                reg.getUser().getId(),
                reg.getUser().getFullName(),
                reg.getStatus(),
                reg.getRegisteredAt()
        );
    }
}
