package com.hlrms.requestworker.messaging;

import com.hlrms.requestworker.event.RequestCreatedEvent;
import com.hlrms.requestworker.service.IdempotentRequestProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RequestEventConsumerTest {

    @Mock
    private IdempotentRequestProcessingService
        idempotentRequestProcessingService;

    private RequestEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer =
            new RequestEventConsumer(
                idempotentRequestProcessingService
            );
    }

    @Test
    void shouldProcessValidRequestCreatedEvent() {

        RequestCreatedEvent event =
            new RequestCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "REQUEST_CREATED",
                1,
                Instant.now()
            );

        consumer.consumeRequestCreated(event);

        verify(idempotentRequestProcessingService)
            .processEvent(event);
    }

    @Test
    void shouldRejectEventWithNullEventId() {

        RequestCreatedEvent event =
            new RequestCreatedEvent(
                null,
                UUID.randomUUID(),
                "REQUEST_CREATED",
                1,
                Instant.now()
            );

        assertThatThrownBy(
            () -> consumer.consumeRequestCreated(event)
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Event ID must not be null");

        verify(
            idempotentRequestProcessingService,
            never()
        )
        .processEvent(event);
    }

    @Test
    void shouldRejectEventWithNullRequestId() {

        RequestCreatedEvent event =
            new RequestCreatedEvent(
                UUID.randomUUID(),
                null,
                "REQUEST_CREATED",
                1,
                Instant.now()
            );

        assertThatThrownBy(
            () -> consumer.consumeRequestCreated(event)
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Request ID must not be null");

        verify(
            idempotentRequestProcessingService,
            never()
        )
        .processEvent(event);
    }

    @Test
    void shouldRejectUnsupportedEventType() {

        RequestCreatedEvent event =
            new RequestCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "REQUEST_UPDATED",
                1,
                Instant.now()
            );

        assertThatThrownBy(
            () -> consumer.consumeRequestCreated(event)
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "Unsupported event type: REQUEST_UPDATED"
        );

        verify(
            idempotentRequestProcessingService,
            never()
        )
        .processEvent(event);
    }

    @Test
    void shouldRejectZeroEventVersion() {

        RequestCreatedEvent event =
            new RequestCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "REQUEST_CREATED",
                0,
                Instant.now()
            );

        assertThatThrownBy(
            () -> consumer.consumeRequestCreated(event)
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "Event version must be greater than zero"
        );

        verify(
            idempotentRequestProcessingService,
            never()
        )
        .processEvent(event);
    }

    @Test
    void shouldRejectNegativeEventVersion() {

        RequestCreatedEvent event =
            new RequestCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "REQUEST_CREATED",
                -1,
                Instant.now()
            );

        assertThatThrownBy(
            () -> consumer.consumeRequestCreated(event)
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "Event version must be greater than zero"
        );

        verify(
            idempotentRequestProcessingService,
            never()
        )
        .processEvent(event);
    }

    @Test
    void shouldRejectEventWithNullOccurredAt() {

        RequestCreatedEvent event =
            new RequestCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "REQUEST_CREATED",
                1,
                null
            );

        assertThatThrownBy(
            () -> consumer.consumeRequestCreated(event)
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "Event occurredAt must not be null"
        );

        verify(
            idempotentRequestProcessingService,
            never()
        )
        .processEvent(event);
    }
}