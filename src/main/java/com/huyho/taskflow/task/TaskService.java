package com.huyho.taskflow.task;

import com.huyho.taskflow.project.Project;
import com.huyho.taskflow.project.ProjectNotFoundException;
import com.huyho.taskflow.project.ProjectRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    public TaskResponse create(Long ownerId, Long projectId, CreateTaskRequest request) {
        Project project = projects.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        Task task = new Task(project, request.title().strip(), request.description(), request.status(), request.priority());
        return TaskResponse.from(tasks.save(task));
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(Long ownerId, Long id) {
        return TaskResponse.from(findTask(ownerId, id));
    }

    @Transactional
    public TaskResponse update(Long ownerId, Long id, UpdateTaskRequest request) {
        Task task = findTask(ownerId, id);
        task.updateDetails(request.title().strip(), request.description(), request.priority());
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse changeStatus(Long ownerId, Long id, ChangeTaskStatusRequest request) {
        Task task = findTask(ownerId, id);
        task.changeStatus(request.status());
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long ownerId, Long id) {
        tasks.delete(findTask(ownerId, id));
    }

    private Task findTask(Long ownerId, Long id) {
        return tasks.findByIdAndProjectOwnerId(id, ownerId).orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public TaskPageResponse list(Long ownerId, Long projectId, int page, int size, TaskStatus status, TaskPriority priority) {
        if (!projects.existsByIdAndOwnerId(projectId, ownerId)) {
            throw new ProjectNotFoundException(projectId);
        }
        Specification<Task> filter = (root, query, builder) ->
                builder.and(builder.equal(root.get("project").get("id"), projectId),
                        builder.equal(root.get("project").get("owner").get("id"), ownerId));
        if (status != null) {
            filter = filter.and((root, query, builder) -> builder.equal(root.get("status"), status));
        }
        if (priority != null) {
            filter = filter.and((root, query, builder) -> builder.equal(root.get("priority"), priority));
        }
        Page<Task> result = tasks.findAll(filter, PageRequest.of(page, size, Sort.by("id")));
        return new TaskPageResponse(result.getContent().stream().map(TaskResponse::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
