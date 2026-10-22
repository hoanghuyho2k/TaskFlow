package com.huyho.taskflow.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.huyho.taskflow.task.Task;
import com.huyho.taskflow.task.TaskRepository;
import com.huyho.taskflow.task.TaskResponse;

@Service
public class DashboardService {

    private final ProjectRepository projects;
    private final TaskRepository tasks;

    public DashboardService(ProjectRepository projects, TaskRepository tasks) {
        this.projects = projects;
        this.tasks = tasks;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getForOwner(Long ownerId) {
        var projectPage = projects.findAllByOwnerId(ownerId,
                PageRequest.of(0, 100, Sort.by("id")));
        Map<Long, List<TaskResponse>> tasksByProject = new HashMap<>();
        for (Task task : tasks.findAllByProjectOwnerId(ownerId, Sort.by("id"))) {
            tasksByProject.computeIfAbsent(task.getProject().getId(), ignored -> new ArrayList<>())
                    .add(TaskResponse.from(task));
        }
        return new DashboardResponse(projectPage.getContent().stream()
                .map(project -> new DashboardProjectResponse(project.getId(), project.getName(),
                        project.getDescription(), project.getCreatedAt(),
                        tasksByProject.getOrDefault(project.getId(), List.of())))
                .toList());
    }
}
