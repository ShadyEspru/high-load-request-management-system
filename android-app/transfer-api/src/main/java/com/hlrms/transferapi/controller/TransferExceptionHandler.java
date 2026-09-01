package com.hlrms.transferapi.controller;

import com.hlrms.transferapi.dto.TransferErrorResponse;
import com.hlrms.transferapi.exception.TransferBusinessException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TransferExceptionHandler {

    @ExceptionHandler(
        TransferBusinessException.class
    )
    public ResponseEntity<TransferErrorResponse>
    handleBusinessError(
            TransferBusinessException exception
    ) {

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                new TransferErrorResponse(
                    exception.getCode(),
                    exception.getMessage()
                )
            );
    }
}
