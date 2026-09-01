package com.hlrms.requestservice.service;

import com.hlrms.requestservice.entity.OutboxEvent;
import com.hlrms.requestservice.entity.RequestEntity;
import com.hlrms.requestservice.entity.RequestStatus;
import com.hlrms.requestservice.repository.RequestRepository;
import jakarta.persistence.EntityManager;
import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestCreationTransactionServiceTest {


    @Mock
    private RequestRepository requestRepository;


    @Mock
    private EntityManager entityManager;


    private final JsonMapper jsonMapper =
            JsonMapper.builder().build();


    private RequestCreationTransactionService service;


    @Test
    void shouldCreateRequestAndOutboxEvent() {

        service = new RequestCreationTransactionService(
                requestRepository,
                entityManager,
                jsonMapper
        );


        UUID userId = UUID.randomUUID();


        RequestEntity savedRequest =
                RequestEntity.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .requestType("TEST")
                        .payload("{\"hello\":\"world\"}")
                        .status(RequestStatus.PENDING)
                        .build();


        when(requestRepository.save(any()))
                .thenReturn(savedRequest);


        RequestEntity result =
                service.createRequestWithOutboxEvent(
                        userId,
                        "key-123",
                        "fingerprint-123",
                        "TEST",
                        "{\"hello\":\"world\"}"
                );


        assertThat(result)
                .isNotNull();

        assertThat(result.getStatus())
                .isEqualTo(RequestStatus.PENDING);


        verify(requestRepository)
                .save(any(RequestEntity.class));


        ArgumentCaptor<OutboxEvent> captor =
                ArgumentCaptor.forClass(OutboxEvent.class);


        verify(entityManager)
                .persist(captor.capture());


        OutboxEvent event =
                captor.getValue();


        assertThat(event.getAggregateType())
                .isEqualTo("REQUEST");


        assertThat(event.getAggregateId())
                .isEqualTo(savedRequest.getId());


        assertThat(event.getPayload())
                .contains(savedRequest.getId().toString());
    }
}
