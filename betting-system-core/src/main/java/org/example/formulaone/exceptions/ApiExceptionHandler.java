package org.example.formulaone.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the Formula 1 Betting API.
 * Handles only the exceptions actually used in the codebase.
 */
@ControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    /**
     * Handle bad request errors (400) - Invalid arguments, missing fields.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(
            IllegalArgumentException ex, WebRequest request) {
        log.warn("Bad request at {}: {}", request.getDescription(false), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /**
     * Handle conflict errors (409) - Business logic conflicts.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(
            IllegalStateException ex, WebRequest request) {
        log.warn("Conflict at {}: {}", request.getDescription(false), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /**
     * Handle HTTP client errors (502) - External API failures.
     */
    @ExceptionHandler(HttpClientException.class)
    public ResponseEntity<Map<String, Object>> handleHttpClientError(
            HttpClientException ex, WebRequest request) {
        log.error("HTTP client error at {}: {}", request.getDescription(false), ex.getMessage());
        return buildResponse(HttpStatus.BAD_GATEWAY, "External API error", request);
    }

    /**
     * Handle database integrity violations (409) - Constraint violations.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, WebRequest request) {
        log.warn("Data integrity violation at {}: {}", request.getDescription(false), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "Data constraint violation", request);
    }

    /**
     * Handle all other exceptions (500) - Catch-all for unexpected errors.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleServerError(
            Exception ex, WebRequest request) {
        log.error("Unexpected server error at {}", request.getDescription(false), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request);
    }

    /**
     * Helper method to build consistent error response JSON.
     */
    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String message, WebRequest request) {

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.status(status).body(body);
    }
}
