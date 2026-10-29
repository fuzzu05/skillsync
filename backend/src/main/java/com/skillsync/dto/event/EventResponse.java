package com.skillsync.dto.event;

import com.skillsync.dto.user.UserProfileResponse;
import com.skillsync.entity.Event;
import com.skillsync.enums.EventType;

import java.time.Instant;
import java.util.List;

public record EventResponse(
        Long id,
        String title,
        String description,
        String location,
        EventType eventType,
        Instant startTime,
        Instant endTime,
        Integer capacity,
        long registeredCount,
        boolean isRegistered,
        boolean isOrganizer,
        UserProfileResponse organizer,
        Instant createdAt
) {
    public static EventResponse fromEntity(Event event, long registeredCount, boolean isRegistered, boolean isOrganizer) {
        UserProfileResponse organizerDto = UserProfileResponse.fromEntity(event.getOrganizer(), List.of());

        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getEventType(),
                event.getStartTime(),
                event.getEndTime(),
                event.getCapacity(),
                registeredCount,
                isRegistered,
                isOrganizer,
                organizerDto,
                event.getCreatedAt()
        );
    }
}
