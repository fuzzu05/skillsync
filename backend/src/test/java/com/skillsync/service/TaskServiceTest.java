package com.skillsync.service;

import com.skillsync.dto.task.CreateTaskRequest;
import com.skillsync.dto.task.TaskResponse;
import com.skillsync.dto.task.UpdateTaskRequest;
import com.skillsync.dto.task.UpdateTaskStatusRequest;
import com.skillsync.entity.Project;
import com.skillsync.entity.Task;
import com.skillsync.entity.Team;
import com.skillsync.entity.TeamMember;
import com.skillsync.entity.User;
import com.skillsync.enums.NotificationType;
import com.skillsync.enums.TaskPriority;
import com.skillsync.enums.TaskStatus;
import com.skillsync.enums.TeamRole;
import com.skillsync.exception.AccessDeniedException;
import com.skillsync.exception.BadRequestException;
import com.skillsync.repository.TaskRepository;
import com.skillsync.repository.TeamMemberRepository;
import com.skillsync.repository.TeamRepository;
import com.skillsync.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TaskService taskService;

    private User creator;
    private User assignee;
    private User intruder;
    private Team team;
    private Task task;

    @BeforeEach
    void setUp() {
        creator = new User("creator@university.edu", "hash", "Creator User", "CS", 3);
        creator.setId(1L);

        assignee = new User("assignee@university.edu", "hash", "Assignee User", "CS", 2);
        assignee.setId(2L);

        intruder = new User("intruder@university.edu", "hash", "Intruder User", "EE", 1);
        intruder.setId(99L);

        Project project = new Project(creator, "Project Title", "Summary", "Description", 4);
        project.setId(10L);

        team = new Team(project, "Project Team");
        team.setId(100L);
        project.setTeam(team);

        task = new Task(team, creator, assignee, "Build REST API", "Implement endpoints", TaskPriority.HIGH, Instant.now().plusSeconds(86400));
        task.setId(500L);
    }

    @Test
    @DisplayName("Should reject task creation if creator is not a member of the team")
    void testCreateTaskRejectsNonTeamMemberCreator() {
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team));
        when(teamMemberRepository.existsByIdTeamIdAndIdUserId(100L, 99L)).thenReturn(false);

        CreateTaskRequest request = new CreateTaskRequest("New Task", "Desc", TaskPriority.MEDIUM, null, null);

        assertThatThrownBy(() -> taskService.createTask(100L, 99L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("must be an active member of the team to create tasks");

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject task creation if assignee is not a member of the team")
    void testCreateTaskRejectsNonTeamMemberAssignee() {
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team));
        when(teamMemberRepository.existsByIdTeamIdAndIdUserId(100L, 1L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(teamMemberRepository.existsByIdTeamIdAndIdUserId(100L, 99L)).thenReturn(false);

        CreateTaskRequest request = new CreateTaskRequest("New Task", "Desc", TaskPriority.MEDIUM, 99L, null);

        assertThatThrownBy(() -> taskService.createTask(100L, 1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Assignee must be an active member of this team");

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully create task and notify assignee")
    void testCreateTaskSuccessfullyWithAssigneeNotifiesAssignee() {
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team));
        when(teamMemberRepository.existsByIdTeamIdAndIdUserId(100L, 1L)).thenReturn(true);
        when(teamMemberRepository.existsByIdTeamIdAndIdUserId(100L, 2L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(101L);
            return t;
        });

        CreateTaskRequest request = new CreateTaskRequest("API Auth", "Set up JWT", TaskPriority.HIGH, 2L, null);
        TaskResponse response = taskService.createTask(100L, 1L, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(101L);
        assertThat(response.title()).isEqualTo("API Auth");

        verify(notificationService).createNotification(
                eq(assignee),
                eq(creator),
                eq(NotificationType.TASK_ASSIGNED),
                anyString(),
                anyString(),
                eq(101L)
        );
    }

    @Test
    @DisplayName("Should update task status and notify creator when updated by another member")
    void testUpdateTaskStatusEmitsNotificationToCreator() {
        when(taskRepository.findById(500L)).thenReturn(Optional.of(task));
        when(teamMemberRepository.existsByIdTeamIdAndIdUserId(100L, 2L)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));

        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.DONE);
        TaskResponse response = taskService.updateTaskStatus(500L, 2L, request);

        assertThat(response.status()).isEqualTo(TaskStatus.DONE);

        verify(notificationService).createNotification(
                eq(creator),
                eq(assignee),
                eq(NotificationType.TASK_STATUS_CHANGED),
                anyString(),
                anyString(),
                eq(500L)
        );
    }

    @Test
    @DisplayName("Should reject task deletion if caller is neither creator nor team leader")
    void testDeleteTaskRejectsRegularMember() {
        when(taskRepository.findById(500L)).thenReturn(Optional.of(task));
        // caller is assignee (id=2), creator is id=1
        TeamMember member = new TeamMember(team, assignee, TeamRole.MEMBER);
        when(teamMemberRepository.findByIdTeamIdAndIdUserId(100L, 2L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> taskService.deleteTask(500L, 2L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("creator or team leader can delete this task");

        verify(taskRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should allow task deletion by team leader even if not the creator")
    void testDeleteTaskAllowedForTeamLeader() {
        User leader = new User("leader@university.edu", "hash", "Team Leader", "CS", 4);
        leader.setId(3L);

        when(taskRepository.findById(500L)).thenReturn(Optional.of(task));
        TeamMember leaderMember = new TeamMember(team, leader, TeamRole.LEADER);
        when(teamMemberRepository.findByIdTeamIdAndIdUserId(100L, 3L)).thenReturn(Optional.of(leaderMember));

        taskService.deleteTask(500L, 3L);

        verify(taskRepository).delete(task);
    }
}
