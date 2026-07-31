package com.hlrms.requestservice.service;

import com.hlrms.requestservice.entity.OutboxEvent;
import com.hlrms.requestservice.entity.RequestEntity;
import com.hlrms.requestservice.entity.RequestStatus;
import com.hlrms.requestservice.event.RequestCreatedEvent;
import com.hlrms.requestservice.repository.OutboxEventRepository;
import com.hlrms.requestservice.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RequestCreationTransactionService {

    private static final String REQUEST_AGGREGATE_TYPE =
        "REQUEST";

    private final RequestRepository requestRepository;

    private final OutboxEventRepository outboxEventRepository;

    private final JsonMapper jsonMapper;

    @Transactional
    public RequestEntity createRequestWithOutboxEvent(
        UUID userId,
        String idempotencyKey,
        String idempotencyFingerprint,
        String requestType,
        String payload
    ) {
        RequestEntity requestEntity =
            RequestEntity.builder()
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .idempotencyFingerprint(
                    idempotencyFingerprint
                )
                .requestType(requestType)
                .payload(payload)
                .status(RequestStatus.PENDING)
                .build();

        RequestEntity savedRequest =
            requestRepository.saveAndFlush(requestEntity);

        RequestCreatedEvent requestCreatedEvent =
            RequestCreatedEvent.of(savedRequest.getId());

        String eventPayload =
            serializeEvent(requestCreatedEvent);

        OutboxEvent outboxEvent =
            OutboxEvent.builder()
                .id(requestCreatedEvent.eventId())
                .aggregateId(savedRequest.getId())
                .aggregateType(REQUEST_AGGREGATE_TYPE)
                .eventType(
                    requestCreatedEvent.eventType()
                )
                .payload(eventPayload)
                .build();

        outboxEventRepository.saveAndFlush(outboxEvent);

        return savedRequest;
    }

    private String serializeEvent(
        RequestCreatedEvent event
    ) {
        try {
            return jsonMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                "Could not serialize RequestCreatedEvent",
                exception
            );
        }
    }
}