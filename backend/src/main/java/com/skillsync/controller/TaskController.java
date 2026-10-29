package com.skillsync.controller;

import com.skillsync.dto.task.CreateTaskRequest;
import com.skillsync.dto.task.TaskResponse;
import com.skillsync.dto.task.UpdateTaskRequest;
import com.skillsync.dto.task.UpdateTaskStatusRequest;
import com.skillsync.enums.TaskStatus;
import com.skillsync.security.UserPrincipal;
import com.skillsync.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Tasks", description = "Team task management, priority tracking, and kanban statuses")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/teams/{teamId}/tasks")
    @Operation(summary = "Create task", description = "Add a task to a project team with priority, assignee, and due date.")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        TaskResponse created = taskService.createTask(teamId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/teams/{teamId}/tasks")
    @Operation(summary = "List team tasks", description = "Retrieve all tasks for a project team, optionally filtered by status (TODO, IN_PROGRESS, IN_REVIEW, DONE).")
    public ResponseEntity<List<TaskResponse>> getTeamTasks(
            @PathVariable Long teamId,
            @RequestParam(required = false) TaskStatus status,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<TaskResponse> tasks = taskService.getTasksByTeam(teamId, principal.getId(), status);
        return ResponseEntity.ok(tasks);
    }

    @PutMapping("/tasks/{taskId}")
    @Operation(summary = "Update task", description = "Edit task title, description, priority, assignee, or deadline.")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        TaskResponse updated = taskService.updateTask(taskId, principal.getId(), request);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/tasks/{taskId}/status")
    @Operation(summary = "Update task status", description = "Quick status change transition on the taskboard.")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateTaskStatusRequest request
    ) {
        TaskResponse updated = taskService.updateTaskStatus(taskId, principal.getId(), request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/tasks/{taskId}")
    @Operation(summary = "Delete task", description = "Delete a task (Creator or Team Leader only).")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        taskService.deleteTask(taskId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tasks/my-tasks")
    @Operation(summary = "Get my assigned tasks", description = "List all tasks assigned to the currently logged in student across teams.")
    public ResponseEntity<List<TaskResponse>> getMyTasks(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<TaskResponse> tasks = taskService.getMyAssignedTasks(principal.getId());
        return ResponseEntity.ok(tasks);
    }
}
