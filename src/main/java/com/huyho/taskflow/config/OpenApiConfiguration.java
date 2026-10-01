package com.huyho.taskflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI taskFlowOpenApi() {
        return new OpenAPI().components(new io.swagger.v3.oas.models.Components().addSecuritySchemes("bearerAuth",
                new io.swagger.v3.oas.models.security.SecurityScheme().type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                        .scheme("bearer").bearerFormat("JWT")))
                .info(new Info().title("TaskFlow API").version("0.0.1")
                .description("Project and Task management. Use IDs returned by creation or listing. "
                        + "Write operations persist real data. Log in and authorize with the returned bearer token. "
                        + "Project membership authorization is the next step."));
    }
}
