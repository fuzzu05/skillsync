package com.skillsync.dto.project;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CreateProjectRequest(
        @NotBlank(message = "Project title is required")
        @Size(max = 150, message = "Title cannot exceed 150 characters")
        String title,

        @NotBlank(message = "Summary is required")
        @Size(max = 255, message = "Summary cannot exceed 255 characters")
        String summary,

        @NotBlank(message = "Description is required")
        String description,

        @Min(value = 1, message = "Team size must be at least 1")
        @Max(value = 20, message = "Team size cannot exceed 20")
        int maxTeamSize,

        @Valid
        List<ProjectSkillRequirementRequest> requiredSkills
) {
}
