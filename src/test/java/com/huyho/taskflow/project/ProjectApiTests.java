package com.huyho.taskflow.project;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectApiTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ProjectRepository repository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    void updatesStoredDetailsAndPreservesCreationTime() throws Exception {
        Project project = repository.saveAndFlush(new Project("Original", "Original description"));
        Long id = project.getId();
        entityManager.clear();
        java.time.Instant createdAt = repository.findById(id).orElseThrow().getCreatedAt();

        mvc.perform(put("/api/projects/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  Updated  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
        entityManager.flush();
        entityManager.clear();
        Project updated = repository.findById(id).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Updated");
        assertThat(updated.getDescription()).isNull();
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void invalidUpdateLeavesStoredProjectUnchanged() throws Exception {
        Project project = repository.saveAndFlush(new Project("Original", "Keep this"));
        mvc.perform(put("/api/projects/{id}", project.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());
        entityManager.clear();
        assertThat(repository.findById(project.getId()).orElseThrow().getName()).isEqualTo("Original");
    }

    @Test
    void deletesProjectAndReturnsNotFoundAfterward() throws Exception {
        Long id = repository.saveAndFlush(new Project("Delete me", null)).getId();
        mvc.perform(delete("/api/projects/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        entityManager.flush();
        entityManager.clear();
        assertThat(repository.findById(id)).isEmpty();
        mvc.perform(get("/api/projects/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/projects/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void updateOfMissingProjectReturnsNotFound() throws Exception {
        mvc.perform(put("/api/projects/9223372036854775807").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Valid name\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listsProjectsWithPaginationAndStableOrdering() throws Exception {
        // Existing manual projects may be present; compare with the actual ordered IDs.
        repository.saveAndFlush(new Project("First test project", null));
        repository.saveAndFlush(new Project("Second test project", null));
        var ordered = repository.findAll(org.springframework.data.domain.Sort.by("id"));
        mvc.perform(get("/api/projects").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(ordered.getFirst().getId()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(ordered.size()))
                .andExpect(jsonPath("$.totalPages").value(ordered.size()));
        mvc.perform(get("/api/projects").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ordered.get(1).getId()));
        mvc.perform(get("/api/projects").param("page", String.valueOf(ordered.size())).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "page=abc"})
    void rejectsInvalidPagination(String query) throws Exception {
        mvc.perform(get("/api/projects?" + query)).andExpect(status().isBadRequest());
    }

    @Test
    void createsAndRetrievesProject() throws Exception {
        String location = mvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  TaskFlow  ","description":"Backend portfolio"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("TaskFlow"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("TaskFlow"))
                .andExpect(jsonPath("$.description").value("Backend portfolio"));
    }

    @Test
    void createsProjectWithoutDescription() throws Exception {
        mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Minimal project\"}"))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":null}", "{\"name\":\"   \"}", "{\"name\":\"\\t\\n\"}"})
    void rejectsMissingOrBlankNameWithoutWriting(String body) throws Exception {
        long before = repository.count();
        mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
        assertThat(repository.count()).isEqualTo(before);
    }

    @Test
    void rejectsOversizedFields() throws Exception {
        mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "a".repeat(151) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Valid\",\"description\":\"" + "a".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForMissingProject() throws Exception {
        mvc.perform(get("/api/projects/9223372036854775807"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc"})
    void rejectsInvalidIds(String id) throws Exception {
        mvc.perform(get("/api/projects/" + id))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
