package com.huyho.taskflow.task;

import java.time.Instant;

public record TaskResponse(Long id, Long projectId, String title, String description,
                           TaskStatus status, TaskPriority priority, Instant createdAt) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getProject().getId(), task.getTitle(),
                task.getDescription(), task.getStatus(), task.getPriority(), task.getCreatedAt());
    }
}
