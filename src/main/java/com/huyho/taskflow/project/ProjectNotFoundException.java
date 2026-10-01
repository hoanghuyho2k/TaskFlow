package com.huyho.taskflow.project;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(Long id) {
        super("Project " + id + " was not found");
    }
}
