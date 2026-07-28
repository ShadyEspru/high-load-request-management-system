package com.hlrms.requestworker.event;

import java.time.Instant;
import java.util.UUID;

public record RequestCreatedEvent(
    UUID eventId,
    UUID requestId,
    String eventType,
    int eventVersion,
    Instant occurredAt
) {
}
