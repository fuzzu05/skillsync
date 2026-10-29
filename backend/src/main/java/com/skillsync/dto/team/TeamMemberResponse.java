package com.skillsync.dto.team;

import com.skillsync.entity.TeamMember;
import com.skillsync.enums.TeamRole;

import java.time.Instant;

public record TeamMemberResponse(
        Long userId,
        String fullName,
        String email,
        String department,
        Integer yearOfStudy,
        TeamRole role,
        Instant joinedAt
) {
    public static TeamMemberResponse fromEntity(TeamMember member) {
        return new TeamMemberResponse(
                member.getUser().getId(),
                member.getUser().getFullName(),
                member.getUser().getEmail(),
                member.getUser().getDepartment(),
                member.getUser().getYearOfStudy(),
                member.getRole(),
                member.getJoinedAt()
        );
    }
}
