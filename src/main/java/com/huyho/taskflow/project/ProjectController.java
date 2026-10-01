package com.huyho.taskflow.project;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

@Tag(name = "Projects")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
})
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @Operation(summary = "Create a Project")
    @ApiResponse(responseCode = "201", description = "Created", useReturnTypeSchema = true)
    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse project = service.create(request);
        return ResponseEntity.created(URI.create("/api/projects/" + project.id())).body(project);
    }

    @Operation(summary = "Retrieve a Project")
    @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
    @GetMapping("/{id}")
    public ProjectResponse getById(@PathVariable @Positive Long id) {
        return service.getById(id);
    }

    @Operation(summary = "List Projects")
    @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
    @GetMapping
    public ProjectPageResponse list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(page, size);
    }

    @Operation(summary = "Replace Project details")
    @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable @Positive Long id,
                                  @Valid @RequestBody UpdateProjectRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Delete a Project and its Tasks")
    @ApiResponse(responseCode = "204", description = "No Content", content = @Content)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
