package com.huyho.taskflow.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    Page<Task> findByProjectId(Long projectId, Pageable pageable);
    java.util.Optional<Task> findByIdAndProjectOwnerId(Long id, Long ownerId);
    Page<Task> findAllByProjectIdAndProjectOwnerId(Long projectId, Long ownerId, Pageable pageable);
    java.util.List<Task> findAllByProjectOwnerId(Long ownerId, org.springframework.data.domain.Sort sort);
    boolean existsByProjectIdAndProjectOwnerId(Long projectId, Long ownerId);
}
