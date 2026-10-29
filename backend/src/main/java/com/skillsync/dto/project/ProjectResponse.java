package com.skillsync.dto.project;

import com.skillsync.dto.user.UserProfileResponse;
import com.skillsync.entity.Project;
import com.skillsync.enums.ProjectStatus;

import java.time.Instant;
import java.util.List;

public record ProjectResponse(
        Long id,
        String title,
        String summary,
        String description,
        ProjectStatus status,
        int maxTeamSize,
        long currentTeamSize,
        UserProfileResponse owner,
        Long teamId,
        List<ProjectSkillResponse> requiredSkills,
        boolean isOwner,
        boolean isMember,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProjectResponse fromEntity(
            Project project,
            long currentTeamSize,
            List<ProjectSkillResponse> requiredSkills,
            boolean isOwner,
            boolean isMember
    ) {
        UserProfileResponse ownerDto = UserProfileResponse.fromEntity(project.getOwner(), List.of());
        Long teamId = project.getTeam() != null ? project.getTeam().getId() : null;

        return new ProjectResponse(
                project.getId(),
                project.getTitle(),
                project.getSummary(),
                project.getDescription(),
                project.getStatus(),
                project.getMaxTeamSize(),
                currentTeamSize,
                ownerDto,
                teamId,
                requiredSkills != null ? requiredSkills : List.of(),
                isOwner,
                isMember,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
