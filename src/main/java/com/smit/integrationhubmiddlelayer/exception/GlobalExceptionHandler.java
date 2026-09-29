package com.smit.integrationhubmiddlelayer.exception;

import com.smit.integrationhubmiddlelayer.dto.ErrorResponse;
import com.smit.integrationhubmiddlelayer.dto.FieldErrorDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Centralized exception handler for all REST endpoints.
 * Maps business and system exceptions to appropriate HTTP status codes and error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler
{
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex)
    {
        log.warn("Resource not found: {}", ex.getMessage()); //$NON-NLS-1$
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex)
    {
        log.warn("Request conflicts with current state: {}", ex.getMessage()); //$NON-NLS-1$
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex)
    {
        log.warn("Bad request: {}", ex.getMessage()); //$NON-NLS-1$
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex)
    {
        List<FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldErrorDetail)
                .toList();
        log.warn("Validation failed: {}", fieldErrors); //$NON-NLS-1$
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("VALIDATION_FAILED", "Request validation failed", fieldErrors)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Malformed JSON or a value of the wrong type (e.g. a non-numeric port) is a client error,
     * so it must not fall through to the catch-all 500 handler.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex)
    {
        log.warn("Malformed request body: {}", ex.getMostSpecificCause().getMessage()); //$NON-NLS-1$
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("MALFORMED_REQUEST", "Request body could not be parsed")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Unknown paths are a 404, not a server error. Logged at debug only: the public endpoint
     * is constantly probed by scanners (/.env, /.git/config, ...) which would flood the logs.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex)
    {
        log.debug("No resource found: {}", ex.getResourcePath()); //$NON-NLS-1$
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", "Resource not found")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex)
    {
        log.error("Unexpected error", ex); //$NON-NLS-1$
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private FieldErrorDetail toFieldErrorDetail(FieldError fieldError)
    {
        return new FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
    }
}
