package com.skillsync.dto.project;

import com.skillsync.enums.Proficiency;
import jakarta.validation.constraints.NotNull;

public record ProjectSkillRequirementRequest(
        @NotNull(message = "Skill ID is required")
        Long skillId,

        @NotNull(message = "Minimum proficiency is required")
        Proficiency minProficiency,

        Boolean isMandatory
) {
    public boolean mandatory() {
        return isMandatory == null || isMandatory;
    }
}
