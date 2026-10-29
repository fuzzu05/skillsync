package com.skillsync.dto.task;

import com.skillsync.entity.Task;
import com.skillsync.enums.TaskPriority;
import com.skillsync.enums.TaskStatus;

import java.time.Instant;

public record TaskResponse(
        Long id,
        Long teamId,
        String title,
        String description,
        TaskPriority priority,
        TaskStatus status,
        Instant dueDate,
        Long creatorId,
        String creatorName,
        Long assigneeId,
        String assigneeName,
        Instant createdAt,
        Instant updatedAt
) {
    public static TaskResponse fromEntity(Task task) {
        Long assigneeId = task.getAssignee() != null ? task.getAssignee().getId() : null;
        String assigneeName = task.getAssignee() != null ? task.getAssignee().getFullName() : null;

        return new TaskResponse(
                task.getId(),
                task.getTeam().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getStatus(),
                task.getDueDate(),
                task.getCreator().getId(),
                task.getCreator().getFullName(),
                assigneeId,
                assigneeName,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
