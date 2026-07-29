package com.hlrms.requestservice.service;

import com.hlrms.requestservice.config.RedisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisDistributedLockService {

    private static final DefaultRedisScript<Long>
        RELEASE_LOCK_SCRIPT =
        new DefaultRedisScript<>(
            """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            else
                return 0
            end
            """,
            Long.class
        );

    private final StringRedisTemplate
        stringRedisTemplate;

    private final RedisProperties redisProperties;

    public LockAttempt tryAcquire(
        String lockKey
    ) {
        String lockToken =
            UUID.randomUUID().toString();

        try {
            Boolean acquired =
                stringRedisTemplate
                    .opsForValue()
                    .setIfAbsent(
                        lockKey,
                        lockToken,
                        redisProperties.getLockTtl()
                    );

            if (Boolean.TRUE.equals(acquired)) {
                return LockAttempt.acquired(
                    lockKey,
                    lockToken
                );
            }

            return LockAttempt.contended(lockKey);

        } catch (DataAccessException exception) {
            log.warn(
                "Redis lock is unavailable. " +
                "Falling back to PostgreSQL. " +
                "lockKey={}, reason={}",
                lockKey,
                exception.getMessage()
            );

            return LockAttempt.redisUnavailable(
                lockKey
            );
        }
    }

    public void release(
        LockAttempt lockAttempt
    ) {
        if (
            lockAttempt == null
                || !lockAttempt.acquired()
                || lockAttempt.token() == null
        ) {
            return;
        }

        try {
            stringRedisTemplate.execute(
                RELEASE_LOCK_SCRIPT,
                Collections.singletonList(
                    lockAttempt.lockKey()
                ),
                lockAttempt.token()
            );

        } catch (DataAccessException exception) {
            log.warn(
                "Could not release Redis lock. " +
                "The lock will expire automatically. " +
                "lockKey={}, reason={}",
                lockAttempt.lockKey(),
                exception.getMessage()
            );
        }
    }

    public boolean waitUntilUnlocked(
        String lockKey
    ) {
        Duration waitTimeout =
            redisProperties.getLockWaitTimeout();

        Duration retryInterval =
            redisProperties.getLockRetryInterval();

        long deadline =
            System.nanoTime()
                + waitTimeout.toNanos();

        while (System.nanoTime() < deadline) {
            try {
                Boolean locked =
                    stringRedisTemplate.hasKey(
                        lockKey
                    );

                if (!Boolean.TRUE.equals(locked)) {
                    return true;
                }

                Thread.sleep(
                    retryInterval.toMillis()
                );

            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();

                return false;

            } catch (DataAccessException exception) {
                log.warn(
                    "Redis became unavailable while " +
                    "waiting for lock. lockKey={}",
                    lockKey
                );

                return false;
            }
        }

        return false;
    }

    public record LockAttempt(
        String lockKey,
        String token,
        boolean acquired,
        boolean redisAvailable
    ) {

        public static LockAttempt acquired(
            String lockKey,
            String token
        ) {
            return new LockAttempt(
                lockKey,
                token,
                true,
                true
            );
        }

        public static LockAttempt contended(
            String lockKey
        ) {
            return new LockAttempt(
                lockKey,
                null,
                false,
                true
            );
        }

        public static LockAttempt redisUnavailable(
            String lockKey
        ) {
            return new LockAttempt(
                lockKey,
                null,
                false,
                false
            );
        }
    }
}