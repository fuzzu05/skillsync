package com.skillsync.service;

import com.skillsync.dto.project.CreateProjectRequest;
import com.skillsync.dto.project.ProjectResponse;
import com.skillsync.dto.project.ProjectSkillRequirementRequest;
import com.skillsync.dto.project.ProjectSkillResponse;
import com.skillsync.dto.project.UpdateProjectRequest;
import com.skillsync.entity.Project;
import com.skillsync.entity.ProjectSkill;
import com.skillsync.entity.Skill;
import com.skillsync.entity.Team;
import com.skillsync.entity.TeamMember;
import com.skillsync.entity.User;
import com.skillsync.enums.ProjectStatus;
import com.skillsync.enums.TeamRole;
import com.skillsync.exception.AccessDeniedException;
import com.skillsync.exception.ResourceNotFoundException;
import com.skillsync.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectSkillRepository projectSkillRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;

    public ProjectService(ProjectRepository projectRepository,
                          ProjectSkillRepository projectSkillRepository,
                          TeamRepository teamRepository,
                          TeamMemberRepository teamMemberRepository,
                          UserRepository userRepository,
                          SkillRepository skillRepository) {
        this.projectRepository = projectRepository;
        this.projectSkillRepository = projectSkillRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
    }

    /**
     * Rule: When creating a project and its team, automatically add the project owner
     * as the initial team leader within the same database transaction.
     */
    @Transactional
    public ProjectResponse createProject(Long ownerId, CreateProjectRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerId));

        // 1. Create and save Project
        Project project = new Project(
                owner,
                request.title().trim(),
                request.summary().trim(),
                request.description().trim(),
                request.maxTeamSize()
        );
        Project savedProject = projectRepository.save(project);

        // 2. Attach required skills
        List<ProjectSkillResponse> skillResponses = new ArrayList<>();
        if (request.requiredSkills() != null && !request.requiredSkills().isEmpty()) {
            List<ProjectSkill> projectSkills = new ArrayList<>();
            for (ProjectSkillRequirementRequest req : request.requiredSkills()) {
                Skill skill = skillRepository.findById(req.skillId())
                        .orElseThrow(() -> new ResourceNotFoundException("Skill", "id", req.skillId()));

                ProjectSkill ps = new ProjectSkill(savedProject, skill, req.minProficiency(), req.mandatory());
                projectSkills.add(ps);
            }
            List<ProjectSkill> savedSkills = projectSkillRepository.saveAll(projectSkills);
            skillResponses = savedSkills.stream().map(ProjectSkillResponse::fromEntity).toList();
        }

        // 3. Create Team and automatically add Project Owner as initial Team Leader (Same Transaction)
        Team team = new Team(savedProject, savedProject.getTitle() + " Team");
        Team savedTeam = teamRepository.save(team);
        savedProject.setTeam(savedTeam);

        TeamMember leader = new TeamMember(savedTeam, owner, TeamRole.LEADER);
        teamMemberRepository.save(leader);

        return ProjectResponse.fromEntity(savedProject, 1L, skillResponses, true, true);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long projectId, Long currentUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        List<ProjectSkillResponse> skills = projectSkillRepository.findByProjectIdWithSkill(projectId)
                .stream()
                .map(ProjectSkillResponse::fromEntity)
                .toList();

        long memberCount = 0;
        boolean isOwner = false;
        boolean isMember = false;

        if (project.getTeam() != null) {
            memberCount = teamMemberRepository.countByIdTeamId(project.getTeam().getId());
            if (currentUserId != null) {
                isOwner = project.getOwner().getId().equals(currentUserId);
                isMember = teamMemberRepository.existsByIdTeamIdAndIdUserId(project.getTeam().getId(), currentUserId);
            }
        }

        return ProjectResponse.fromEntity(project, memberCount, skills, isOwner, isMember);
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> listProjects(ProjectStatus status, Long skillId, String query, Long currentUserId, Pageable pageable) {
        Page<Project> page;
        if (skillId != null) {
            page = projectRepository.findBySkillIdAndStatus(skillId, status, pageable);
        } else {
            page = projectRepository.searchProjects(query, status, pageable);
        }

        return page.map(p -> {
            List<ProjectSkillResponse> skills = projectSkillRepository.findByProjectIdWithSkill(p.getId())
                    .stream()
                    .map(ProjectSkillResponse::fromEntity)
                    .toList();
            long count = p.getTeam() != null ? teamMemberRepository.countByIdTeamId(p.getTeam().getId()) : 0;
            boolean isOwner = currentUserId != null && p.getOwner().getId().equals(currentUserId);
            boolean isMember = currentUserId != null && p.getTeam() != null &&
                    teamMemberRepository.existsByIdTeamIdAndIdUserId(p.getTeam().getId(), currentUserId);
            return ProjectResponse.fromEntity(p, count, skills, isOwner, isMember);
        });
    }

    @Transactional
    public ProjectResponse updateProject(Long projectId, Long currentUserId, UpdateProjectRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        if (!project.getOwner().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Only the project owner can update project details");
        }

        project.setTitle(request.title().trim());
        project.setSummary(request.summary().trim());
        project.setDescription(request.description().trim());
        if (request.status() != null) {
            project.setStatus(request.status());
        }
        project.setMaxTeamSize(request.maxTeamSize());

        Project saved = projectRepository.save(project);

        List<ProjectSkillResponse> skills = projectSkillRepository.findByProjectIdWithSkill(projectId)
                .stream()
                .map(ProjectSkillResponse::fromEntity)
                .toList();

        long memberCount = saved.getTeam() != null ? teamMemberRepository.countByIdTeamId(saved.getTeam().getId()) : 0;

        return ProjectResponse.fromEntity(saved, memberCount, skills, true, true);
    }

    @Transactional
    public void deleteProject(Long projectId, Long currentUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        if (!project.getOwner().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Only the project owner can delete this project");
        }

        projectRepository.delete(project);
    }
}
