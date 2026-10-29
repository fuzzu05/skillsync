package com.skillsync.dto.user;

import com.skillsync.entity.User;
import com.skillsync.enums.Role;

import java.time.Instant;
import java.util.List;

public record UserProfileResponse(
        Long id,
        String email,
        String fullName,
        String headline,
        String bio,
        String department,
        Integer yearOfStudy,
        String githubUrl,
        String linkedinUrl,
        Role role,
        Instant createdAt,
        List<UserSkillResponse> skills
) {
    public static UserProfileResponse fromEntity(User user, List<UserSkillResponse> skills) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getHeadline(),
                user.getBio(),
                user.getDepartment(),
                user.getYearOfStudy(),
                user.getGithubUrl(),
                user.getLinkedinUrl(),
                user.getRole(),
                user.getCreatedAt(),
                skills != null ? skills : List.of()
        );
    }
}
