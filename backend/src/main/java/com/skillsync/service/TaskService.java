package com.skillsync.service;

import com.skillsync.dto.task.CreateTaskRequest;
import com.skillsync.dto.task.TaskResponse;
import com.skillsync.dto.task.UpdateTaskRequest;
import com.skillsync.dto.task.UpdateTaskStatusRequest;
import com.skillsync.entity.Task;
import com.skillsync.entity.Team;
import com.skillsync.entity.TeamMember;
import com.skillsync.entity.User;
import com.skillsync.enums.TaskPriority;
import com.skillsync.enums.TaskStatus;
import com.skillsync.enums.TeamRole;
import com.skillsync.exception.AccessDeniedException;
import com.skillsync.exception.BadRequestException;
import com.skillsync.exception.ResourceNotFoundException;
import com.skillsync.repository.TaskRepository;
import com.skillsync.repository.TeamMemberRepository;
import com.skillsync.repository.TeamRepository;
import com.skillsync.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public TaskService(TaskRepository taskRepository,
                       TeamRepository teamRepository,
                       TeamMemberRepository teamMemberRepository,
                       UserRepository userRepository,
                       NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public TaskResponse createTask(Long teamId, Long creatorId, CreateTaskRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

        // Enforce membership authorization
        if (!teamMemberRepository.existsByIdTeamIdAndIdUserId(teamId, creatorId)) {
            throw new AccessDeniedException("You must be an active member of the team to create tasks");
        }

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", creatorId));

        User assignee = null;
        if (request.assigneeId() != null) {
            if (!teamMemberRepository.existsByIdTeamIdAndIdUserId(teamId, request.assigneeId())) {
                throw new BadRequestException("Assignee must be an active member of this team");
            }
            assignee = userRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.assigneeId()));
        }

        Task task = new Task(
                team,
                creator,
                assignee,
                request.title().trim(),
                request.description() != null ? request.description().trim() : null,
                request.priority() != null ? request.priority() : TaskPriority.MEDIUM,
                request.dueDate()
        );

        Task saved = taskRepository.save(task);

        if (assignee != null) {
            notificationService.createNotification(
                    assignee,
                    creator,
                    com.skillsync.enums.NotificationType.TASK_ASSIGNED,
                    "New Task Assigned",
                    "You were assigned to: " + saved.getTitle(),
                    saved.getId()
            );
        }

        return TaskResponse.fromEntity(saved);
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, Long userId, UpdateTaskRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        Long teamId = task.getTeam().getId();
        if (!teamMemberRepository.existsByIdTeamIdAndIdUserId(teamId, userId)) {
            throw new AccessDeniedException("You must be a member of the team to update this task");
        }

        User updater = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        task.setTitle(request.title().trim());
        task.setDescription(request.description() != null ? request.description().trim() : null);
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        task.setDueDate(request.dueDate());

        if (request.assigneeId() != null) {
            if (!teamMemberRepository.existsByIdTeamIdAndIdUserId(teamId, request.assigneeId())) {
                throw new BadRequestException("New assignee must be a member of this team");
            }
            User assignee = userRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.assigneeId()));
            task.setAssignee(assignee);

            notificationService.createNotification(
                    assignee,
                    updater,
                    com.skillsync.enums.NotificationType.TASK_ASSIGNED,
                    "Task Assigned",
                    "You were assigned to: " + task.getTitle(),
                    task.getId()
            );
        } else {
            task.setAssignee(null);
        }

        Task saved = taskRepository.save(task);
        return TaskResponse.fromEntity(saved);
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long taskId, Long userId, UpdateTaskStatusRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        Long teamId = task.getTeam().getId();
        if (!teamMemberRepository.existsByIdTeamIdAndIdUserId(teamId, userId)) {
            throw new AccessDeniedException("You must be a member of the team to change task status");
        }

        task.setStatus(request.status());
        Task saved = taskRepository.save(task);

        User updater = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!task.getCreator().getId().equals(userId)) {
            notificationService.createNotification(
                    task.getCreator(),
                    updater,
                    com.skillsync.enums.NotificationType.TASK_STATUS_CHANGED,
                    "Task Status Changed",
                    "Task '" + task.getTitle() + "' moved to " + request.status(),
                    task.getId()
            );
        }

        return TaskResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));

        Long teamId = task.getTeam().getId();
        boolean isCreator = task.getCreator().getId().equals(userId);
        boolean isLeader = teamMemberRepository.findByIdTeamIdAndIdUserId(teamId, userId)
                .map(m -> m.getRole() == TeamRole.LEADER)
                .orElse(false);

        if (!isCreator && !isLeader) {
            throw new AccessDeniedException("Only the task creator or team leader can delete this task");
        }

        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByTeam(Long teamId, Long userId, TaskStatus status) {
        if (!teamMemberRepository.existsByIdTeamIdAndIdUserId(teamId, userId)) {
            throw new AccessDeniedException("You must be a member of this team to view its tasks");
        }

        List<Task> tasks;
        if (status != null) {
            tasks = taskRepository.findByTeamIdAndStatusWithDetails(teamId, status);
        } else {
            tasks = taskRepository.findByTeamIdWithDetails(teamId);
        }

        return tasks.stream().map(TaskResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getMyAssignedTasks(Long userId) {
        return taskRepository.findByAssigneeIdWithDetails(userId)
                .stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }
}
