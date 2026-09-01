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
public class RequestStatusTransactionService {

    private final RequestRepository requestRepository;

    @Transactional
    public boolean markAsProcessing(UUID requestId) {
        RequestEntity request =
            getRequiredRequest(requestId);

        if (
            request.getStatus() ==
            RequestStatus.COMPLETED
        ) {
            log.info(
                "Request is already completed. requestId={}",
                requestId
            );

            return false;
        }

        request.markAsProcessing();

        requestRepository.saveAndFlush(request);

        return true;
    }

    @Transactional
    public void markAsCompleted(
        UUID requestId,
        String processingResult
    ) {
        RequestEntity request =
            getRequiredRequest(requestId);

        if (
            request.getStatus() ==
            RequestStatus.COMPLETED
        ) {
            return;
        }

        request.markAsCompleted(processingResult);

        requestRepository.saveAndFlush(request);
    }

    @Transactional
    public void markAsFailed(
        UUID requestId,
        String processingError
    ) {
        RequestEntity request =
            getRequiredRequest(requestId);

        if (
            request.getStatus() ==
            RequestStatus.COMPLETED
        ) {
            log.warn(
                "Completed request will not be changed " +
                "to FAILED. requestId={}",
                requestId
            );

            return;
        }

        request.markAsFailed(processingError);

        requestRepository.saveAndFlush(request);
    }

    private RequestEntity getRequiredRequest(
        UUID requestId
    ) {
        return requestRepository
            .findById(requestId)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "Request not found: " + requestId
                )
            );
    }
}