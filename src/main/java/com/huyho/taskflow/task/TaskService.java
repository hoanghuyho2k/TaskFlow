package com.huyho.taskflow.task;

import com.huyho.taskflow.project.Project;
import com.huyho.taskflow.project.ProjectNotFoundException;
import com.huyho.taskflow.project.ProjectRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository tasks;
    private final ProjectRepository projects;

    public TaskService(TaskRepository tasks, ProjectRepository projects) {
        this.tasks = tasks;
        this.projects = projects;
    }

    @Transactional
    public TaskResponse create(Long projectId, CreateTaskRequest request) {
        Project project = projects.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        Task task = new Task(project, request.title().strip(), request.description());
        return TaskResponse.from(tasks.save(task));
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(Long id) {
        return TaskResponse.from(findTask(id));
    }

    @Transactional
    public TaskResponse update(Long id, UpdateTaskRequest request) {
        Task task = findTask(id);
        task.updateDetails(request.title().strip(), request.description(), request.priority());
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse changeStatus(Long id, ChangeTaskStatusRequest request) {
        Task task = findTask(id);
        task.changeStatus(request.status());
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long id) {
        tasks.delete(findTask(id));
    }

    private Task findTask(Long id) {
        return tasks.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public TaskPageResponse list(Long projectId, int page, int size) {
        if (!projects.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }
        Page<Task> result = tasks.findByProjectId(projectId, PageRequest.of(page, size, Sort.by("id")));
        return new TaskPageResponse(result.getContent().stream().map(TaskResponse::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
