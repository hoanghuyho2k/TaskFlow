package com.huyho.taskflow.project;

import java.time.Instant;
import java.util.List;

import com.huyho.taskflow.task.TaskResponse;

public record DashboardProjectResponse(
        Long id, String name, String description, Instant createdAt, List<TaskResponse> tasks) {
}
