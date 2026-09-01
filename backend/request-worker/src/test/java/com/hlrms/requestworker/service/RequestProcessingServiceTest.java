package com.hlrms.requestworker.service;

import com.hlrms.requestworker.metrics.WorkerMetrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;

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
    private JdbcTemplate jdbcTemplate;

    @Mock
    private TransferExecutionClient
        transferExecutionClient;

    @Mock
    private Timer.Sample timerSample;

    @Mock
    private ResultSet resultSet;

    private RequestProcessingService service;

    @BeforeEach
    void setUp() {

        service =
            new RequestProcessingService(
                workerMetrics,
                meterRegistry,
                statusTransactionService,
                jdbcTemplate,
                transferExecutionClient
            );

        ReflectionTestUtils.setField(
            service,
            "forceFailure",
            false
        );
    }

    @Test
    void shouldSkipAlreadyCompletedRequest() {

        UUID requestId =
            UUID.randomUUID();

        when(
            statusTransactionService
                .markAsProcessing(
                    requestId
                )
        )
        .thenReturn(false);

        service.processRequest(
            requestId
        );

        verify(
            statusTransactionService
        )
        .markAsProcessing(
            requestId
        );

        verify(
            statusTransactionService,
            never()
        )
        .markAsCompleted(
            any(),
            anyString()
        );

        verifyNoInteractions(
            workerMetrics,
            meterRegistry,
            jdbcTemplate,
            transferExecutionClient
        );
    }

    @Test
    void shouldExecuteMoneyTransferAndMarkCompleted()
            throws Exception {

        UUID requestId =
            UUID.randomUUID();

        UUID senderUserId =
            UUID.randomUUID();

        String recipientTransferId =
            "BLZWCKE5CT5EUGH7";

        String payload =
            """
            {
              "amount": 12.50,
              "currency": "USD",
              "recipientName": "Receiver",
              "recipientTransferId": "BLZWCKE5CT5EUGH7"
            }
            """;

        when(
            statusTransactionService
                .markAsProcessing(
                    requestId
                )
        )
        .thenReturn(true);

        when(
            workerMetrics
                .startProcessingTimer(
                    meterRegistry
                )
        )
        .thenReturn(
            timerSample
        );

        mockRequestRow(
            requestId,
            senderUserId,
            "MONEY_TRANSFER",
            payload
        );

        when(
            transferExecutionClient
                .execute(
                    eq(requestId),
                    eq(senderUserId),
                    eq(recipientTransferId),
                    argThat(
                        amount ->
                            amount != null &&
                            amount.compareTo(
                                new BigDecimal(
                                    "12.50"
                                )
                            ) == 0
                    ),
                    eq("USD")
                )
        )
        .thenReturn(
            """
            {"status":"COMPLETED"}
            """
        );

        service.processRequest(
            requestId
        );

        verify(
            transferExecutionClient
        )
        .execute(
            eq(requestId),
            eq(senderUserId),
            eq(recipientTransferId),
            argThat(
                amount ->
                    amount != null &&
                    amount.compareTo(
                        new BigDecimal(
                            "12.50"
                        )
                    ) == 0
            ),
            eq("USD")
        );

        verify(
            statusTransactionService
        )
        .markAsCompleted(
            requestId,
            """
            {"status":"COMPLETED"}
            """
        );

        verify(
            statusTransactionService,
            never()
        )
        .markAsFailed(
            any(),
            anyString()
        );

        verify(
            workerMetrics
        )
        .requestCompleted();

        verify(
            workerMetrics
        )
        .recordProcessingTime(
            timerSample
        );
    }

    @Test
    void shouldMarkRejectedTransferAsFailed()
            throws Exception {

        UUID requestId =
            UUID.randomUUID();

        UUID senderUserId =
            UUID.randomUUID();

        String payload =
            """
            {
              "amount": 20000.00,
              "currency": "USD",
              "recipientTransferId": "BLZWCKE5CT5EUGH7"
            }
            """;

        when(
            statusTransactionService
                .markAsProcessing(
                    requestId
                )
        )
        .thenReturn(true);

        when(
            workerMetrics
                .startProcessingTimer(
                    meterRegistry
                )
        )
        .thenReturn(
            timerSample
        );

        mockRequestRow(
            requestId,
            senderUserId,
            "MONEY_TRANSFER",
            payload
        );

        when(
            transferExecutionClient
                .execute(
                    eq(requestId),
                    eq(senderUserId),
                    eq("BLZWCKE5CT5EUGH7"),
                    argThat(
                        amount ->
                            amount != null &&
                            amount.compareTo(
                                new BigDecimal(
                                    "20000.00"
                                )
                            ) == 0
                    ),
                    eq("USD")
                )
        )
        .thenThrow(
            new TransferRejectedException(
                "الرصيد غير كافٍ لإتمام الحوالة"
            )
        );

        service.processRequest(
            requestId
        );

        verify(
            statusTransactionService
        )
        .markAsFailed(
            requestId,
            "الرصيد غير كافٍ لإتمام الحوالة"
        );

        verify(
            statusTransactionService,
            never()
        )
        .markAsCompleted(
            any(),
            anyString()
        );

        verify(
            workerMetrics,
            never()
        )
        .requestCompleted();

        verify(
            workerMetrics
        )
        .recordProcessingTime(
            timerSample
        );
    }

    @Test
    void shouldPropagateForcedFailureWithoutMarkingCompleted() {

        UUID requestId =
            UUID.randomUUID();

        ReflectionTestUtils.setField(
            service,
            "forceFailure",
            true
        );

        when(
            statusTransactionService
                .markAsProcessing(
                    requestId
                )
        )
        .thenReturn(true);

        when(
            workerMetrics
                .startProcessingTimer(
                    meterRegistry
                )
        )
        .thenReturn(
            timerSample
        );

        assertThatThrownBy(
            () ->
                service.processRequest(
                    requestId
                )
        )
        .isInstanceOf(
            IllegalStateException.class
        )
        .hasMessageContaining(
            "Forced worker failure for testing"
        );

        verify(
            statusTransactionService,
            never()
        )
        .markAsCompleted(
            any(),
            anyString()
        );

        verify(
            statusTransactionService,
            never()
        )
        .markAsFailed(
            any(),
            anyString()
        );

        verify(
            workerMetrics,
            never()
        )
        .requestCompleted();

        /*
         * يتم تسجيل الزمن داخل finally،
         * لذلك يجب أن يحدث حتى عند الاستثناء.
         */
        verify(
            workerMetrics
        )
        .recordProcessingTime(
            timerSample
        );

        verifyNoInteractions(
            jdbcTemplate,
            transferExecutionClient
        );
    }

    @SuppressWarnings({
        "unchecked",
        "rawtypes"
    })
    private void mockRequestRow(
            UUID requestId,
            UUID senderUserId,
            String requestType,
            String payload
    ) throws Exception {

        when(
            resultSet.getObject(
                "user_id",
                UUID.class
            )
        )
        .thenReturn(
            senderUserId
        );

        when(
            resultSet.getString(
                "request_type"
            )
        )
        .thenReturn(
            requestType
        );

        when(
            resultSet.getString(
                "payload"
            )
        )
        .thenReturn(
            payload
        );

        when(
            jdbcTemplate.queryForObject(
                anyString(),
                any(RowMapper.class),
                eq(requestId)
            )
        )
        .thenAnswer(
            invocation -> {

                RowMapper mapper =
                    invocation.getArgument(
                        1
                    );

                return mapper.mapRow(
                    resultSet,
                    0
                );
            }
        );
    }
}
