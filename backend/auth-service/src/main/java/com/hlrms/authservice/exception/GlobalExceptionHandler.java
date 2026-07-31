package com.hlrms.authservice.exception;

import com.hlrms.authservice.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(
            EmailAlreadyExistsException ex,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request,
                null
        );
    }

    @ExceptionHandler({
            InvalidCredentialsException.class,
            InvalidRefreshTokenException.class
    })
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            RuntimeException ex,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request,
                errors
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                request,
                null
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatusCode status,
            String message,
            HttpServletRequest request,
            List<String> validationErrors
    ) {

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.toString(),
                message,
                request.getRequestURI(),
                validationErrors
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }
}