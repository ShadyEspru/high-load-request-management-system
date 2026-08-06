package com.hlrms.requestservice.service;

import com.hlrms.requestservice.dto.RequestResponseDto;
import com.hlrms.requestservice.entity.RequestStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "hlrms.redis.request-cache-key-prefix=hlrms:test:cache:request:",
        "hlrms.redis.request-cache-ttl=2s"
    }
)
@ActiveProfiles("test")
class RequestCacheServiceIntegrationTest {

    private static final String TEST_PREFIX =
        "hlrms:test:cache:request:";

    @Autowired
    private RequestCacheService requestCacheService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @AfterEach
    void cleanRedis() {
        Set<String> keys =
            stringRedisTemplate.keys(
                TEST_PREFIX + "*"
            );

        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    @Test
    void shouldReturnEmptyForCacheMiss() {

        Optional<RequestResponseDto> result =
            requestCacheService.find(
                UUID.randomUUID()
            );

        assertThat(result)
            .isEmpty();
    }

    @Test
    void shouldSaveAndReadCompletedRequest() {

        RequestResponseDto request =
            createRequest(
                RequestStatus.COMPLETED
            );

        requestCacheService.saveIfTerminal(
            request
        );

        Optional<RequestResponseDto> cached =
            requestCacheService.find(
                request.id()
            );

        assertThat(cached)
            .isPresent();

        assertThat(cached.orElseThrow())
            .usingRecursiveComparison()
            .isEqualTo(request);
    }

    @Test
    void shouldSaveAndReadFailedRequest() {

        RequestResponseDto request =
            createRequest(
                RequestStatus.FAILED
            );

        requestCacheService.saveIfTerminal(
            request
        );

        RequestResponseDto cached =
            requestCacheService
                .find(request.id())
                .orElseThrow();

        assertThat(cached.id())
            .isEqualTo(request.id());

        assertThat(cached.status())
            .isEqualTo(RequestStatus.FAILED);

        assertThat(cached.errorMessage())
            .isEqualTo(request.errorMessage());
    }

    @Test
    void shouldNotCachePendingRequest() {

        RequestResponseDto request =
            createRequest(
                RequestStatus.PENDING
            );

        requestCacheService.saveIfTerminal(
            request
        );

        assertThat(
            requestCacheService.find(
                request.id()
            )
        )
        .isEmpty();

        assertThat(
            stringRedisTemplate.hasKey(
                redisKey(request.id())
            )
        )
        .isFalse();
    }

    @Test
    void shouldNotCacheProcessingRequest() {

        RequestResponseDto request =
            createRequest(
                RequestStatus.PROCESSING
            );

        requestCacheService.saveIfTerminal(
            request
        );

        assertThat(
            requestCacheService.find(
                request.id()
            )
        )
        .isEmpty();
    }

    @Test
    void shouldDeleteCachedRequest() {

        RequestResponseDto request =
            createRequest(
                RequestStatus.COMPLETED
            );

        requestCacheService.saveIfTerminal(
            request
        );

        assertThat(
            requestCacheService.find(
                request.id()
            )
        )
        .isPresent();

        requestCacheService.delete(
            request.id()
        );

        assertThat(
            requestCacheService.find(
                request.id()
            )
        )
        .isEmpty();
    }

    @Test
    void shouldDeleteMalformedCachedValue() {

        UUID requestId =
            UUID.randomUUID();

        stringRedisTemplate
            .opsForValue()
            .set(
                redisKey(requestId),
                "{this-is-invalid-json"
            );

        Optional<RequestResponseDto> result =
            requestCacheService.find(
                requestId
            );

        assertThat(result)
            .isEmpty();

        assertThat(
            stringRedisTemplate.hasKey(
                redisKey(requestId)
            )
        )
        .isFalse();
    }

    @Test
    void shouldApplyCacheTtl() {

        RequestResponseDto request =
            createRequest(
                RequestStatus.COMPLETED
            );

        requestCacheService.saveIfTerminal(
            request
        );

        Long ttl =
            stringRedisTemplate.getExpire(
                redisKey(request.id())
            );

        assertThat(ttl)
            .isNotNull()
            .isPositive()
            .isLessThanOrEqualTo(2);
    }

    @Test
    void shouldExpireCachedRequest()
        throws InterruptedException {

        RequestResponseDto request =
            createRequest(
                RequestStatus.COMPLETED
            );

        requestCacheService.saveIfTerminal(
            request
        );

        assertThat(
            requestCacheService.find(
                request.id()
            )
        )
        .isPresent();

        Thread.sleep(
            Duration.ofMillis(2_300).toMillis()
        );

        assertThat(
            requestCacheService.find(
                request.id()
            )
        )
        .isEmpty();
    }

    private RequestResponseDto createRequest(
        RequestStatus status
    ) {
        Instant now =
            Instant.now();

        boolean completed =
            status == RequestStatus.COMPLETED;

        boolean failed =
            status == RequestStatus.FAILED;

        return new RequestResponseDto(
            UUID.randomUUID(),
            "cache-test-" + UUID.randomUUID(),
            "CACHE_TEST",
            """
            {
              "message": "Redis cache integration test"
            }
            """,
            status,
            completed
                ? "Request completed successfully"
                : null,
            failed
                ? "Request processing failed"
                : null,
            now.minusSeconds(5),
            now,
            completed || failed
                ? now
                : null,
            0L
        );
    }

    private String redisKey(
        UUID requestId
    ) {
        return TEST_PREFIX + requestId;
    }
}