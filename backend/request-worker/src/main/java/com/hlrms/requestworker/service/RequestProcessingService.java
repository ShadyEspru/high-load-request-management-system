package com.hlrms.requestworker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestProcessingService {

    private final RequestStatusTransactionService
        statusTransactionService;

    @Value("${hlrms.worker.force-failure:false}")
    private boolean forceFailure;

    public void processRequest(UUID requestId) {
        boolean shouldProcess =
            statusTransactionService
                .markAsProcessing(requestId);

        if (!shouldProcess) {
            return;
        }

        log.info(
            "Request processing started. requestId={}",
            requestId
        );

        simulateProcessing(requestId);

        String processingResult =
            "Request processed successfully " +
            "by request-worker";

        statusTransactionService.markAsCompleted(
            requestId,
            processingResult
        );

        log.info(
            "Request processing completed. requestId={}",
            requestId
        );
    }

    private void simulateProcessing(UUID requestId) {
        if (forceFailure) {
            throw new IllegalStateException(
                "Forced worker failure for testing. " +
                "requestId=" +
                requestId
            );
        }

        try {
            Thread.sleep(2_000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                "Request processing was interrupted",
                exception
            );
        }
    }
}