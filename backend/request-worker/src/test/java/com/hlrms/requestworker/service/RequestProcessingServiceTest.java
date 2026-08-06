package com.hlrms.requestworker.service;

import com.hlrms.requestworker.metrics.WorkerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestProcessingServiceTest {

    @Mock
    private WorkerMetrics workerMetrics;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private RequestStatusTransactionService
        statusTransactionService;

    @Mock
    private Timer.Sample timerSample;

    private RequestProcessingService service;

    @BeforeEach
    void setUp() {
        service =
            new RequestProcessingService(
                workerMetrics,
                meterRegistry,
                statusTransactionService
            );

        ReflectionTestUtils.setField(
            service,
            "forceFailure",
            false
        );
    }

    @Test
    void shouldSkipAlreadyCompletedRequest() {

        UUID requestId = UUID.randomUUID();

        when(
            statusTransactionService
                .markAsProcessing(requestId)
        )
        .thenReturn(false);

        service.processRequest(requestId);

        verify(statusTransactionService)
            .markAsProcessing(requestId);

        verify(
            statusTransactionService,
            never()
        )
        .markAsCompleted(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString()
        );

        verifyNoInteractions(
            workerMetrics,
            meterRegistry
        );
    }

    @Test
    void shouldProcessRequestAndRecordSuccessMetrics() {

        UUID requestId = UUID.randomUUID();

        when(
            statusTransactionService
                .markAsProcessing(requestId)
        )
        .thenReturn(true);

        when(
            workerMetrics.startProcessingTimer(
                meterRegistry
            )
        )
        .thenReturn(timerSample);

        service.processRequest(requestId);

        var orderedCalls =
            inOrder(
                statusTransactionService,
                workerMetrics
            );

        orderedCalls.verify(
            statusTransactionService
        )
        .markAsProcessing(requestId);

        orderedCalls.verify(
            workerMetrics
        )
        .startProcessingTimer(meterRegistry);

        orderedCalls.verify(
            statusTransactionService
        )
        .markAsCompleted(
            requestId,
            "Request processed successfully " +
                "by request-worker"
        );

        orderedCalls.verify(
            workerMetrics
        )
        .requestCompleted();

        orderedCalls.verify(
            workerMetrics
        )
        .recordProcessingTime(timerSample);

        verify(
            workerMetrics,
            never()
        )
        .requestFailed();
    }

    @Test
    void shouldPropagateForcedFailureWithoutMarkingCompleted() {

        UUID requestId = UUID.randomUUID();

        ReflectionTestUtils.setField(
            service,
            "forceFailure",
            true
        );

        when(
            statusTransactionService
                .markAsProcessing(requestId)
        )
        .thenReturn(true);

        when(
            workerMetrics.startProcessingTimer(
                meterRegistry
            )
        )
        .thenReturn(timerSample);

        assertThatThrownBy(
            () -> service.processRequest(requestId)
        )
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(
            "Forced worker failure for testing"
        )
        .hasMessageContaining(
            requestId.toString()
        );

        verify(statusTransactionService)
            .markAsProcessing(requestId);

        verify(
            statusTransactionService,
            never()
        )
        .markAsCompleted(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString()
        );

        verify(
            workerMetrics,
            never()
        )
        .requestCompleted();

        verify(
            workerMetrics,
            never()
        )
        .recordProcessingTime(
            org.mockito.ArgumentMatchers.any()
        );
    }
}