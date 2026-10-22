package com.huyho.taskflow.task;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.huyho.taskflow.project.Project;
import com.huyho.taskflow.project.ProjectRepository;
import com.huyho.taskflow.project.ProjectService;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest
@Transactional
class TaskRepositoryTests {

    @Autowired
    private TaskRepository tasks;
    @Autowired
    private ProjectRepository projects;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void savesAndReloadsTaskWithProjectAndDefaults() {
        Project project = projects.saveAndFlush(new Project("Task project", null, com.huyho.taskflow.user.User.reference(1L)));
        Instant beforeSave = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Task saved = tasks.saveAndFlush(new Task(project, "Build API", "Add endpoints"));
        Long id = saved.getId();
        Long projectId = project.getId();
        entityManager.clear();

        Task reloaded = tasks.findById(id).orElseThrow();
        assertThat(id).isPositive();
        assertThat(reloaded.getProject().getId()).isEqualTo(projectId);
        assertThat(reloaded.getTitle()).isEqualTo("Build API");
        assertThat(reloaded.getDescription()).isEqualTo("Add endpoints");
        assertThat(reloaded.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(reloaded.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(reloaded.getCreatedAt()).isBetween(beforeSave, Instant.now());
        assertThat(jdbc.queryForObject("SELECT status FROM tasks WHERE id = ?", String.class, id))
                .isEqualTo("TODO");
    }

    @Test
    void listsOnlyTasksBelongingToRequestedProject() {
        Project first = projects.saveAndFlush(new Project("First", null, com.huyho.taskflow.user.User.reference(1L)));
        Project second = projects.saveAndFlush(new Project("Second", null, com.huyho.taskflow.user.User.reference(1L)));
        Task firstTask = tasks.saveAndFlush(new Task(first, "First task", null));
        Task secondTask = tasks.saveAndFlush(new Task(first, "Second task", null));
        tasks.saveAndFlush(new Task(second, "Other project task", null));
        entityManager.clear();

        var page = tasks.findByProjectId(first.getId(), PageRequest.of(0, 1, Sort.by("id")));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Task::getId).containsExactly(firstTask.getId());
        assertThat(tasks.findByProjectId(first.getId(), PageRequest.of(1, 1, Sort.by("id"))).getContent())
                .extracting(Task::getId).containsExactly(secondTask.getId());
    }

    @Test
    void deletingProjectCascadesToItsTasksOnly() {
        Project removed = projects.saveAndFlush(new Project("Remove", null, com.huyho.taskflow.user.User.reference(1L)));
        Project kept = projects.saveAndFlush(new Project("Keep", null, com.huyho.taskflow.user.User.reference(1L)));
        Long removedTaskId = tasks.saveAndFlush(new Task(removed, "Remove task", null)).getId();
        Long keptTaskId = tasks.saveAndFlush(new Task(kept, "Keep task", null)).getId();

        entityManager.clear();
        projectService.delete(1L, removed.getId());
        entityManager.flush();
        // Database cascades do not refresh entities already held in JPA's cache.
        entityManager.clear();
        assertThat(tasks.findById(removedTaskId)).isEmpty();
        assertThat(tasks.findById(keptTaskId)).isPresent();
        assertThat(projects.findById(kept.getId())).isPresent();
    }

    @Test
    void databaseRejectsMissingProject() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tasks (project_id, title, created_at)
                VALUES (9223372036854775807, 'Orphan', CURRENT_TIMESTAMP)
                """)) .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsInvalidStatus() {
        Project project = projects.saveAndFlush(new Project("Status constraint", null, com.huyho.taskflow.user.User.reference(1L)));
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tasks (project_id, title, status, created_at)
                VALUES (?, 'Invalid status', 'UNKNOWN', CURRENT_TIMESTAMP)
                """, project.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsInvalidPriority() {
        Project project = projects.saveAndFlush(new Project("Priority constraint", null, com.huyho.taskflow.user.User.reference(1L)));
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tasks (project_id, title, priority, created_at)
                VALUES (?, 'Invalid priority', 'URGENT', CURRENT_TIMESTAMP)
                """, project.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsBlankTitle() {
        Project project = projects.saveAndFlush(new Project("Title constraint", null, com.huyho.taskflow.user.User.reference(1L)));
        assertThatThrownBy(() -> tasks.saveAndFlush(new Task(project, "   ", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
