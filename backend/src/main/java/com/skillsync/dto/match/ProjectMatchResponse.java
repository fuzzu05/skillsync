package com.skillsync.dto.match;

import com.skillsync.enums.ProjectStatus;

import java.util.List;

public record ProjectMatchResponse(
        Long projectId,
        String title,
        String summary,
        ProjectStatus status,
        int maxTeamSize,
        long currentTeamSize,
        double matchScore,               // 0.0 to 100.0%
        int matchPercentage,            // Rounded int e.g. 85
        List<SkillMatchDetail> matchedSkills,
        List<SkillMatchDetail> missingSkills,
        boolean isFullyQualified
) {
}
