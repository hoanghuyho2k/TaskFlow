package com.huyho.taskflow.task;

import com.huyho.taskflow.project.Project;
import com.huyho.taskflow.project.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskFilterApiTests {

    @Autowired private MockMvc mvc;
    @Autowired private TaskRepository tasks;
    @Autowired private ProjectRepository projects;

    private Task addTask(Project project, TaskStatus status, TaskPriority priority) {
        Task task = new Task(project, "Filter test", null);
        task.changeStatus(status);
        task.updateDetails(task.getTitle(), null, priority);
        return tasks.saveAndFlush(task);
    }

    @Test
    void filtersBeforePaginationAndNeverIncludesAnotherProjectsTasks() throws Exception {
        Project project = projects.saveAndFlush(new Project("Filters", null));
        Project other = projects.saveAndFlush(new Project("Other", null));
        Long first = addTask(project, TaskStatus.DONE, TaskPriority.HIGH).getId();
        Long second = addTask(project, TaskStatus.DONE, TaskPriority.HIGH).getId();
        addTask(project, TaskStatus.TODO, TaskPriority.HIGH);
        addTask(project, TaskStatus.DONE, TaskPriority.LOW);
        addTask(other, TaskStatus.DONE, TaskPriority.HIGH);

        mvc.perform(get("/api/projects/{id}/tasks", project.getId()).param("status", "DONE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/projects/{id}/tasks", project.getId()).param("priority", "HIGH"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/projects/{id}/tasks", project.getId()).param("status", "DONE")
                        .param("priority", "HIGH").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(first))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/projects/{id}/tasks", project.getId()).param("status", "DONE")
                        .param("priority", "HIGH").param("size", "1").param("page", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(second));
        mvc.perform(get("/api/projects/{id}/tasks", project.getId()).param("status", "IN_PROGRESS"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"status=UNKNOWN", "status=done", "priority=URGENT", "priority=1", "page=abc"})
    void invalidQueryReportsParameterName(String query) throws Exception {
        mvc.perform(get("/api/projects/9223372036854775807/tasks?" + query))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/projects/9223372036854775807/tasks"))
                .andExpect(jsonPath("$.errors[0].field").value(query.split("=")[0]));
    }

    @Test
    void filteredListStillRejectsMissingProject() throws Exception {
        mvc.perform(get("/api/projects/9223372036854775807/tasks?status=DONE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidJsonHasSafeConsistentProblemDetail() throws Exception {
        mvc.perform(patch("/api/tasks/9223372036854775807/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INVALID\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(
                        "Request body must contain valid JSON with the expected field types and enum values"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
