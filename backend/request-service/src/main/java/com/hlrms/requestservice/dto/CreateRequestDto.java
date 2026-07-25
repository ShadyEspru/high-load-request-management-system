package com.hlrms.requestservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRequestDto(

    @NotBlank(message = "Request type is required")
    @Size(
        max = 100,
        message = "Request type must not exceed 100 characters"
    )
    String requestType,

    @NotBlank(message = "Payload is required")
    @Size(
        max = 10_000,
        message = "Payload must not exceed 10000 characters"
    )
    String payload

) {
}