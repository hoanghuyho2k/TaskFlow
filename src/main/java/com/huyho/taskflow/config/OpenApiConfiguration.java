package com.huyho.taskflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI taskFlowOpenApi() {
        return new OpenAPI().info(new Info().title("TaskFlow API").version("0.0.1")
                .description("Project and Task management. Use IDs returned by creation or listing. "
                        + "Write operations persist real data. Authentication is a later milestone."));
    }
}
