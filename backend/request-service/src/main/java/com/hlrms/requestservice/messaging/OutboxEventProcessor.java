package com.hlrms.requestservice.messaging;

import com.hlrms.requestservice.entity.OutboxEvent;
import com.hlrms.requestservice.entity.OutboxEventStatus;
import com.hlrms.requestservice.event.RequestCreatedEvent;
import com.hlrms.requestservice.service.OutboxEventTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition
    .ConditionalOnProperty;
import org.springframework.boot.context.event
    .ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "hlrms.outbox",
    name = "publisher-enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class OutboxEventProcessor {

    private final OutboxEventTransactionService
        outboxEventTransactionService;

    private final RequestEventPublisher
        requestEventPublisher;

    private final JsonMapper jsonMapper;

    @Value("${hlrms.outbox.batch-size:20}")
    private int batchSize;

    @Value("${hlrms.outbox.max-attempts:10}")
    private int maxAttempts;

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedEvents() {
        outboxEventTransactionService
            .recoverInterruptedEvents();
    }

    @Scheduled(
        initialDelayString =
            "${hlrms.outbox.initial-delay:2000}",
        fixedDelayString =
            "${hlrms.outbox.fixed-delay:1000}"
    )
    public void processPendingEvents() {
        List<UUID> eventIds =
            outboxEventTransactionService
                .claimPendingEvents(batchSize);

        if (eventIds.isEmpty()) {
            return;
        }

        log.info(
            "Claimed outbox event batch. count={}",
            eventIds.size()
        );

        for (UUID eventId : eventIds) {
            processSingleEvent(eventId);
        }
    }

    private void processSingleEvent(UUID eventId) {
        try {
            OutboxEvent outboxEvent =
                outboxEventTransactionService
                    .getRequiredEvent(eventId);

            if (
                outboxEvent.getStatus() !=
                OutboxEventStatus.PROCESSING
            ) {
                log.warn(
                    "Skipping outbox event with " +
                    "unexpected status. " +
                    "eventId={}, status={}",
                    eventId,
                    outboxEvent.getStatus()
                );

                return;
            }

            RequestCreatedEvent event =
                deserializeRequestCreatedEvent(
                    outboxEvent.getPayload()
                );

            requestEventPublisher
                .publishRequestCreated(event);

            outboxEventTransactionService
                .markAsPublished(eventId);

            log.info(
                "Outbox event published. " +
                "eventId={}, requestId={}",
                event.eventId(),
                event.requestId()
            );
        } catch (Exception exception) {
            log.error(
                "Outbox event publishing failed. " +
                "eventId={}",
                eventId,
                exception
            );

            outboxEventTransactionService
                .markPublishingFailed(
                    eventId,
                    exception,
                    maxAttempts
                );
        }
    }

    private RequestCreatedEvent
    deserializeRequestCreatedEvent(
        String payload
    ) {
        try {
            return jsonMapper.readValue(
                payload,
                RequestCreatedEvent.class
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                "Could not deserialize " +
                "RequestCreatedEvent",
                exception
            );
        }
    }
}