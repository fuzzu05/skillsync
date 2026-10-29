package com.skillsync.dto.event;

import com.skillsync.enums.EventType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateEventRequest(
        @NotBlank(message = "Title cannot be blank")
        @Size(max = 150, message = "Title cannot exceed 150 characters")
        String title,

        @NotBlank(message = "Description cannot be blank")
        String description,

        @NotBlank(message = "Location cannot be blank")
        @Size(max = 150, message = "Location cannot exceed 150 characters")
        String location,

        @NotNull(message = "Event type is required")
        EventType eventType,

        @NotNull(message = "Start time is required")
        Instant startTime,

        @NotNull(message = "End time is required")
        Instant endTime,

        @Min(value = 1, message = "Capacity must be at least 1")
        Integer capacity
) {
}
