package com.huyho.taskflow.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.huyho.taskflow.user.UserRepository;

@Service
public class ProjectService {

    private final ProjectRepository repository;
    private final UserRepository users;

    public ProjectService(ProjectRepository repository, UserRepository users) {
        this.repository = repository;
        this.users = users;
    }

    @Transactional
    public ProjectResponse create(Long ownerId, CreateProjectRequest request) {
        var owner = users.findById(ownerId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
        Project project = new Project(request.name().strip(), request.description(), owner);
        return ProjectResponse.from(repository.save(project));
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(Long ownerId, Long id) {
        return ProjectResponse.from(findProject(ownerId, id));
    }

    @Transactional(readOnly = true)
    public ProjectPageResponse list(Long ownerId, int page, int size) {
        Page<Project> projects = repository.findAllByOwnerId(ownerId, PageRequest.of(page, size, Sort.by("id")));
        return new ProjectPageResponse(projects.getContent().stream().map(ProjectResponse::from).toList(),
                projects.getNumber(), projects.getSize(), projects.getTotalElements(), projects.getTotalPages());
    }

    @Transactional
    public ProjectResponse update(Long ownerId, Long id, UpdateProjectRequest request) {
        Project project = findProject(ownerId, id);
        project.updateDetails(request.name().strip(), request.description());
        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(Long ownerId, Long id) {
        repository.delete(findProject(ownerId, id));
    }

    private Project findProject(Long ownerId, Long id) {
        return repository.findByIdAndOwnerId(id, ownerId).orElseThrow(() -> new ProjectNotFoundException(id));
    }
}
