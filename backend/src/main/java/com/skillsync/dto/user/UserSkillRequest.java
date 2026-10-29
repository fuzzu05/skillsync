package com.skillsync.dto.user;

import com.skillsync.enums.Proficiency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UserSkillRequest(
        @NotNull(message = "Skill ID is required")
        Long skillId,

        @NotNull(message = "Proficiency level is required")
        Proficiency proficiency,

        @DecimalMin(value = "0.0", message = "Years of experience cannot be negative")
        BigDecimal yearsOfExperience
) {
}
