package com.huyho.taskflow.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository repository;

    public ProjectService(ProjectRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        Project project = new Project(request.name().strip(), request.description());
        return ProjectResponse.from(repository.save(project));
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(Long id) {
        return ProjectResponse.from(findProject(id));
    }

    @Transactional(readOnly = true)
    public ProjectPageResponse list(int page, int size) {
        Page<Project> projects = repository.findAll(PageRequest.of(page, size, Sort.by("id")));
        return new ProjectPageResponse(projects.getContent().stream().map(ProjectResponse::from).toList(),
                projects.getNumber(), projects.getSize(), projects.getTotalElements(), projects.getTotalPages());
    }

    @Transactional
    public ProjectResponse update(Long id, UpdateProjectRequest request) {
        Project project = findProject(id);
        project.updateDetails(request.name().strip(), request.description());
        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findProject(id));
    }

    private Project findProject(Long id) {
        return repository.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
    }
}
