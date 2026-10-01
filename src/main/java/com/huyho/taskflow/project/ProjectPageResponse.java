package com.huyho.taskflow.project;

import java.util.List;

public record ProjectPageResponse(
        List<ProjectResponse> content, int page, int size, long totalElements, int totalPages) {
}
