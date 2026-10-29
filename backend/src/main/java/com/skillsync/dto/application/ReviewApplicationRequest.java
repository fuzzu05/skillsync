package com.skillsync.dto.application;

import com.skillsync.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewApplicationRequest(
        @NotNull(message = "Decision status is required")
        ApplicationStatus status,

        @Size(max = 255, message = "Rejection reason cannot exceed 255 characters")
        String rejectionReason
) {
}
