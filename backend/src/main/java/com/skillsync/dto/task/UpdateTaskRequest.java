package com.skillsync.dto.task;

import com.skillsync.enums.TaskPriority;
import com.skillsync.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateTaskRequest(
        @NotBlank(message = "Task title cannot be blank")
        @Size(max = 150, message = "Task title cannot exceed 150 characters")
        String title,

        String description,

        TaskPriority priority,

        TaskStatus status,

        Long assigneeId,

        Instant dueDate
) {
}
