package com.hlrms.requestservice.service;

import com.hlrms.requestservice.service.RedisDistributedLockService.LockAttempt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "hlrms.redis.lock-key-prefix=hlrms:test:lock:",
        "hlrms.redis.lock-ttl=1s",
        "hlrms.redis.lock-wait-timeout=500ms",
        "hlrms.redis.lock-retry-interval=25ms"
    }
)
@ActiveProfiles("test")
class RedisDistributedLockServiceIntegrationTest {

    private static final String TEST_PREFIX =
        "hlrms:test:lock:";

    @Autowired
    private RedisDistributedLockService
        lockService;

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
    void shouldAcquireAvailableLock() {

        String lockKey =
            TEST_PREFIX + UUID.randomUUID();

        LockAttempt attempt =
            lockService.tryAcquire(lockKey);

        assertThat(attempt.acquired())
            .isTrue();

        assertThat(attempt.redisAvailable())
            .isTrue();

        assertThat(attempt.lockKey())
            .isEqualTo(lockKey);

        assertThat(attempt.token())
            .isNotBlank();

        assertThat(
            stringRedisTemplate.hasKey(lockKey)
        )
        .isTrue();
    }

    @Test
    void shouldRejectSecondLockForSameKey() {

        String lockKey =
            TEST_PREFIX + UUID.randomUUID();

        LockAttempt first =
            lockService.tryAcquire(lockKey);

        LockAttempt second =
            lockService.tryAcquire(lockKey);

        assertThat(first.acquired())
            .isTrue();

        assertThat(second.acquired())
            .isFalse();

        assertThat(second.redisAvailable())
            .isTrue();

        assertThat(second.token())
            .isNull();
    }

    @Test
    void shouldReleaseOwnedLock() {

        String lockKey =
            TEST_PREFIX + UUID.randomUUID();

        LockAttempt attempt =
            lockService.tryAcquire(lockKey);

        lockService.release(attempt);

        assertThat(
            stringRedisTemplate.hasKey(lockKey)
        )
        .isFalse();

        LockAttempt nextAttempt =
            lockService.tryAcquire(lockKey);

        assertThat(nextAttempt.acquired())
            .isTrue();
    }

    @Test
    void shouldNotReleaseLockUsingWrongToken() {

        String lockKey =
            TEST_PREFIX + UUID.randomUUID();

        LockAttempt ownerAttempt =
            lockService.tryAcquire(lockKey);

        LockAttempt wrongAttempt =
            new LockAttempt(
                lockKey,
                UUID.randomUUID().toString(),
                true,
                true
            );

        lockService.release(wrongAttempt);

        assertThat(
            stringRedisTemplate.hasKey(lockKey)
        )
        .isTrue();

        assertThat(
            stringRedisTemplate
                .opsForValue()
                .get(lockKey)
        )
        .isEqualTo(ownerAttempt.token());

        lockService.release(ownerAttempt);
    }

    @Test
    void shouldIgnoreNullOrUnacquiredRelease() {

        String lockKey =
            TEST_PREFIX + UUID.randomUUID();

        lockService.release(null);

        lockService.release(
            LockAttempt.contended(lockKey)
        );

        assertThat(
            stringRedisTemplate.hasKey(lockKey)
        )
        .isFalse();
    }

    @Test
    void shouldExpireLockAutomatically()
        throws InterruptedException {

        String lockKey =
            TEST_PREFIX + UUID.randomUUID();

        LockAttempt attempt =
            lockService.tryAcquire(lockKey);

        assertThat(attempt.acquired())
            .isTrue();

        Thread.sleep(
            Duration.ofMillis(1_250).toMillis()
        );

        assertThat(
            stringRedisTemplate.hasKey(lockKey)
        )
        .isFalse();

        LockAttempt secondAttempt =
            lockService.tryAcquire(lockKey);

        assertThat(secondAttempt.acquired())
            .isTrue();
    }

    @Test
    void shouldWaitUntilLockIsReleased() {

        String lockKey =
            TEST_PREFIX + UUID.randomUUID();

        LockAttempt attempt =
            lockService.tryAcquire(lockKey);

        CompletableFuture<Void> releaseFuture =
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(150);
                    lockService.release(attempt);

                } catch (
                    InterruptedException exception
                ) {
                    Thread.currentThread()
                        .interrupt();

                    throw new IllegalStateException(
                        exception
                    );
                }
            });

        boolean unlocked =
            lockService.waitUntilUnlocked(
                lockKey
            );

        releaseFuture.join();

        assertThat(unlocked)
            .isTrue();

        assertThat(
            stringRedisTemplate.hasKey(lockKey)
        )
        .isFalse();
    }

    @Test
    void shouldReturnFalseWhenWaitTimesOut() {

        String lockKey =
            TEST_PREFIX + UUID.randomUUID();

        LockAttempt attempt =
            lockService.tryAcquire(lockKey);

        boolean unlocked =
            lockService.waitUntilUnlocked(
                lockKey
            );

        assertThat(unlocked)
            .isFalse();

        assertThat(
            stringRedisTemplate.hasKey(lockKey)
        )
        .isTrue();

        lockService.release(attempt);
    }
}