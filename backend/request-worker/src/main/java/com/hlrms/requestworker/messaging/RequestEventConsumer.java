package com.hlrms.requestworker.messaging;

import com.hlrms.requestworker.config.RabbitMqConstants;
import com.hlrms.requestworker.event.RequestCreatedEvent;
import com.hlrms.requestworker.service.IdempotentRequestProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestEventConsumer {

    private final IdempotentRequestProcessingService
        idempotentRequestProcessingService;

    @RabbitListener(
        queues = RabbitMqConstants.REQUEST_QUEUE
    )
    public void consumeRequestCreated(
        RequestCreatedEvent event
    ) {
        log.info(
            "Received RequestCreatedEvent. " +
            "eventId={}, requestId={}, eventType={}, " +
            "eventVersion={}, occurredAt={}",
            event.eventId(),
            event.requestId(),
            event.eventType(),
            event.eventVersion(),
            event.occurredAt()
        );

        validateEvent(event);

        idempotentRequestProcessingService.processEvent(
            event
        );
    }

    private void validateEvent(
        RequestCreatedEvent event
    ) {
        if (event.eventId() == null) {
            throw new IllegalArgumentException(
                "Event ID must not be null"
            );
        }

        if (event.requestId() == null) {
            throw new IllegalArgumentException(
                "Request ID must not be null"
            );
        }

        if (
            !"REQUEST_CREATED".equals(
                event.eventType()
            )
        ) {
            throw new IllegalArgumentException(
                "Unsupported event type: " +
                event.eventType()
            );
        }

        if (event.eventVersion() <= 0) {
            throw new IllegalArgumentException(
                "Event version must be greater than zero"
            );
        }

        if (event.occurredAt() == null) {
            throw new IllegalArgumentException(
                "Event occurredAt must not be null"
            );
        }
    }
}