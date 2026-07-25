package com.hlrms.requestservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RequestNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRequestNotFound(
        RequestNotFoundException exception
    ) {
        return buildErrorResponse(
            HttpStatus.NOT_FOUND,
            exception.getMessage(),
            null
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
        MethodArgumentNotValidException exception
    ) {
        Map<String, String> validationErrors =
            new LinkedHashMap<>();

        for (FieldError fieldError :
            exception.getBindingResult().getFieldErrors()) {

            validationErrors.put(
                fieldError.getField(),
                fieldError.getDefaultMessage()
            );
        }

        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            validationErrors
        );
    }

    @ExceptionHandler(
        MethodArgumentTypeMismatchException.class
    )
    public ResponseEntity<Map<String, Object>>
    handleTypeMismatch(
        MethodArgumentTypeMismatchException exception
    ) {
        String message =
            "Invalid value for parameter: "
                + exception.getName();

        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            message,
            null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>>
    handleUnexpectedException(Exception exception) {

        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred",
            null
        );
    }

    private ResponseEntity<Map<String, Object>>
    buildErrorResponse(
        HttpStatus status,
        String message,
        Object details
    ) {
        Map<String, Object> response =
            new LinkedHashMap<>();

        response.put(
            "timestamp",
            Instant.now().toString()
        );

        response.put(
            "status",
            status.value()
        );

        response.put(
            "error",
            status.getReasonPhrase()
        );

        response.put(
            "message",
            message
        );

        if (details != null) {
            response.put("details", details);
        }

        return ResponseEntity
            .status(status)
            .body(response);
    }
}