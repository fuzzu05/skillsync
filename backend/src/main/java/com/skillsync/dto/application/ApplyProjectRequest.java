package com.skillsync.dto.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplyProjectRequest(
        @NotBlank(message = "Application pitch is required")
        @Size(max = 2000, message = "Pitch cannot exceed 2000 characters")
        String pitch
) {
}
