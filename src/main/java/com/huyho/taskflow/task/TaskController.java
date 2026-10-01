package com.huyho.taskflow.task;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;

import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tasks")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
})
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @Operation(summary = "Retrieve a Task")
    @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
    @GetMapping("/{id}")
    public TaskResponse getById(@PathVariable @Positive Long id) {
        return service.getById(id);
    }

    @Operation(summary = "Replace title description and priority; preserve status")
    @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable @Positive Long id,
                               @Valid @RequestBody UpdateTaskRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Change status; all transitions including reopening are allowed")
    @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true)
    @PatchMapping("/{id}/status")
    public TaskResponse changeStatus(@PathVariable @Positive Long id,
                                     @Valid @RequestBody ChangeTaskStatusRequest request) {
        return service.changeStatus(id, request);
    }

    @Operation(summary = "Delete a Task; preserve its Project")
    @ApiResponse(responseCode = "204", description = "No Content", content = @Content)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
