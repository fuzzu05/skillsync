package com.skillsync.dto.skill;

import com.skillsync.enums.SkillCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSkillRequest(
        @NotBlank(message = "Skill name is required")
        @Size(max = 80, message = "Skill name cannot exceed 80 characters")
        String name,

        @NotNull(message = "Skill category is required")
        SkillCategory category,

        @Size(max = 255, message = "Description cannot exceed 255 characters")
        String description
) {
}
