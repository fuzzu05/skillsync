package com.skillsync.dto.application;

import com.skillsync.dto.user.UserProfileResponse;
import com.skillsync.dto.user.UserSkillResponse;
import com.skillsync.entity.ProjectApplication;
import com.skillsync.enums.ApplicationStatus;

import java.time.Instant;
import java.util.List;

public record ProjectApplicationResponse(
        Long id,
        Long projectId,
        String projectTitle,
        UserProfileResponse applicant,
        String pitch,
        ApplicationStatus status,
        String rejectionReason,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProjectApplicationResponse fromEntity(
            ProjectApplication app,
            List<UserSkillResponse> applicantSkills
    ) {
        UserProfileResponse applicantDto = UserProfileResponse.fromEntity(app.getApplicant(), applicantSkills);

        return new ProjectApplicationResponse(
                app.getId(),
                app.getProject().getId(),
                app.getProject().getTitle(),
                applicantDto,
                app.getPitch(),
                app.getStatus(),
                app.getRejectionReason(),
                app.getReviewedAt(),
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }
}
