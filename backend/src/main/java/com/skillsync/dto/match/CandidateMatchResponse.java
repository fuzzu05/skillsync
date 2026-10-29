package com.skillsync.dto.match;

import java.util.List;

public record CandidateMatchResponse(
        Long candidateId,
        String fullName,
        String headline,
        String department,
        Integer yearOfStudy,
        String githubUrl,
        String linkedinUrl,
        double matchScore,               // 0.0 to 100.0%
        int matchPercentage,            // Rounded int e.g. 85
        List<SkillMatchDetail> matchedSkills,
        List<SkillMatchDetail> missingSkills,
        boolean isFullyQualified
) {
}
