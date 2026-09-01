package com.hlrms.transferapi.dto;

public record TransferErrorResponse(
    String code,
    String message
) {
}
