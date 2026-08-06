package com.hlrms.requestservice.service;

import com.hlrms.requestservice.config.RedisProperties;
import com.hlrms.requestservice.service.RedisIdempotencyService.IdempotencyRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "hlrms.redis.idempotency-key-prefix=hlrms:test:idempotency:",
        "hlrms.redis.lock-key-prefix=hlrms:test:lock:",
        "hlrms.redis.idempotency-ttl=2s"
    }
)
@ActiveProfiles("test")
class RedisIdempotencyServiceIntegrationTest {

    private static final String TEST_PREFIX =
        "hlrms:test:";

    @Autowired
    private RedisIdempotencyService
        redisIdempotencyService;

    @Autowired
    private RedisProperties redisProperties;

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
    void shouldSaveAndReadIdempotencyRecord() {

        String idempotencyKey =
            "create-request-" + UUID.randomUUID();

        String fingerprint =
            "a".repeat(64);

        UUID requestId =
            UUID.randomUUID();

        redisIdempotencyService.save(
            idempotencyKey,
            fingerprint,
            requestId
        );

        Optional<IdempotencyRecord> result =
            redisIdempotencyService.find(
                idempotencyKey
            );

        assertThat(result)
            .isPresent();

        assertThat(
            result.orElseThrow().fingerprint()
        )
        .isEqualTo(fingerprint);

        assertThat(
            result.orElseThrow().requestId()
        )
        .isEqualTo(requestId);
    }

    @Test
    void shouldReturnEmptyForUnknownKey() {

        Optional<IdempotencyRecord> result =
            redisIdempotencyService.find(
                "unknown-" + UUID.randomUUID()
            );

        assertThat(result)
            .isEmpty();
    }

    @Test
    void shouldDeleteIdempotencyRecord() {

        String idempotencyKey =
            "delete-test-" + UUID.randomUUID();

        redisIdempotencyService.save(
            idempotencyKey,
            "b".repeat(64),
            UUID.randomUUID()
        );

        assertThat(
            redisIdempotencyService.find(
                idempotencyKey
            )
        )
        .isPresent();

        redisIdempotencyService.delete(
            idempotencyKey
        );

        assertThat(
            redisIdempotencyService.find(
                idempotencyKey
            )
        )
        .isEmpty();
    }

    @Test
    void shouldStoreValueUsingHashedRedisKey() {

        String idempotencyKey =
            "sensitive-key-" + UUID.randomUUID();

        redisIdempotencyService.save(
            idempotencyKey,
            "c".repeat(64),
            UUID.randomUUID()
        );

        String expectedRedisKey =
            redisProperties
                .getIdempotencyKeyPrefix()
                + sha256(idempotencyKey);

        assertThat(
            stringRedisTemplate.hasKey(
                expectedRedisKey
            )
        )
        .isTrue();

        assertThat(expectedRedisKey)
            .doesNotContain(idempotencyKey);

        assertThat(
            stringRedisTemplate.hasKey(
                redisProperties
                    .getIdempotencyKeyPrefix()
                    + idempotencyKey
            )
        )
        .isFalse();
    }

    @Test
    void shouldBuildHashedLockKey() {

        String idempotencyKey =
            "lock-source-" + UUID.randomUUID();

        String lockKey =
            redisIdempotencyService
                .buildLockKey(idempotencyKey);

        assertThat(lockKey)
            .startsWith(
                redisProperties.getLockKeyPrefix()
            )
            .endsWith(sha256(idempotencyKey))
            .doesNotContain(idempotencyKey);
    }

    @Test
    void shouldApplyConfiguredTtl() {

        String idempotencyKey =
            "ttl-test-" + UUID.randomUUID();

        redisIdempotencyService.save(
            idempotencyKey,
            "d".repeat(64),
            UUID.randomUUID()
        );

        String redisKey =
            redisProperties
                .getIdempotencyKeyPrefix()
                + sha256(idempotencyKey);

        Long ttl =
            stringRedisTemplate.getExpire(
                redisKey
            );

        assertThat(ttl)
            .isNotNull()
            .isPositive()
            .isLessThanOrEqualTo(2);
    }

    @Test
    void shouldExpireRecordAfterTtl()
        throws InterruptedException {

        String idempotencyKey =
            "expiry-test-" + UUID.randomUUID();

        redisIdempotencyService.save(
            idempotencyKey,
            "e".repeat(64),
            UUID.randomUUID()
        );

        assertThat(
            redisIdempotencyService.find(
                idempotencyKey
            )
        )
        .isPresent();

        Thread.sleep(
            Duration.ofMillis(2_300).toMillis()
        );

        assertThat(
            redisIdempotencyService.find(
                idempotencyKey
            )
        )
        .isEmpty();
    }

    private String sha256(
        String value
    ) {
        try {
            MessageDigest digest =
                MessageDigest.getInstance(
                    "SHA-256"
                );

            return HexFormat
                .of()
                .formatHex(
                    digest.digest(
                        value.getBytes(
                            StandardCharsets.UTF_8
                        )
                    )
                );

        } catch (Exception exception) {
            throw new IllegalStateException(
                exception
            );
        }
    }
}