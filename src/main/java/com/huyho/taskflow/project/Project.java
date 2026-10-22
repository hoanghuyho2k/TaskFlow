package com.huyho.taskflow.project;

import java.time.Instant;

import com.huyho.taskflow.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", updatable = false)
    private User owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // JPA needs a no-argument constructor to reconstruct stored entities.
    protected Project() {
    }

    public Project(String name, String description, User owner) {
        this.name = name;
        this.description = description;
        this.owner = java.util.Objects.requireNonNull(owner, "Project owner is required");
    }

    @PrePersist
    private void initializeCreatedAt() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void updateDetails(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public User getOwner() {
        return owner;
    }
}
