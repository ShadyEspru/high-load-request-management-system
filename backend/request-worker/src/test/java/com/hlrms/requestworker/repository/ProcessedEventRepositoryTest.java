package com.hlrms.requestworker.repository;

import com.hlrms.requestworker.entity.ProcessedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@Transactional
class ProcessedEventRepositoryTest {

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID createRequest() {
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Timestamp now =
            Timestamp.from(Instant.now());

        jdbcTemplate.update(
            """
            INSERT INTO requests
            (
                id,
                user_id,
                request_type,
                payload,
                status,
                created_at,
                updated_at,
                version,
                idempotency_key,
                idempotency_fingerprint
            )
            VALUES
            (
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?
            )
            """,
            requestId,
            userId,
            "TEST",
            "{\"message\":\"processed event repository test\"}",
            "PENDING",
            now,
            now,
            0L,
            "processed-event-test-" + UUID.randomUUID(),
            "a".repeat(64)
        );

        return requestId;
    }

    @Test
    void shouldRegisterNewEvent() {

        UUID eventId = UUID.randomUUID();
        UUID requestId = createRequest();
        Instant occurredAt = Instant.now();

        int insertedRows =
            processedEventRepository.tryRegisterEvent(
                eventId,
                requestId,
                "REQUEST_CREATED",
                1,
                occurredAt
            );

        assertThat(insertedRows)
            .isEqualTo(1);

        ProcessedEvent savedEvent =
            processedEventRepository
                .findById(eventId)
                .orElseThrow();

        assertThat(savedEvent.getEventId())
            .isEqualTo(eventId);

        assertThat(savedEvent.getRequestId())
            .isEqualTo(requestId);

        assertThat(savedEvent.getEventType())
            .isEqualTo("REQUEST_CREATED");

        assertThat(savedEvent.getEventVersion())
            .isEqualTo(1);

        assertThat(savedEvent.getOccurredAt())
            .isNotNull();

        assertThat(savedEvent.getProcessedAt())
            .isNull();
    }

    @Test
    void shouldRejectDuplicateEventId() {

        UUID eventId = UUID.randomUUID();
        UUID requestId = createRequest();

        long countBefore =
            processedEventRepository.count();

        int firstInsert =
            processedEventRepository.tryRegisterEvent(
                eventId,
                requestId,
                "REQUEST_CREATED",
                1,
                Instant.now()
            );

        int duplicateInsert =
            processedEventRepository.tryRegisterEvent(
                eventId,
                requestId,
                "REQUEST_CREATED",
                1,
                Instant.now()
            );

        long countAfter =
            processedEventRepository.count();

        assertThat(firstInsert)
            .isEqualTo(1);

        assertThat(duplicateInsert)
            .isZero();

        assertThat(countAfter)
            .isEqualTo(countBefore + 1);

        assertThat(
            processedEventRepository.findById(eventId)
        )
        .isPresent();
    }

    @Test
    void shouldMarkRegisteredEventAsProcessed() {

        UUID eventId = UUID.randomUUID();
        UUID requestId = createRequest();

        processedEventRepository.tryRegisterEvent(
            eventId,
            requestId,
            "REQUEST_CREATED",
            1,
            Instant.now()
        );

        int updatedRows =
            processedEventRepository
                .markEventAsProcessed(eventId);

        assertThat(updatedRows)
            .isEqualTo(1);

        ProcessedEvent processedEvent =
            processedEventRepository
                .findById(eventId)
                .orElseThrow();

        assertThat(processedEvent.getProcessedAt())
            .isNotNull();
    }

    @Test
    void shouldNotMarkAlreadyProcessedEventAgain() {

        UUID eventId = UUID.randomUUID();
        UUID requestId = createRequest();

        processedEventRepository.tryRegisterEvent(
            eventId,
            requestId,
            "REQUEST_CREATED",
            1,
            Instant.now()
        );

        int firstUpdate =
            processedEventRepository
                .markEventAsProcessed(eventId);

        int secondUpdate =
            processedEventRepository
                .markEventAsProcessed(eventId);

        assertThat(firstUpdate)
            .isEqualTo(1);

        assertThat(secondUpdate)
            .isZero();
    }

    @Test
    void shouldNotMarkUnknownEventAsProcessed() {

        int updatedRows =
            processedEventRepository
                .markEventAsProcessed(
                    UUID.randomUUID()
                );

        assertThat(updatedRows)
            .isZero();
    }
}