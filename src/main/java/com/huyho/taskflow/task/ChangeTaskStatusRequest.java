package com.huyho.taskflow.task;

import jakarta.validation.constraints.NotNull;

public record ChangeTaskStatusRequest(
        @NotNull(message = "Status is required") TaskStatus status) {
}
