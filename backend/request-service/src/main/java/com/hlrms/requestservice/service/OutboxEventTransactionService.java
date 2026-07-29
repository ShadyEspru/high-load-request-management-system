package com.hlrms.requestservice.service;

import com.hlrms.requestservice.entity.OutboxEvent;
import com.hlrms.requestservice.entity.OutboxEventStatus;
import com.hlrms.requestservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventTransactionService {

    private static final int
        MAX_ERROR_MESSAGE_LENGTH = 2_000;

    private final OutboxEventRepository
        outboxEventRepository;

    @Transactional(
        propagation = Propagation.REQUIRES_NEW
    )
    public List<UUID> claimPendingEvents(
        int batchSize
    ) {
        List<OutboxEvent> events =
            outboxEventRepository.findBatchForUpdate(
                OutboxEventStatus.PENDING,
                PageRequest.of(0, batchSize)
            );

        events.forEach(
            OutboxEvent::markAsProcessing
        );

        outboxEventRepository.saveAllAndFlush(events);

        return events
            .stream()
            .map(OutboxEvent::getId)
            .toList();
    }

    @Transactional(
        readOnly = true,
        propagation = Propagation.REQUIRES_NEW
    )
    public OutboxEvent getRequiredEvent(
        UUID eventId
    ) {
        return outboxEventRepository
            .findById(eventId)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "Outbox event not found: " +
                    eventId
                )
            );
    }

    @Transactional(
        propagation = Propagation.REQUIRES_NEW
    )
    public void markAsPublished(UUID eventId) {
        OutboxEvent event =
            getRequiredEventInsideTransaction(eventId);

        if (
            event.getStatus() ==
            OutboxEventStatus.PUBLISHED
        ) {
            return;
        }

        event.markAsPublished();

        outboxEventRepository.saveAndFlush(event);
    }

    @Transactional(
        propagation = Propagation.REQUIRES_NEW
    )
    public void markPublishingFailed(
        UUID eventId,
        Throwable cause,
        int maxAttempts
    ) {
        OutboxEvent event =
            getRequiredEventInsideTransaction(eventId);

        String errorMessage =
            buildErrorMessage(cause);

        int nextRetryCount =
            event.getRetryCount() + 1;

        if (nextRetryCount >= maxAttempts) {
            event.markAsFailed(errorMessage);

            log.error(
                "Outbox event permanently failed. " +
                "eventId={}, retryCount={}, error={}",
                eventId,
                nextRetryCount,
                errorMessage
            );
        } else {
            event.markAsPendingForRetry(
                errorMessage
            );

            log.warn(
                "Outbox event returned to PENDING. " +
                "eventId={}, retryCount={}, error={}",
                eventId,
                nextRetryCount,
                errorMessage
            );
        }

        outboxEventRepository.saveAndFlush(event);
    }

    @Transactional(
        propagation = Propagation.REQUIRES_NEW
    )
    public int recoverInterruptedEvents() {
        int recovered =
            outboxEventRepository
                .resetProcessingEventsToPending();

        if (recovered > 0) {
            log.warn(
                "Recovered interrupted PROCESSING " +
                "outbox events. count={}",
                recovered
            );
        }

        return recovered;
    }

    private OutboxEvent getRequiredEventInsideTransaction(
        UUID eventId
    ) {
        return outboxEventRepository
            .findById(eventId)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "Outbox event not found: " +
                    eventId
                )
            );
    }

    private String buildErrorMessage(
        Throwable cause
    ) {
        Throwable rootCause = cause;

        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        String errorMessage =
            rootCause
                .getClass()
                .getSimpleName() +
            ": " +
            (
                rootCause.getMessage() == null
                    ? "No error message"
                    : rootCause.getMessage()
            );

        if (
            errorMessage.length() <=
            MAX_ERROR_MESSAGE_LENGTH
        ) {
            return errorMessage;
        }

        return errorMessage.substring(
            0,
            MAX_ERROR_MESSAGE_LENGTH
        );
    }
}