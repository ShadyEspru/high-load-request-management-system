package com.hlrms.requestservice.messaging;

import com.hlrms.requestservice.event.RequestCreatedEvent;
import com.hlrms.requestservice.messaging.RequestEventPublisher.PendingPublish;
import com.hlrms.requestservice.service.OutboxEventTransactionService;
import com.hlrms.requestservice.service.OutboxEventTransactionService.ClaimedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
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

    private record PendingOutboxPublish(
        UUID outboxEventId,
        PendingPublish pendingPublish
    ) {
    }

    @EventListener(
        ApplicationReadyEvent.class
    )
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

        List<ClaimedEvent> events =
            outboxEventTransactionService
                .claimPendingEvents(
                    batchSize
                );

        if (events.isEmpty()) {
            return;
        }

        log.info(
            "Claimed outbox event batch. count={}",
            events.size()
        );

        List<PendingOutboxPublish>
            pendingPublishes =
                new ArrayList<>(
                    events.size()
                );

        /*
         * Phase 1:
         * Send the complete batch without waiting
         * for publisher confirms message-by-message.
         */
        for (
            ClaimedEvent claimedEvent :
            events
        ) {

            try {

                RequestCreatedEvent event =
                    deserializeRequestCreatedEvent(
                        claimedEvent.payload()
                    );

                PendingPublish pendingPublish =
                    requestEventPublisher
                        .publishRequestCreatedAsync(
                            event
                        );

                pendingPublishes.add(
                    new PendingOutboxPublish(
                        claimedEvent.id(),
                        pendingPublish
                    )
                );

            } catch (Exception exception) {

                handlePublishingFailure(
                    claimedEvent.id(),
                    exception
                );
            }
        }

        /*
         * Phase 2:
         * Messages are already in flight.
         * Now collect their correlated confirms.
         */
        List<UUID> publishedEventIds =
            new ArrayList<>(
                pendingPublishes.size()
            );

        for (
            PendingOutboxPublish pending :
            pendingPublishes
        ) {

            try {

                requestEventPublisher
                    .awaitConfirmation(
                        pending.pendingPublish()
                    );

                publishedEventIds.add(
                    pending.outboxEventId()
                );

            } catch (Exception exception) {

                handlePublishingFailure(
                    pending.outboxEventId(),
                    exception
                );
            }
        }

        if (
            publishedEventIds.isEmpty()
        ) {
            return;
        }

        /*
         * Phase 3:
         * One database transaction marks all
         * confirmed events PUBLISHED.
         */
        try {

            outboxEventTransactionService
                .markAsPublished(
                    publishedEventIds
                );

            log.info(
                "Outbox event batch marked " +
                "as PUBLISHED. count={}",
                publishedEventIds.size()
            );

        } catch (Exception exception) {

            log.error(
                "Could not mark published " +
                "outbox batch. count={}",
                publishedEventIds.size(),
                exception
            );

            for (
                UUID eventId :
                publishedEventIds
            ) {

                try {

                    outboxEventTransactionService
                        .markPublishingFailed(
                            eventId,
                            exception,
                            maxAttempts
                        );

                } catch (
                    Exception recoveryException
                ) {

                    log.error(
                        "Could not recover outbox " +
                        "event after batch update " +
                        "failure. eventId={}",
                        eventId,
                        recoveryException
                    );
                }
            }
        }
    }

    private void handlePublishingFailure(
        UUID eventId,
        Exception exception
    ) {

        log.error(
            "Outbox event publishing failed. " +
            "eventId={}",
            eventId,
            exception
        );

        try {

            outboxEventTransactionService
                .markPublishingFailed(
                    eventId,
                    exception,
                    maxAttempts
                );

        } catch (Exception recoveryException) {

            log.error(
                "Could not record outbox " +
                "publishing failure. eventId={}",
                eventId,
                recoveryException
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

        } catch (
            JacksonException exception
        ) {

            throw new IllegalStateException(
                "Could not deserialize " +
                "RequestCreatedEvent",
                exception
            );
        }
    }
}
