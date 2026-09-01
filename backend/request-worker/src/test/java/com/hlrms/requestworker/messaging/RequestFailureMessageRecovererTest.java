package com.hlrms.requestworker.messaging;

import com.hlrms.requestworker.event.RequestCreatedEvent;
import com.hlrms.requestworker.service.RequestStatusTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestFailureMessageRecovererTest {

    @Mock
    private JacksonJsonMessageConverter
        messageConverter;

    @Mock
    private RequestStatusTransactionService
        statusTransactionService;

    @Mock
    private Message message;

    private RequestFailureMessageRecoverer recoverer;

    @BeforeEach
    void setUp() {
        recoverer =
            new RequestFailureMessageRecoverer(
                messageConverter,
                statusTransactionService
            );
    }

    private RequestCreatedEvent createEvent() {
        return new RequestCreatedEvent(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "REQUEST_CREATED",
            1,
            Instant.now()
        );
    }

    @Test
    void shouldMarkRequestAsFailedAndRejectMessage() {

        RequestCreatedEvent event =
            createEvent();

        IllegalStateException processingFailure =
            new IllegalStateException(
                "Worker processing failed"
            );

        when(
            messageConverter.fromMessage(message)
        )
        .thenReturn(event);

        assertThatThrownBy(
            () -> recoverer.recover(
                message,
                processingFailure
            )
        )
        .isInstanceOf(
            AmqpRejectAndDontRequeueException.class
        )
        .hasMessageContaining(
            "Retries exhausted"
        )
        .hasCause(processingFailure);

        verify(statusTransactionService)
            .markAsFailed(
                event.requestId(),
                "IllegalStateException: " +
                    "Worker processing failed"
            );
    }

    @Test
    void shouldUseRootCauseInFailureMessage() {

        RequestCreatedEvent event =
            createEvent();

        RuntimeException rootCause =
            new RuntimeException(
                "Database connection lost"
            );

        IllegalStateException wrapperCause =
            new IllegalStateException(
                "Processing wrapper",
                rootCause
            );

        when(
            messageConverter.fromMessage(message)
        )
        .thenReturn(event);

        assertThatThrownBy(
            () -> recoverer.recover(
                message,
                wrapperCause
            )
        )
        .isInstanceOf(
            AmqpRejectAndDontRequeueException.class
        )
        .hasCause(wrapperCause);

        verify(statusTransactionService)
            .markAsFailed(
                event.requestId(),
                "RuntimeException: " +
                    "Database connection lost"
            );
    }

    @Test
    void shouldUseDefaultTextWhenCauseHasNoMessage() {

        RequestCreatedEvent event =
            createEvent();

        IllegalStateException processingFailure =
            new IllegalStateException();

        when(
            messageConverter.fromMessage(message)
        )
        .thenReturn(event);

        assertThatThrownBy(
            () -> recoverer.recover(
                message,
                processingFailure
            )
        )
        .isInstanceOf(
            AmqpRejectAndDontRequeueException.class
        );

        verify(statusTransactionService)
            .markAsFailed(
                event.requestId(),
                "IllegalStateException: " +
                    "No error message"
            );
    }

    @Test
    void shouldTruncateLongFailureMessage() {

        RequestCreatedEvent event =
            createEvent();

        String longMessage =
            "x".repeat(3_000);

        IllegalArgumentException processingFailure =
            new IllegalArgumentException(
                longMessage
            );

        when(
            messageConverter.fromMessage(message)
        )
        .thenReturn(event);

        assertThatThrownBy(
            () -> recoverer.recover(
                message,
                processingFailure
            )
        )
        .isInstanceOf(
            AmqpRejectAndDontRequeueException.class
        );

        ArgumentCaptor<String> errorCaptor =
            ArgumentCaptor.forClass(String.class);

        verify(statusTransactionService)
            .markAsFailed(
                org.mockito.ArgumentMatchers.eq(
                    event.requestId()
                ),
                errorCaptor.capture()
            );

        assertThat(errorCaptor.getValue())
            .hasSize(2_000)
            .startsWith(
                "IllegalArgumentException: "
            );
    }

    @Test
    void shouldRejectUnexpectedMessageTypeWithoutUpdatingRequest() {

        when(
            messageConverter.fromMessage(message)
        )
        .thenReturn("unexpected-message");

        RuntimeException processingFailure =
            new RuntimeException(
                "Processing failed"
            );

        assertThatThrownBy(
            () -> recoverer.recover(
                message,
                processingFailure
            )
        )
        .isInstanceOf(
            AmqpRejectAndDontRequeueException.class
        )
        .hasCause(processingFailure);

        verify(
            statusTransactionService,
            never()
        )
        .markAsFailed(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void shouldStillRejectMessageWhenStatusUpdateFails() {

        RequestCreatedEvent event =
            createEvent();

        RuntimeException processingFailure =
            new RuntimeException(
                "Original processing failure"
            );

        when(
            messageConverter.fromMessage(message)
        )
        .thenReturn(event);

        doThrow(
            new IllegalStateException(
                "Could not update database"
            )
        )
        .when(statusTransactionService)
        .markAsFailed(
            org.mockito.ArgumentMatchers.eq(
                event.requestId()
            ),
            org.mockito.ArgumentMatchers.anyString()
        );

        assertThatThrownBy(
            () -> recoverer.recover(
                message,
                processingFailure
            )
        )
        .isInstanceOf(
            AmqpRejectAndDontRequeueException.class
        )
        .hasMessageContaining(
            "Retries exhausted"
        )
        .hasCause(processingFailure);

        verify(statusTransactionService)
            .markAsFailed(
                org.mockito.ArgumentMatchers.eq(
                    event.requestId()
                ),
                org.mockito.ArgumentMatchers.anyString()
            );
    }

    @Test
    void shouldStillRejectWhenMessageConversionFails() {

        RuntimeException conversionFailure =
            new RuntimeException(
                "Invalid message payload"
            );

        RuntimeException processingFailure =
            new RuntimeException(
                "Original processing failure"
            );

        when(
            messageConverter.fromMessage(message)
        )
        .thenThrow(conversionFailure);

        assertThatThrownBy(
            () -> recoverer.recover(
                message,
                processingFailure
            )
        )
        .isInstanceOf(
            AmqpRejectAndDontRequeueException.class
        )
        .hasCause(processingFailure);

        verify(
            statusTransactionService,
            never()
        )
        .markAsFailed(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString()
        );
    }
}