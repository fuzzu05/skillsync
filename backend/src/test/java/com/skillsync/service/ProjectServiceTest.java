package com.skillsync.service;

import com.skillsync.dto.project.CreateProjectRequest;
import com.skillsync.dto.project.ProjectResponse;
import com.skillsync.dto.project.ProjectSkillRequirementRequest;
import com.skillsync.entity.*;
import com.skillsync.enums.Proficiency;
import com.skillsync.enums.SkillCategory;
import com.skillsync.enums.TeamRole;
import com.skillsync.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectSkillRepository projectSkillRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private ProjectService projectService;

    private User owner;
    private Skill javaSkill;

    @BeforeEach
    void setUp() {
        owner = new User("dev@university.edu", "hash", "Lead Developer", "CS", 3);
        owner.setId(5L);

        javaSkill = new Skill("Java", SkillCategory.BACKEND, "Java Core");
        javaSkill.setId(1L);
    }

    @Test
    @DisplayName("Rule 3: Should atomically create project, team, and designate creator as initial team leader")
    void testCreateProjectAddsOwnerAsTeamLeader() {
        // Arrange
        CreateProjectRequest request = new CreateProjectRequest(
                "Autonomous Rover",
                "Robotics AI Rover",
                "Detailed specs of the campus rover project",
                4,
                List.of(new ProjectSkillRequirementRequest(1L, Proficiency.INTERMEDIATE, true))
        );

        when(userRepository.findById(5L)).thenReturn(Optional.of(owner));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(101L);
            return p;
        });
        when(skillRepository.findById(1L)).thenReturn(Optional.of(javaSkill));
        when(projectSkillRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> {
            Team t = inv.getArgument(0);
            t.setId(201L);
            return t;
        });

        // Act
        ProjectResponse response = projectService.createProject(5L, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(101L);
        assertThat(response.isOwner()).isTrue();
        assertThat(response.isMember()).isTrue();

        // Verify team was created
        ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
        verify(teamRepository).save(teamCaptor.capture());
        Team savedTeam = teamCaptor.getValue();
        assertThat(savedTeam.getName()).contains("Autonomous Rover");

        // Rule 3: Verify owner added as initial LEADER
        ArgumentCaptor<TeamMember> memberCaptor = ArgumentCaptor.forClass(TeamMember.class);
        verify(teamMemberRepository).save(memberCaptor.capture());
        TeamMember savedLeader = memberCaptor.getValue();
        assertThat(savedLeader.getUser().getId()).isEqualTo(5L);
        assertThat(savedLeader.getRole()).isEqualTo(TeamRole.LEADER);
    }
}
