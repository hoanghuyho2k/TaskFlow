package com.huyho.taskflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiTests {

    @Autowired private MockMvc mvc;

    @Test
    void documentsContractsAndCreationAndDeletionStatusCodes() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("TaskFlow API"))
                .andExpect(jsonPath("$.paths['/api/projects'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/projects/{id}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/projects/{projectId}/tasks'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/tasks/{id}/status'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/tasks/{id}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.components.schemas.CreateTaskRequest.properties.title.maxLength").value(200))
                .andExpect(jsonPath("$.components.schemas.TaskResponse.properties.status.enum").isArray())
                .andExpect(jsonPath("$.paths['/actuator/health']").doesNotExist());
    }

    @Test
    void servesSwaggerUiAndConfiguration() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").value("/v3/api-docs"));
    }
}
