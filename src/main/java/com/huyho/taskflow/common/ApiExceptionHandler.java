package com.huyho.taskflow.common;

import com.huyho.taskflow.project.ProjectNotFoundException;
import com.huyho.taskflow.task.TaskNotFoundException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(com.huyho.taskflow.auth.EmailAlreadyRegisteredException.class)
    public ProblemDetail handleEmailConflict(com.huyho.taskflow.auth.EmailAlreadyRegisteredException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Request parameter has an invalid value");
        if (exception instanceof MethodArgumentTypeMismatchException mismatch) {
            Class<?> type = mismatch.getRequiredType();
            String message = type != null && type.isEnum()
                    ? "Must be one of " + java.util.Arrays.toString(type.getEnumConstants())
                    : "Must have the expected type";
            problem.setProperty("errors", java.util.List.of(new FieldError(mismatch.getName(), message)));
        }
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status,
                "Request body must contain valid JSON with the expected field types and enum values");
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ProblemDetail handleProjectNotFound(ProjectNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ProblemDetail handleTaskNotFound(TaskNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Request validation failed");
        problem.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList());
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    public record FieldError(String field, String message) {
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (exception.isForReturnValue()) {
            return super.handleHandlerMethodValidationException(exception, headers, status, request);
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Request validation failed");
        problem.setProperty("errors", exception.getParameterValidationResults().stream()
                .flatMap(result -> {
                    if (result instanceof ParameterErrors errors) {
                        return errors.getFieldErrors().stream()
                                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()));
                    }
                    return result.getResolvableErrors().stream()
                            .map(error -> new FieldError(result.getMethodParameter().getParameterName(),
                                    error.getDefaultMessage()));
                }).toList());
        return handleExceptionInternal(exception, problem, headers, status, request);
    }
}
