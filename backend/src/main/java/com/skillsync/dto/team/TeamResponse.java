package com.skillsync.dto.team;

import com.skillsync.entity.Team;

import java.util.List;

public record TeamResponse(
        Long teamId,
        Long projectId,
        String projectTitle,
        String teamName,
        List<TeamMemberResponse> members
) {
    public static TeamResponse fromEntity(Team team, List<TeamMemberResponse> members) {
        return new TeamResponse(
                team.getId(),
                team.getProject().getId(),
                team.getProject().getTitle(),
                team.getName(),
                members != null ? members : List.of()
        );
    }
}
