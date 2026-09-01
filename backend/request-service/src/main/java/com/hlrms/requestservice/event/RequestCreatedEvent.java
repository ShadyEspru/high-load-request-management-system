package com.hlrms.requestservice.event;

import java.time.Instant;
import java.util.UUID;

public record RequestCreatedEvent(
    UUID eventId,
    UUID requestId,
    String eventType,
    int eventVersion,
    Instant occurredAt
) {

    public static RequestCreatedEvent of(UUID requestId) {
        return new RequestCreatedEvent(
            UUID.randomUUID(),
            requestId,
            "REQUEST_CREATED",
            1,
            Instant.now()
        );
    }
}