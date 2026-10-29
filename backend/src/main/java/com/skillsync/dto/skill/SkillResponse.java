package com.skillsync.dto.skill;

import com.skillsync.entity.Skill;
import com.skillsync.enums.SkillCategory;

public record SkillResponse(
        Long id,
        String name,
        SkillCategory category,
        String description
) {
    public static SkillResponse fromEntity(Skill skill) {
        return new SkillResponse(
                skill.getId(),
                skill.getName(),
                skill.getCategory(),
                skill.getDescription()
        );
    }
}
