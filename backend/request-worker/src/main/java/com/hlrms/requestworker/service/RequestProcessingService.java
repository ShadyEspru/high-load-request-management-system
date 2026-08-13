package com.hlrms.requestworker.service;

import com.hlrms.requestworker.metrics.WorkerMetrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestProcessingService {

    private final WorkerMetrics workerMetrics;

    private final MeterRegistry meterRegistry;

    private final RequestStatusTransactionService
        statusTransactionService;

    private final JdbcTemplate jdbcTemplate;

    private final TransferExecutionClient
        transferExecutionClient;

    private final JsonMapper jsonMapper =
        JsonMapper.builder().build();

    @Value("${hlrms.worker.force-failure:false}")
    private boolean forceFailure;

    public void processRequest(
            UUID requestId
    ) {

        boolean shouldProcess =
            statusTransactionService
                .markAsProcessing(
                    requestId
                );

        if (!shouldProcess) {
            return;
        }

        Timer.Sample sample =
            workerMetrics
                .startProcessingTimer(
                    meterRegistry
                );

        log.info(
            "Request processing started. requestId={}",
            requestId
        );

        try {

            if (forceFailure) {
                throw new IllegalStateException(
                    "Forced worker failure for testing"
                );
            }

            RequestData request =
                loadRequest(
                    requestId
                );

            String processingResult;

            if (
                "MONEY_TRANSFER"
                    .equalsIgnoreCase(
                        request.requestType()
                    )
            ) {

                processingResult =
                    processMoneyTransfer(
                        requestId,
                        request
                    );

            } else {

                simulateProcessing();

                processingResult =
                    "Request processed successfully " +
                    "by request-worker";
            }

            statusTransactionService
                .markAsCompleted(
                    requestId,
                    processingResult
                );

            workerMetrics.requestCompleted();

            log.info(
                "Request processing completed. requestId={}",
                requestId
            );

        } catch (
            TransferRejectedException exception
        ) {

            statusTransactionService
                .markAsFailed(
                    requestId,
                    exception.getMessage()
                );

            log.warn(
                "Transfer rejected. requestId={}, reason={}",
                requestId,
                exception.getMessage()
            );

        } finally {

            workerMetrics
                .recordProcessingTime(
                    sample
                );
        }
    }

    private String processMoneyTransfer(
            UUID requestId,
            RequestData request
    ) {

        try {

            JsonNode root =
                jsonMapper.readTree(
                    request.payload()
                );

            JsonNode recipientNode =
                root.get(
                    "recipientTransferId"
                );

            JsonNode amountNode =
                root.get(
                    "amount"
                );

            JsonNode currencyNode =
                root.get(
                    "currency"
                );

            if (
                recipientNode == null ||
                amountNode == null ||
                currencyNode == null
            ) {
                throw new IllegalArgumentException(
                    "Invalid transfer payload"
                );
            }

            String recipientTransferId =
                recipientNode
                    .asText()
                    .trim()
                    .toUpperCase();

            BigDecimal amount =
                amountNode
                    .decimalValue();

            String currency =
                currencyNode
                    .asText()
                    .trim()
                    .toUpperCase();

            return transferExecutionClient
                .execute(
                    requestId,
                    request.senderUserId(),
                    recipientTransferId,
                    amount,
                    currency
                );

        } catch (
            TransferRejectedException exception
        ) {
            throw exception;

        } catch (Exception exception) {

            throw new IllegalStateException(
                "Unable to process money transfer",
                exception
            );
        }
    }

    private RequestData loadRequest(
            UUID requestId
    ) {

        return jdbcTemplate
            .queryForObject(
                """
                SELECT
                    user_id,
                    request_type,
                    payload
                FROM requests
                WHERE id = ?
                """,

                (resultSet, rowNum) ->
                    new RequestData(
                        resultSet.getObject(
                            "user_id",
                            UUID.class
                        ),

                        resultSet.getString(
                            "request_type"
                        ),

                        resultSet.getString(
                            "payload"
                        )
                    ),

                requestId
            );
    }

    private void simulateProcessing() {

        try {

            Thread.sleep(
                2_000
            );

        } catch (
            InterruptedException exception
        ) {

            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException(
                "Request processing was interrupted",
                exception
            );
        }
    }

    private record RequestData(
        UUID senderUserId,
        String requestType,
        String payload
    ) {
    }
}
