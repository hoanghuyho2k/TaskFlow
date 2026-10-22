package com.huyho.taskflow.task;

import com.huyho.taskflow.project.Project;
import com.huyho.taskflow.project.ProjectRepository;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.security.test.context.support.WithMockUser(username = "1")
@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskMutationApiTests {

    @Autowired private MockMvc mvc;
    @Autowired private TaskRepository tasks;
    @Autowired private ProjectRepository projects;
    @Autowired private EntityManager entityManager;

    private Task createTask() {
        Project project = projects.saveAndFlush(new Project("Mutation project", null, com.huyho.taskflow.user.User.reference(1L)));
        Task task = tasks.saveAndFlush(new Task(project, "Original", "Keep description"));
        entityManager.clear();
        return tasks.findById(task.getId()).orElseThrow();
    }

    private Task reload(Long id) {
        entityManager.flush();
        entityManager.clear();
        return tasks.findById(id).orElseThrow();
    }

    @Test
    void updatePersistsDetailsAndPriorityAndPreservesOtherFields() throws Exception {
        Task task = createTask();
        Long projectId = task.getProject().getId();
        var createdAt = task.getCreatedAt();
        task.changeStatus(TaskStatus.IN_PROGRESS);
        entityManager.flush();
        mvc.perform(put("/api/tasks/{id}", task.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" Updated \",\"priority\":\"HIGH\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Updated"));
        Task stored = reload(task.getId());
        assertThat(stored.getTitle()).isEqualTo("Updated");
        assertThat(stored.getDescription()).isNull();
        assertThat(stored.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(stored.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(stored.getProject().getId()).isEqualTo(projectId);
        assertThat(stored.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void statusChangesPersistWithoutChangingDetailsAndCanReopen() throws Exception {
        Task task = createTask();
        var createdAt = task.getCreatedAt();
        for (String next : new String[]{"IN_PROGRESS", "DONE", "DONE", "TODO"}) {
            mvc.perform(patch("/api/tasks/{id}/status", task.getId()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"" + next + "\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(next));
            Task stored = reload(task.getId());
            assertThat(stored.getStatus().name()).isEqualTo(next);
            assertThat(stored.getTitle()).isEqualTo("Original");
            assertThat(stored.getDescription()).isEqualTo("Keep description");
            assertThat(stored.getPriority()).isEqualTo(TaskPriority.MEDIUM);
            assertThat(stored.getCreatedAt()).isEqualTo(createdAt);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"title\":\"Valid\"}", "{\"title\":\"   \",\"priority\":\"HIGH\"}",
            "{\"title\":\"Valid\",\"priority\":null}", "{\"title\":\"Valid\",\"priority\":\"URGENT\"}", "{"})
    void invalidUpdateDoesNotChangeStoredTask(String body) throws Exception {
        Task task = createTask();
        mvc.perform(put("/api/tasks/{id}", task.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        Task stored = reload(task.getId());
        assertThat(stored.getTitle()).isEqualTo("Original");
        assertThat(stored.getDescription()).isEqualTo("Keep description");
        assertThat(stored.getPriority()).isEqualTo(TaskPriority.MEDIUM);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"status\":null}", "{\"status\":\"UNKNOWN\"}", "{"})
    void invalidStatusDoesNotChangeStoredTask(String body) throws Exception {
        Task task = createTask();
        mvc.perform(patch("/api/tasks/{id}/status", task.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(reload(task.getId()).getStatus()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void rejectsOversizedUpdateFields() throws Exception {
        Task task = createTask();
        for (String body : new String[]{"{\"title\":\"" + "a".repeat(201) + "\",\"priority\":\"LOW\"}",
                "{\"title\":\"Valid\",\"description\":\"" + "a".repeat(5001) + "\",\"priority\":\"LOW\"}"}) {
            mvc.perform(put("/api/tasks/{id}", task.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void deleteRemovesTaskButPreservesProjectAndSibling() throws Exception {
        Task task = createTask();
        Long projectId = task.getProject().getId();
        Long siblingId = tasks.saveAndFlush(new Task(task.getProject(), "Sibling", null)).getId();
        mvc.perform(delete("/api/tasks/{id}", task.getId()))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        entityManager.flush();
        entityManager.clear();
        assertThat(tasks.findById(task.getId())).isEmpty();
        assertThat(tasks.findById(siblingId)).isPresent();
        assertThat(projects.findById(projectId)).isPresent();
        mvc.perform(delete("/api/tasks/{id}", task.getId())).andExpect(status().isNotFound());
    }

    @Test
    void missingTasksReturnNotFound() throws Exception {
        String path = "/api/tasks/9223372036854775807";
        mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Valid\",\"priority\":\"LOW\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(patch(path + "/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete(path)).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc"})
    void invalidIdsReturnBadRequest(String id) throws Exception {
        mvc.perform(put("/api/tasks/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Valid\",\"priority\":\"LOW\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/tasks/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/tasks/" + id)).andExpect(status().isBadRequest());
    }
}
