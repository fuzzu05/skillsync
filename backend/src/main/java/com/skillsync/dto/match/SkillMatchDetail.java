package com.skillsync.dto.match;

import com.skillsync.enums.Proficiency;
import com.skillsync.enums.SkillCategory;

public record SkillMatchDetail(
        Long skillId,
        String skillName,
        SkillCategory category,
        Proficiency requiredProficiency,
        Proficiency userProficiency,
        boolean isSatisfied,
        boolean isMandatory
) {
}
