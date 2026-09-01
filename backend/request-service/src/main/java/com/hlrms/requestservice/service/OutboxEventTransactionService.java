package com.hlrms.requestservice.service;

import com.hlrms.requestservice.entity.OutboxEvent;
import com.hlrms.requestservice.entity.OutboxEventStatus;
import com.hlrms.requestservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
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

    private final JdbcTemplate jdbcTemplate;

    public record ClaimedEvent(
        UUID id,
        String payload
    ) {
    }

    @Transactional(
        propagation = Propagation.REQUIRES_NEW
    )
    public List<ClaimedEvent> claimPendingEvents(
        int batchSize
    ) {
        return jdbcTemplate.query(
            """
            WITH picked AS (
                SELECT id
                FROM outbox_events
                WHERE status = 'PENDING'
                ORDER BY created_at
                FOR UPDATE SKIP LOCKED
                LIMIT ?
            )
            UPDATE outbox_events AS event
            SET status = 'PROCESSING'
            FROM picked
            WHERE event.id = picked.id
            RETURNING
                event.id,
                event.payload::text AS payload
            """,
            preparedStatement ->
                preparedStatement.setInt(
                    1,
                    batchSize
                ),
            (resultSet, rowNum) ->
                new ClaimedEvent(
                    resultSet.getObject(
                        "id",
                        UUID.class
                    ),
                    resultSet.getString(
                        "payload"
                    )
                )
        );
    }

    @Transactional(
        propagation = Propagation.REQUIRES_NEW
    )
    public void markAsPublished(UUID eventId) {

        int updated =
            jdbcTemplate.update(
                """
                UPDATE outbox_events
                SET
                    status = 'PUBLISHED',
                    published_at = NOW(),
                    last_error = NULL
                WHERE id = ?
                  AND status = 'PROCESSING'
                """,
                eventId
            );

        if (updated == 1) {
            return;
        }

        String currentStatus =
            jdbcTemplate.queryForObject(
                """
                SELECT status
                FROM outbox_events
                WHERE id = ?
                """,
                String.class,
                eventId
            );

        if (
            OutboxEventStatus.PUBLISHED
                .name()
                .equals(currentStatus)
        ) {
            return;
        }

        throw new IllegalStateException(
            "Could not mark outbox event as " +
            "PUBLISHED. eventId=" +
            eventId +
            ", currentStatus=" +
            currentStatus
        );
    }

    @Transactional(
        propagation = Propagation.REQUIRES_NEW
    )
    public void markAsPublished(
        List<UUID> eventIds
    ) {
        if (
            eventIds == null ||
            eventIds.isEmpty()
        ) {
            return;
        }

        String placeholders =
            String.join(
                ", ",
                Collections.nCopies(
                    eventIds.size(),
                    "?"
                )
            );

        Object[] parameters =
            eventIds.toArray();

        String updateSql =
            """
            UPDATE outbox_events
            SET
                status = 'PUBLISHED',
                published_at = NOW(),
                last_error = NULL
            WHERE status = 'PROCESSING'
              AND id IN (%s)
            """.formatted(
                placeholders
            );

        int updated =
            jdbcTemplate.update(
                updateSql,
                parameters
            );

        if (
            updated ==
            eventIds.size()
        ) {
            return;
        }

        String verificationSql =
            """
            SELECT COUNT(*)
            FROM outbox_events
            WHERE status = 'PUBLISHED'
              AND id IN (%s)
            """.formatted(
                placeholders
            );

        Long publishedCount =
            jdbcTemplate.queryForObject(
                verificationSql,
                Long.class,
                parameters
            );

        if (
            publishedCount != null &&
            publishedCount ==
                eventIds.size()
        ) {
            return;
        }

        throw new IllegalStateException(
            "Could not mark complete outbox " +
            "batch as PUBLISHED. expected=" +
            eventIds.size() +
            ", updated=" +
            updated +
            ", published=" +
            publishedCount
        );
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
            getRequiredEventInsideTransaction(
                eventId
            );

        String errorMessage =
            buildErrorMessage(cause);

        int nextRetryCount =
            event.getRetryCount() + 1;

        if (nextRetryCount >= maxAttempts) {

            event.markAsFailed(
                errorMessage
            );

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

        outboxEventRepository.saveAndFlush(
            event
        );
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

    private OutboxEvent
    getRequiredEventInsideTransaction(
        UUID eventId
    ) {
        return outboxEventRepository
            .findById(eventId)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Outbox event not found: " +
                        eventId
                    )
            );
    }

    private String buildErrorMessage(
        Throwable cause
    ) {
        Throwable rootCause = cause;

        while (
            rootCause.getCause() != null
        ) {
            rootCause =
                rootCause.getCause();
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
