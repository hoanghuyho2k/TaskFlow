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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.security.test.context.support.WithMockUser(username = "1")
@org.springframework.test.context.ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskApiTests {

    @Autowired private MockMvc mvc;
    @Autowired private ProjectRepository projects;
    @Autowired private TaskRepository tasks;
    @Autowired private EntityManager entityManager;

    @Test
    void createsAndRetrievesTaskWithDefaults() throws Exception {
        Long projectId = projects.saveAndFlush(new Project("API task project", null, com.huyho.taskflow.user.User.reference(1L))).getId();
        String location = mvc.perform(post("/api/projects/{id}/tasks", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  Build API  \",\"description\":\"Implement endpoints\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.title").value("Build API"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.project").doesNotExist())
                .andReturn().getResponse().getHeader("Location");
        entityManager.flush();
        entityManager.clear();
        mvc.perform(get(location)).andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.title").value("Build API"))
                .andExpect(jsonPath("$.description").value("Implement endpoints"));
    }

    @Test
    void permitsMissingDescription() throws Exception {
        Long id = projects.saveAndFlush(new Project("Optional description", null, com.huyho.taskflow.user.User.reference(1L))).getId();
        mvc.perform(post("/api/projects/{id}/tasks", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Minimal task\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void createsTaskWithRequestedStatusAndPriority() throws Exception {
        Long projectId = projects.saveAndFlush(new Project("Custom task options", null, com.huyho.taskflow.user.User.reference(1L))).getId();
        mvc.perform(post("/api/projects/{id}/tasks", projectId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\\"title\\":\\"Ship feature\\",\\"status\\":\\"IN_PROGRESS\\",\\"priority\\":\\"HIGH\\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void listsOnlyRequestedProjectsTasksInPages() throws Exception {
        Project first = projects.saveAndFlush(new Project("First project", null, com.huyho.taskflow.user.User.reference(1L)));
        Project second = projects.saveAndFlush(new Project("Second project", null, com.huyho.taskflow.user.User.reference(1L)));
        Long firstId = tasks.saveAndFlush(new Task(first, "First task", null)).getId();
        Long nextId = tasks.saveAndFlush(new Task(first, "Next task", null)).getId();
        tasks.saveAndFlush(new Task(second, "Other task", null));
        entityManager.clear();
        mvc.perform(get("/api/projects/{id}/tasks", first.getId()).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(firstId))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/projects/{id}/tasks", first.getId()).param("page", "1").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(nextId));
        mvc.perform(get("/api/projects/{id}/tasks", first.getId()).param("page", "2").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void returnsEmptyPageForExistingProjectWithoutTasks() throws Exception {
        Long id = projects.saveAndFlush(new Project("Empty project", null, com.huyho.taskflow.user.User.reference(1L))).getId();
        mvc.perform(get("/api/projects/{id}/tasks", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void missingProjectReturnsNotFoundForCreateAndList() throws Exception {
        long before = tasks.count();
        mvc.perform(post("/api/projects/9223372036854775807/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Valid task\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/projects/9223372036854775807/tasks")).andExpect(status().isNotFound());
        assertThat(tasks.count()).isEqualTo(before);
    }

    @Test
    void missingTaskReturnsProblemDetail() throws Exception {
        mvc.perform(get("/api/tasks/9223372036854775807"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"title\":null}", "{\"title\":\"   \"}", "{\"title\":\"\\t\\n\"}"})
    void rejectsInvalidTitleWithoutWriting(String body) throws Exception {
        Long id = projects.saveAndFlush(new Project("Validation project", null, com.huyho.taskflow.user.User.reference(1L))).getId();
        long before = tasks.count();
        mvc.perform(post("/api/projects/{id}/tasks", id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("title"));
        assertThat(tasks.count()).isEqualTo(before);
    }

    @Test
    void rejectsOversizedFieldsAndMalformedJson() throws Exception {
        Long id = projects.saveAndFlush(new Project("Limits project", null, com.huyho.taskflow.user.User.reference(1L))).getId();
        for (String body : new String[]{"{", "{\"title\":\"" + "a".repeat(201) + "\"}",
                "{\"title\":\"Valid\",\"description\":\"" + "a".repeat(5001) + "\"}"}) {
            mvc.perform(post("/api/projects/{id}/tasks", id).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc"})
    void rejectsInvalidIds(String id) throws Exception {
        mvc.perform(get("/api/tasks/" + id)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/projects/" + id + "/tasks")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/projects/" + id + "/tasks").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Valid\"}"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "page=abc"})
    void rejectsInvalidPagination(String query) throws Exception {
        Long id = projects.saveAndFlush(new Project("Pagination project", null, com.huyho.taskflow.user.User.reference(1L))).getId();
        mvc.perform(get("/api/projects/" + id + "/tasks?" + query)).andExpect(status().isBadRequest());
    }
}
