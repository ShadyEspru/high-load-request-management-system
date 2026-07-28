package com.hlrms.requestworker.service;

import com.hlrms.requestworker.entity.RequestEntity;
import com.hlrms.requestworker.entity.RequestStatus;
import com.hlrms.requestworker.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestProcessingService {

    private final RequestRepository requestRepository;

    @Transactional
    public void processRequest(UUID requestId) {
        RequestEntity request =
            requestRepository
                .findById(requestId)
                .orElseThrow(
                    () -> new IllegalArgumentException(
                        "Request not found: " + requestId
                    )
                );

        if (request.getStatus() == RequestStatus.COMPLETED) {
            log.info(
                "Request is already completed. requestId={}",
                requestId
            );

            return;
        }

        request.markAsProcessing();
        requestRepository.saveAndFlush(request);

        log.info(
            "Request processing started. requestId={}",
            requestId
        );

        simulateProcessing();

        String processingResult =
            "Request processed successfully by request-worker";

        request.markAsCompleted(processingResult);
        requestRepository.save(request);

        log.info(
            "Request processing completed. requestId={}",
            requestId
        );
    }

    private void simulateProcessing() {
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
