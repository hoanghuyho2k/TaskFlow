package com.huyho.taskflow.project;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    java.util.Optional<Project> findByIdAndOwnerId(Long id, Long ownerId);
    boolean existsByIdAndOwnerId(Long id, Long ownerId);
    org.springframework.data.domain.Page<Project> findAllByOwnerId(Long ownerId, org.springframework.data.domain.Pageable pageable);
}
