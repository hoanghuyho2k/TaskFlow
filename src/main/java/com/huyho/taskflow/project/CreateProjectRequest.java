package com.huyho.taskflow.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank(message = "Name must not be blank")
        @Size(max = 150, message = "Name must be at most 150 characters") String name,
        @Size(max = 2000, message = "Description must be at most 2000 characters") String description) {
}
