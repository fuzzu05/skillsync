package com.skillsync.dto.project;

import com.skillsync.entity.ProjectSkill;
import com.skillsync.enums.Proficiency;
import com.skillsync.enums.SkillCategory;

public record ProjectSkillResponse(
        Long skillId,
        String skillName,
        SkillCategory category,
        Proficiency minProficiency,
        boolean isMandatory
) {
    public static ProjectSkillResponse fromEntity(ProjectSkill projectSkill) {
        return new ProjectSkillResponse(
                projectSkill.getSkill().getId(),
                projectSkill.getSkill().getName(),
                projectSkill.getSkill().getCategory(),
                projectSkill.getMinProficiency(),
                projectSkill.isMandatory()
        );
    }
}
