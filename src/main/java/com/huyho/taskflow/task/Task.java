package com.huyho.taskflow.task;

import java.time.Instant;
import java.util.Objects;

import com.huyho.taskflow.project.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Task() {
    }

    public Task(Project project, String title, String description) {
        this(project, title, description, TaskStatus.TODO, TaskPriority.MEDIUM);
    }

    public Task(Project project, String title, String description, TaskStatus status, TaskPriority priority) {
        this.project = Objects.requireNonNull(project, "Project is required");
        this.title = title;
        this.description = description;
        this.status = status == null ? TaskStatus.TODO : status;
        this.priority = priority == null ? TaskPriority.MEDIUM : priority;
    }

    @PrePersist
    private void initializeCreatedAt() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void updateDetails(String title, String description, TaskPriority priority) {
        this.title = title;
        this.description = description;
        this.priority = Objects.requireNonNull(priority, "Priority is required");
    }

    public void changeStatus(TaskStatus status) {
        this.status = Objects.requireNonNull(status, "Status is required");
    }

    public Project getProject() {
        return project;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
