package com.hlrms.requestservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;


import java.util.LinkedHashMap;
import java.util.Map;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
        LoggerFactory.getLogger(
            GlobalExceptionHandler.class
        );

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
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>>
    handleHandlerMethodValidationException(
        HandlerMethodValidationException exception
    ) {
        Map<String, String> validationErrors =
            new LinkedHashMap<>();

        exception.getParameterValidationResults()
            .forEach(result -> {
                String parameterName = result
                    .getMethodParameter()
                    .getParameterName();

                if (parameterName == null) {
                    parameterName = "parameter";
                }

                String message = result
                    .getResolvableErrors()
                    .stream()
                    .map(MessageSourceResolvable::getDefaultMessage)
                    .filter(value -> value != null)
                    .findFirst()
                    .orElse("Invalid value");

                validationErrors.put(
                    parameterName,
                    message
                );
            });

        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            validationErrors
        );
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<Map<String, Object>>
    handleIdempotencyConflict(
        IdempotencyConflictException exception
    ) {
        return buildErrorResponse(
            HttpStatus.CONFLICT,
            exception.getMessage(),
            null
        );
    }

    @ExceptionHandler(MessagePublishingException.class)
    public ResponseEntity<Map<String, Object>>
    handleMessagePublishingException(
        MessagePublishingException exception
    ) {
        return buildErrorResponse(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Request was saved, but the processing event " +
            "could not be published",
            Map.of(
                "reason",
                exception.getMessage()
            )
        );
    }

    @ExceptionHandler(
        TrustedUserHeaderException.class
    )
    public ResponseEntity<Map<String, Object>>
    handleTrustedUserHeaderException(
        TrustedUserHeaderException exception
    ) {
        return buildErrorResponse(
            HttpStatus.UNAUTHORIZED,
            exception.getMessage(),
            null
        );
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>>
    handleForbiddenException(
            ForbiddenException exception
    ) {
        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                exception.getMessage(),
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>>
    handleGenericException(
        Exception exception
    ) {
        log.error(
            "Unhandled exception in request-service",
            exception
        );

        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred",
            null
        );
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>>
    handleMissingRequestHeader(
        MissingRequestHeaderException exception
    ) {
        Map<String, String> details = new LinkedHashMap<>();

        details.put(
            exception.getHeaderName(),
            exception.getHeaderName() + " header is required"
        );

        return buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Required request header is missing",
            details
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