package com.hlrms.requestworker.service;

import com.hlrms.requestworker.event.RequestCreatedEvent;
import com.hlrms.requestworker.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotentRequestProcessingServiceTest {

    @Mock
    private ProcessedEventRepository
        processedEventRepository;

    @Mock
    private RequestProcessingService
        requestProcessingService;

    private IdempotentRequestProcessingService
        service;

    private RequestCreatedEvent event;

    @BeforeEach
    void setUp() {
        service =
            new IdempotentRequestProcessingService(
                processedEventRepository,
                requestProcessingService
            );

        event =
            new RequestCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "REQUEST_CREATED",
                1,
                Instant.now()
            );
    }

    @Test
    void shouldProcessNewEventAndMarkItAsProcessed() {

        when(
            processedEventRepository.tryRegisterEvent(
                event.eventId(),
                event.requestId(),
                event.eventType(),
                event.eventVersion(),
                event.occurredAt()
            )
        )
        .thenReturn(1);

        when(
            processedEventRepository.markEventAsProcessed(
                event.eventId()
            )
        )
        .thenReturn(1);

        service.processEvent(event);

        verify(
            requestProcessingService
        )
        .processRequest(event.requestId());

        verify(
            processedEventRepository
        )
        .markEventAsProcessed(event.eventId());
    }

    @Test
    void shouldIgnoreDuplicateEvent() {

        when(
            processedEventRepository.tryRegisterEvent(
                event.eventId(),
                event.requestId(),
                event.eventType(),
                event.eventVersion(),
                event.occurredAt()
            )
        )
        .thenReturn(0);

        service.processEvent(event);

        verify(
            requestProcessingService,
            never()
        )
        .processRequest(event.requestId());

        verify(
            processedEventRepository,
            never()
        )
        .markEventAsProcessed(event.eventId());
    }

    @Test
    void shouldFailWhenEventCannotBeMarkedAsProcessed() {

        when(
            processedEventRepository.tryRegisterEvent(
                event.eventId(),
                event.requestId(),
                event.eventType(),
                event.eventVersion(),
                event.occurredAt()
            )
        )
        .thenReturn(1);

        when(
            processedEventRepository.markEventAsProcessed(
                event.eventId()
            )
        )
        .thenReturn(0);

        assertThatThrownBy(
            () -> service.processEvent(event)
        )
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(
            "Could not mark event as processed"
        );

        verify(
            requestProcessingService
        )
        .processRequest(event.requestId());
    }

    @Test
    void shouldNotMarkEventAsProcessedWhenRequestProcessingFails() {

        when(
            processedEventRepository.tryRegisterEvent(
                event.eventId(),
                event.requestId(),
                event.eventType(),
                event.eventVersion(),
                event.occurredAt()
            )
        )
        .thenReturn(1);

        doThrow(
            new IllegalStateException(
                "Processing failed"
            )
        )
        .when(requestProcessingService)
        .processRequest(event.requestId());

        assertThatThrownBy(
            () -> service.processEvent(event)
        )
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Processing failed");

        verify(
            processedEventRepository,
            never()
        )
        .markEventAsProcessed(event.eventId());
    }
}