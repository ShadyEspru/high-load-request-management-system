package com.hlrms.requestworker.service;

import com.hlrms.requestworker.entity.RequestEntity;
import com.hlrms.requestworker.entity.RequestStatus;
import com.hlrms.requestworker.repository.RequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestStatusTransactionServiceTest {

    @Mock
    private RequestRepository requestRepository;

    private RequestStatusTransactionService service;

    @BeforeEach
    void setUp() {
        service =
            new RequestStatusTransactionService(
                requestRepository
            );
    }

    private RequestEntity createRequest(
        UUID requestId,
        RequestStatus status
    ) {
        return new RequestEntity(
            requestId,
            "TEST",
            "{\"message\":\"test\"}",
            status,
            null,
            null,
            Instant.now(),
            Instant.now(),
            null,
            0L,
            "test-key-" + requestId,
            "a".repeat(64)
        );
    }

    @Test
    void shouldMarkPendingRequestAsProcessing() {

        UUID requestId = UUID.randomUUID();

        RequestEntity request =
            createRequest(
                requestId,
                RequestStatus.PENDING
            );

        when(
            requestRepository.findById(requestId)
        )
        .thenReturn(Optional.of(request));

        boolean result =
            service.markAsProcessing(requestId);

        assertThat(result)
            .isTrue();

        assertThat(request.getStatus())
            .isEqualTo(RequestStatus.PROCESSING);

        assertThat(request.getResult())
            .isNull();

        assertThat(request.getErrorMessage())
            .isNull();

        assertThat(request.getCompletedAt())
            .isNull();

        verify(requestRepository)
            .saveAndFlush(request);
    }

    @Test
    void shouldNotProcessAlreadyCompletedRequest() {

        UUID requestId = UUID.randomUUID();

        RequestEntity request =
            createRequest(
                requestId,
                RequestStatus.COMPLETED
            );

        when(
            requestRepository.findById(requestId)
        )
        .thenReturn(Optional.of(request));

        boolean result =
            service.markAsProcessing(requestId);

        assertThat(result)
            .isFalse();

        assertThat(request.getStatus())
            .isEqualTo(RequestStatus.COMPLETED);

        verify(
            requestRepository,
            never()
        )
        .saveAndFlush(request);
    }

    @Test
    void shouldMarkRequestAsCompleted() {

        UUID requestId = UUID.randomUUID();

        RequestEntity request =
            createRequest(
                requestId,
                RequestStatus.PROCESSING
            );

        when(
            requestRepository.findById(requestId)
        )
        .thenReturn(Optional.of(request));

        service.markAsCompleted(
            requestId,
            "Completed successfully"
        );

        assertThat(request.getStatus())
            .isEqualTo(RequestStatus.COMPLETED);

        assertThat(request.getResult())
            .isEqualTo("Completed successfully");

        assertThat(request.getErrorMessage())
            .isNull();

        assertThat(request.getCompletedAt())
            .isNotNull();

        verify(requestRepository)
            .saveAndFlush(request);
    }

    @Test
    void shouldNotModifyAlreadyCompletedRequestAgain() {

        UUID requestId = UUID.randomUUID();

        RequestEntity request =
            createRequest(
                requestId,
                RequestStatus.COMPLETED
            );

        when(
            requestRepository.findById(requestId)
        )
        .thenReturn(Optional.of(request));

        service.markAsCompleted(
            requestId,
            "Another result"
        );

        verify(
            requestRepository,
            never()
        )
        .saveAndFlush(request);
    }

    @Test
    void shouldMarkRequestAsFailed() {

        UUID requestId = UUID.randomUUID();

        RequestEntity request =
            createRequest(
                requestId,
                RequestStatus.PROCESSING
            );

        when(
            requestRepository.findById(requestId)
        )
        .thenReturn(Optional.of(request));

        service.markAsFailed(
            requestId,
            "Processing failed"
        );

        assertThat(request.getStatus())
            .isEqualTo(RequestStatus.FAILED);

        assertThat(request.getResult())
            .isNull();

        assertThat(request.getErrorMessage())
            .isEqualTo("Processing failed");

        assertThat(request.getCompletedAt())
            .isNotNull();

        verify(requestRepository)
            .saveAndFlush(request);
    }

    @Test
    void shouldNotChangeCompletedRequestToFailed() {

        UUID requestId = UUID.randomUUID();

        RequestEntity request =
            createRequest(
                requestId,
                RequestStatus.COMPLETED
            );

        when(
            requestRepository.findById(requestId)
        )
        .thenReturn(Optional.of(request));

        service.markAsFailed(
            requestId,
            "Late failure"
        );

        assertThat(request.getStatus())
            .isEqualTo(RequestStatus.COMPLETED);

        verify(
            requestRepository,
            never()
        )
        .saveAndFlush(request);
    }

    @Test
    void shouldThrowWhenRequestDoesNotExist() {

        UUID requestId = UUID.randomUUID();

        when(
            requestRepository.findById(requestId)
        )
        .thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> service.markAsProcessing(
                requestId
            )
        )
        .isInstanceOf(
            IllegalArgumentException.class
        )
        .hasMessage(
            "Request not found: " + requestId
        );

        verify(
            requestRepository,
            never()
        )
        .saveAndFlush(
            org.mockito.ArgumentMatchers.any()
        );
    }
}