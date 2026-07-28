package com.hlrms.requestservice.dto;

import com.hlrms.requestservice.entity.RequestStatus;

import java.time.Instant;
import java.util.UUID;

public record RequestResponseDto(
    UUID id,
    String idempotencyKey,
    String requestType,
    String payload,
    RequestStatus status,
    String result,
    String errorMessage,
    Instant createdAt,
    Instant updatedAt,
    Instant completedAt,
    Long version
) {
}