package com.skillsync.service;

import com.skillsync.dto.application.ApplyProjectRequest;
import com.skillsync.dto.application.ProjectApplicationResponse;
import com.skillsync.dto.application.ReviewApplicationRequest;
import com.skillsync.entity.Project;
import com.skillsync.entity.ProjectApplication;
import com.skillsync.entity.Team;
import com.skillsync.entity.User;
import com.skillsync.enums.ApplicationStatus;
import com.skillsync.enums.ProjectStatus;
import com.skillsync.exception.AccessDeniedException;
import com.skillsync.exception.BadRequestException;
import com.skillsync.exception.DuplicateResourceException;
import com.skillsync.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectApplicationServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectApplicationRepository applicationRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserSkillRepository userSkillRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ProjectApplicationService applicationService;

    private User owner;
    private User applicant;
    private Project project;
    private Team team;

    @BeforeEach
    void setUp() {
        owner = new User("owner@university.edu", "hash", "Prof Owner", "CS", 4);
        owner.setId(1L);

        applicant = new User("applicant@university.edu", "hash", "Student Applicant", "CS", 2);
        applicant.setId(2L);

        project = new Project(owner, "Robotics Drone", "Autonomous drone", "Description", 2);
        project.setId(10L);

        team = new Team(project, "Robotics Drone Team");
        team.setId(100L);
        project.setTeam(team);
    }

    @Test
    @DisplayName("Rule 1: Should reject duplicate applications even if applicant exists")
    void testStrictSingleApplicationRule() {
        // Arrange
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(2L)).thenReturn(Optional.of(applicant));
        when(applicationRepository.existsByProjectIdAndApplicantId(10L, 2L)).thenReturn(true);

        ApplyProjectRequest request = new ApplyProjectRequest("I love robotics and want to build the drone");

        // Act & Assert
        assertThatThrownBy(() -> applicationService.applyToProject(10L, 2L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Only one application per student per project is permitted");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should prevent project owner from applying to own project")
    void testOwnerCannotApply() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        ApplyProjectRequest request = new ApplyProjectRequest("Applying to my own project");

        assertThatThrownBy(() -> applicationService.applyToProject(10L, 1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot apply to your own project");
    }

    @Test
    @DisplayName("Rule 1: Should safely enforce team capacity during concurrent application acceptance")
    void testSafeCapacityEnforcementOnAcceptance() {
        // Project maxTeamSize is 2.
        // If current team size is already 2, acceptance must be rejected
        ProjectApplication app = new ProjectApplication(project, applicant, "Great pitch");
        app.setId(50L);

        when(projectRepository.findByIdWithPessimisticLock(10L)).thenReturn(Optional.of(project));
        when(applicationRepository.findById(50L)).thenReturn(Optional.of(app));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(teamMemberRepository.countByIdTeamId(100L)).thenReturn(2L); // Already at max (2)

        ReviewApplicationRequest request = new ReviewApplicationRequest(ApplicationStatus.ACCEPTED, null);

        assertThatThrownBy(() -> applicationService.reviewApplication(10L, 50L, 1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Team has already reached maximum capacity of 2");

        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should auto-transition project status to IN_PROGRESS when team reaches max capacity")
    void testAutoTransitionToInProgressWhenFull() {
        ProjectApplication app = new ProjectApplication(project, applicant, "Pitch");
        app.setId(50L);

        when(projectRepository.findByIdWithPessimisticLock(10L)).thenReturn(Optional.of(project));
        when(applicationRepository.findById(50L)).thenReturn(Optional.of(app));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(teamMemberRepository.countByIdTeamId(100L)).thenReturn(1L); // 1 existing member + 1 new = 2 (max)
        when(applicationRepository.save(any(ProjectApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewApplicationRequest request = new ReviewApplicationRequest(ApplicationStatus.ACCEPTED, null);

        ProjectApplicationResponse response = applicationService.reviewApplication(10L, 50L, 1L, request);

        assertThat(response.status()).isEqualTo(ApplicationStatus.ACCEPTED);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        verify(projectRepository).save(project);
        verify(teamMemberRepository).save(any());
    }

    @Test
    @DisplayName("Should reject review by non-owner")
    void testNonOwnerCannotReview() {
        when(projectRepository.findByIdWithPessimisticLock(10L)).thenReturn(Optional.of(project));
        ReviewApplicationRequest request = new ReviewApplicationRequest(ApplicationStatus.ACCEPTED, null);

        assertThatThrownBy(() -> applicationService.reviewApplication(10L, 50L, 999L, request))
                .isInstanceOf(AccessDeniedException.class);
    }
}
