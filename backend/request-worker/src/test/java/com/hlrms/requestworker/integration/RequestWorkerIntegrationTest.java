package com.hlrms.requestworker.integration;

import com.hlrms.requestworker.config.RabbitMqConstants;
import com.hlrms.requestworker.entity.ProcessedEvent;
import com.hlrms.requestworker.entity.RequestEntity;
import com.hlrms.requestworker.entity.RequestStatus;
import com.hlrms.requestworker.event.RequestCreatedEvent;
import com.hlrms.requestworker.repository.ProcessedEventRepository;
import com.hlrms.requestworker.repository.RequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
class RequestWorkerIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private ProcessedEventRepository
        processedEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanRabbitQueue() {
        rabbitTemplate.execute(channel -> {
            channel.queuePurge(
                RabbitMqConstants.REQUEST_QUEUE
            );

            return null;
        });
    }

    private UUID createPendingRequest() {
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
            "INTEGRATION_TEST",
            """
            {
              "message":
              "RabbitMQ worker integration test"
            }
            """,
            "PENDING",
            now,
            now,
            0L,
            "worker-integration-" +
                UUID.randomUUID(),
            "b".repeat(64)
        );

        return requestId;
    }

    @Test
    void shouldConsumeMessageAndCompleteRequest() {

        UUID requestId =
            createPendingRequest();

        UUID eventId =
            UUID.randomUUID();

        RequestCreatedEvent event =
            new RequestCreatedEvent(
                eventId,
                requestId,
                "REQUEST_CREATED",
                1,
                Instant.now()
            );

        rabbitTemplate.convertAndSend(
            RabbitMqConstants.REQUEST_EXCHANGE,
            RabbitMqConstants.REQUEST_ROUTING_KEY,
            event
        );

        await()
            .atMost(Duration.ofSeconds(15))
            .pollInterval(
                Duration.ofMillis(250)
            )
            .untilAsserted(() -> {

                RequestEntity request =
                    requestRepository
                        .findById(requestId)
                        .orElseThrow();

                assertThat(request.getStatus())
                    .isEqualTo(
                        RequestStatus.COMPLETED
                    );

                assertThat(request.getResult())
                    .isEqualTo(
                        "Request processed successfully " +
                        "by request-worker"
                    );

                assertThat(request.getCompletedAt())
                    .isNotNull();
            });

        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {

                ProcessedEvent processedEvent =
                    processedEventRepository
                        .findById(eventId)
                        .orElseThrow();

                assertThat(
                    processedEvent.getRequestId()
                )
                .isEqualTo(requestId);

                assertThat(
                    processedEvent.getProcessedAt()
                )
                .isNotNull();
            });
    }

    @Test
    void shouldIgnoreDuplicateEventDelivery() {

        UUID requestId =
            createPendingRequest();

        UUID eventId =
            UUID.randomUUID();

        RequestCreatedEvent event =
            new RequestCreatedEvent(
                eventId,
                requestId,
                "REQUEST_CREATED",
                1,
                Instant.now()
            );

        rabbitTemplate.convertAndSend(
            RabbitMqConstants.REQUEST_EXCHANGE,
            RabbitMqConstants.REQUEST_ROUTING_KEY,
            event
        );

        await()
            .atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> {

                RequestEntity request =
                    requestRepository
                        .findById(requestId)
                        .orElseThrow();

                assertThat(request.getStatus())
                    .isEqualTo(
                        RequestStatus.COMPLETED
                    );
            });

        long processedBefore =
            processedEventRepository.count();

        rabbitTemplate.convertAndSend(
            RabbitMqConstants.REQUEST_EXCHANGE,
            RabbitMqConstants.REQUEST_ROUTING_KEY,
            event
        );

        await()
            .during(Duration.ofSeconds(2))
            .atMost(Duration.ofSeconds(4))
            .untilAsserted(() -> {

                long processedAfter =
                    processedEventRepository.count();

                assertThat(processedAfter)
                    .isEqualTo(processedBefore);

                ProcessedEvent processedEvent =
                    processedEventRepository
                        .findById(eventId)
                        .orElseThrow();

                assertThat(
                    processedEvent.getProcessedAt()
                )
                .isNotNull();
            });
    }
}