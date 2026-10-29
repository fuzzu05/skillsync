package com.skillsync.service;

import com.skillsync.dto.team.TeamMemberResponse;
import com.skillsync.dto.team.TeamResponse;
import com.skillsync.entity.Team;
import com.skillsync.exception.ResourceNotFoundException;
import com.skillsync.repository.TeamMemberRepository;
import com.skillsync.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    public TeamService(TeamRepository teamRepository, TeamMemberRepository teamMemberRepository) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeamByProjectId(Long projectId) {
        Team team = teamRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Team for project", "projectId", projectId));

        List<TeamMemberResponse> members = teamMemberRepository.findByTeamIdWithUser(team.getId())
                .stream()
                .map(TeamMemberResponse::fromEntity)
                .toList();

        return TeamResponse.fromEntity(team, members);
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeamById(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        List<TeamMemberResponse> members = teamMemberRepository.findByTeamIdWithUser(teamId)
                .stream()
                .map(TeamMemberResponse::fromEntity)
                .toList();

        return TeamResponse.fromEntity(team, members);
    }

    @Transactional(readOnly = true)
    public boolean isUserInTeam(Long teamId, Long userId) {
        return teamMemberRepository.existsByIdTeamIdAndIdUserId(teamId, userId);
    }
}
