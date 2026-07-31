package com.hlrms.authservice.dto.response;

import java.time.Instant;
import java.util.UUID;

public record RegisterResponse(

        UUID id,

        String email,

        String firstName,

        String lastName,

        Instant createdAt

) {
}