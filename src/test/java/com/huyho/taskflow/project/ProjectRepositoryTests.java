package com.huyho.taskflow.project;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ProjectRepositoryTests {

    @Autowired
    private ProjectRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndReloadsProjectFromPostgres() {
        Instant beforeSave = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Project saved = repository.saveAndFlush(new Project("TaskFlow", "Backend portfolio"));
        Long id = saved.getId();

        // Clear the first-level cache so findById reads the database.
        entityManager.clear();
        Project reloaded = repository.findById(id).orElseThrow();

        assertThat(id).isPositive();
        assertThat(reloaded.getName()).isEqualTo("TaskFlow");
        assertThat(reloaded.getDescription()).isEqualTo("Backend portfolio");
        assertThat(reloaded.getCreatedAt()).isBetween(beforeSave, Instant.now());
    }

    @Test
    void permitsMissingDescription() {
        Project saved = repository.saveAndFlush(new Project("Minimal project", null));
        entityManager.clear();

        assertThat(repository.findById(saved.getId()).orElseThrow().getDescription()).isNull();
    }

    @Test
    void databaseRejectsBlankName() {
        assertThatThrownBy(() -> repository.saveAndFlush(new Project("   ", null)))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
