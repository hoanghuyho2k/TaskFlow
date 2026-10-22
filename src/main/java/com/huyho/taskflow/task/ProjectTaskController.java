package com.huyho.taskflow.task;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import com.huyho.taskflow.config.AuthenticatedUser;

@Tag(name = "Project Tasks")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
})
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
public class ProjectTaskController {

    private final TaskService service;

    public ProjectTaskController(TaskService service) {
        this.service = service;
    }

    @Operation(summary = "Create a Task in a Project")
    @ApiResponse(responseCode = "201", description = "Created", useReturnTypeSchema = true)
    @PostMapping
    public ResponseEntity<TaskResponse> create(Authentication authentication, @PathVariable @Positive Long projectId,
                                              @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse task = service.create(AuthenticatedUser.id(authentication), projectId, request);
        return ResponseEntity.created(URI.create("/api/tasks/" + task.id())).body(task);
    }

    @Operation(summary = "List Tasks with optional status and priority filters")
    @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
    @GetMapping
    public TaskPageResponse list(Authentication authentication, @PathVariable @Positive Long projectId,
                                @RequestParam(defaultValue = "0") @Min(0) int page,
                                @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                @RequestParam(required = false) TaskStatus status,
                                @RequestParam(required = false) TaskPriority priority) {
        return service.list(AuthenticatedUser.id(authentication), projectId, page, size, status, priority);
    }
}
