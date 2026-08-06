package com.hlrms.requestservice.repository;

import com.hlrms.requestservice.entity.OutboxEvent;
import com.hlrms.requestservice.entity.OutboxEventStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OutboxEventRepositoryTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private OutboxEvent createEvent(
        UUID aggregateId,
        OutboxEventStatus status,
        Instant createdAt
    ) {
        return OutboxEvent.builder()
            .id(UUID.randomUUID())
            .aggregateId(aggregateId)
            .aggregateType("REQUEST")
            .eventType("REQUEST_CREATED")
            .payload("""
                {
                  "requestType": "TEST",
                  "message": "Outbox repository test"
                }
                """)
            .status(status)
            .retryCount(0)
            .createdAt(createdAt)
            .build();
    }

    @Test
    void shouldSaveOutboxEventWithExpectedValues() {

        UUID aggregateId = UUID.randomUUID();

        OutboxEvent savedEvent =
            outboxEventRepository.saveAndFlush(
                createEvent(
                    aggregateId,
                    OutboxEventStatus.PENDING,
                    Instant.now()
                )
            );

        assertThat(savedEvent.getId())
            .isNotNull();

        assertThat(savedEvent.getAggregateId())
            .isEqualTo(aggregateId);

        assertThat(savedEvent.getAggregateType())
            .isEqualTo("REQUEST");

        assertThat(savedEvent.getEventType())
            .isEqualTo("REQUEST_CREATED");

        assertThat(savedEvent.getStatus())
            .isEqualTo(OutboxEventStatus.PENDING);

        assertThat(savedEvent.getRetryCount())
            .isZero();

        assertThat(savedEvent.getCreatedAt())
            .isNotNull();
    }

    @Test
    void shouldReturnPendingEventsOrderedByOldestFirst() {

        Instant now = Instant.now();

        OutboxEvent oldestEvent =
            outboxEventRepository.save(
                createEvent(
                    UUID.randomUUID(),
                    OutboxEventStatus.PENDING,
                    now.minusSeconds(30)
                )
            );

        OutboxEvent newestEvent =
            outboxEventRepository.save(
                createEvent(
                    UUID.randomUUID(),
                    OutboxEventStatus.PENDING,
                    now.minusSeconds(10)
                )
            );

        outboxEventRepository.save(
            createEvent(
                UUID.randomUUID(),
                OutboxEventStatus.PUBLISHED,
                now.minusSeconds(60)
            )
        );

        outboxEventRepository.flush();

        List<OutboxEvent> events =
            outboxEventRepository.findBatchForUpdate(
                OutboxEventStatus.PENDING,
                PageRequest.of(0, 10)
            );

        assertThat(events)
            .extracting(OutboxEvent::getId)
            .containsExactly(
                oldestEvent.getId(),
                newestEvent.getId()
            );
    }

    @Test
    void shouldRespectRequestedBatchSize() {

        Instant now = Instant.now();

        for (int index = 0; index < 5; index++) {
            outboxEventRepository.save(
                createEvent(
                    UUID.randomUUID(),
                    OutboxEventStatus.PENDING,
                    now.plusSeconds(index)
                )
            );
        }

        outboxEventRepository.flush();

        List<OutboxEvent> events =
            outboxEventRepository.findBatchForUpdate(
                OutboxEventStatus.PENDING,
                PageRequest.of(0, 2)
            );

        assertThat(events)
            .hasSize(2);
    }

    @Test
    void shouldNotReturnEventsWithDifferentStatus() {

        outboxEventRepository.save(
            createEvent(
                UUID.randomUUID(),
                OutboxEventStatus.PUBLISHED,
                Instant.now()
            )
        );

        outboxEventRepository.save(
            createEvent(
                UUID.randomUUID(),
                OutboxEventStatus.FAILED,
                Instant.now()
            )
        );

        outboxEventRepository.flush();

        List<OutboxEvent> events =
            outboxEventRepository.findBatchForUpdate(
                OutboxEventStatus.PENDING,
                PageRequest.of(0, 10)
            );

        assertThat(events)
            .isEmpty();
    }

    @Test
    void shouldResetProcessingEventsToPending() {

        OutboxEvent processingEventOne =
            outboxEventRepository.save(
                createEvent(
                    UUID.randomUUID(),
                    OutboxEventStatus.PROCESSING,
                    Instant.now()
                )
            );

        OutboxEvent processingEventTwo =
            outboxEventRepository.save(
                createEvent(
                    UUID.randomUUID(),
                    OutboxEventStatus.PROCESSING,
                    Instant.now()
                )
            );

        OutboxEvent publishedEvent =
            outboxEventRepository.save(
                createEvent(
                    UUID.randomUUID(),
                    OutboxEventStatus.PUBLISHED,
                    Instant.now()
                )
            );

        outboxEventRepository.flush();

        int updatedRows =
            outboxEventRepository
                .resetProcessingEventsToPending();

        assertThat(updatedRows)
            .isEqualTo(2);

        OutboxEvent refreshedOne =
            outboxEventRepository
                .findById(processingEventOne.getId())
                .orElseThrow();

        OutboxEvent refreshedTwo =
            outboxEventRepository
                .findById(processingEventTwo.getId())
                .orElseThrow();

        OutboxEvent refreshedPublished =
            outboxEventRepository
                .findById(publishedEvent.getId())
                .orElseThrow();

        assertThat(refreshedOne.getStatus())
            .isEqualTo(OutboxEventStatus.PENDING);

        assertThat(refreshedTwo.getStatus())
            .isEqualTo(OutboxEventStatus.PENDING);

        assertThat(refreshedPublished.getStatus())
            .isEqualTo(OutboxEventStatus.PUBLISHED);
    }
}