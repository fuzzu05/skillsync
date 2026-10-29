package com.skillsync.dto.user;

import com.skillsync.entity.UserSkill;
import com.skillsync.enums.Proficiency;
import com.skillsync.enums.SkillCategory;

import java.math.BigDecimal;

public record UserSkillResponse(
        Long skillId,
        String skillName,
        SkillCategory category,
        Proficiency proficiency,
        BigDecimal yearsOfExperience
) {
    public static UserSkillResponse fromEntity(UserSkill userSkill) {
        return new UserSkillResponse(
                userSkill.getSkill().getId(),
                userSkill.getSkill().getName(),
                userSkill.getSkill().getCategory(),
                userSkill.getProficiency(),
                userSkill.getYearsOfExperience()
        );
    }
}
