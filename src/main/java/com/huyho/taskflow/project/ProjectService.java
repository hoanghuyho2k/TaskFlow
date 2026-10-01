package com.huyho.taskflow.project;

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
        return ProjectResponse.from(repository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id)));
    }
}
