package com.hlrms.requestservice.service;

import com.hlrms.requestservice.dto.CreateRequestDto;
import com.hlrms.requestservice.dto.CreateRequestResult;
import com.hlrms.requestservice.entity.RequestEntity;
import com.hlrms.requestservice.metrics.RequestMetrics;
import com.hlrms.requestservice.repository.RequestRepository;
import com.hlrms.requestservice.security.CurrentUserProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class RequestServiceImplIdempotencyTest {


    @Mock
    private RequestRepository requestRepository;

    @Mock
    private RequestCreationTransactionService
            requestCreationTransactionService;

    @Mock
    private RedisIdempotencyService
            redisIdempotencyService;

    @Mock
    private RedisDistributedLockService
            redisDistributedLockService;

    @Mock
    private RequestCacheService requestCacheService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private RequestMetrics requestMetrics;


    private RequestServiceImpl service;


    private UUID userId;


    @BeforeEach
    void setup() {

        service =
            new RequestServiceImpl(
                requestRepository,
                requestCreationTransactionService,
                redisIdempotencyService,
                redisDistributedLockService,
                requestCacheService,
                currentUserProvider,
                requestMetrics
            );


        userId = UUID.randomUUID();

        when(currentUserProvider.getUserId())
                .thenReturn(userId);
    }



    @Test
    void shouldCreateRequestWhenNoReplayExists() {


        CreateRequestDto dto =
            new CreateRequestDto(
                "TEST",
                "{\"message\":\"hello\"}"
            );


        String key =
            "idem-test-1";


        when(redisIdempotencyService.find(
                anyString()
        ))
        .thenReturn(Optional.empty());

        RequestEntity savedRequest =
            RequestEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .requestType("TEST")
                .payload("{\"message\":\"hello\"}")
                .build();


        when(
            requestCreationTransactionService
                .createRequestWithOutboxEvent(
                    any(),
                    anyString(),
                    anyString(),
                    anyString(),
                    anyString()
                )
        )
        .thenReturn(savedRequest);



        CreateRequestResult result =
            service.createRequest(
                dto,
                key
            );


        assertThat(result)
            .isNotNull();


        assertThat(result.replayed())
            .isFalse();



        verify(
            requestCreationTransactionService
        )
        .createRequestWithOutboxEvent(
            eq(userId),
            eq(key),
            anyString(),
            eq("TEST"),
            eq("{\"message\":\"hello\"}")
        );


        verify(redisIdempotencyService)
            .save(
                anyString(),
                anyString(),
                eq(savedRequest.getId())
            );


        verifyNoInteractions(
            redisDistributedLockService
        );
    }
}
