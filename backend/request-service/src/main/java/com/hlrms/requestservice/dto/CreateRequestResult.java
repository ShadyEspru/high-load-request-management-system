package com.hlrms.requestservice.dto;

public record CreateRequestResult(
    RequestResponseDto request,
    boolean replayed
) {
}