package com.peprah.todo.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    record ErrorResponse(int status, String error, String message, String path, LocalDateTime timestamp) {}

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        int statusValue = ex.getStatusCode().value();
        HttpStatus httpStatus = HttpStatus.resolve(statusValue);
        String error = httpStatus != null ? httpStatus.getReasonPhrase() : String.valueOf(statusValue);
        String message = ex.getReason() != null ? ex.getReason() : error;
        return ResponseEntity.status(ex.getStatusCode())
                .body(new ErrorResponse(statusValue, error, message, request.getRequestURI(), LocalDateTime.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "Bad Request", message, request.getRequestURI(), LocalDateTime.now()));
    }
}
