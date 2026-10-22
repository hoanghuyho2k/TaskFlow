package com.huyho.taskflow.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        @NotBlank(message = "Title must not be blank")
        @Size(max = 200, message = "Title must be at most 200 characters") String title,
        @Size(max = 5000, message = "Description must be at most 5000 characters") String description,
        TaskStatus status,
        TaskPriority priority) {
}
