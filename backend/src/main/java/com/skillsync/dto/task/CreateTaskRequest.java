package com.skillsync.dto.task;

import com.skillsync.enums.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateTaskRequest(
        @NotBlank(message = "Task title is required")
        @Size(max = 150, message = "Task title cannot exceed 150 characters")
        String title,

        String description,

        TaskPriority priority,

        Long assigneeId,

        Instant dueDate
) {
}
