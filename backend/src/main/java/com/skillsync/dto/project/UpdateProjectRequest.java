package com.skillsync.dto.project;

import com.skillsync.enums.ProjectStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @NotBlank(message = "Title cannot be blank")
        @Size(max = 150, message = "Title cannot exceed 150 characters")
        String title,

        @NotBlank(message = "Summary cannot be blank")
        @Size(max = 255, message = "Summary cannot exceed 255 characters")
        String summary,

        @NotBlank(message = "Description cannot be blank")
        String description,

        ProjectStatus status,

        @Min(value = 1, message = "Max team size must be at least 1")
        @Max(value = 20, message = "Max team size cannot exceed 20")
        int maxTeamSize
) {
}
