package com.hlrms.requestservice.service;

import com.hlrms.requestservice.config.RedisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisIdempotencyService {

    private static final String VALUE_SEPARATOR = ":";

    private final StringRedisTemplate
        stringRedisTemplate;

    private final RedisProperties redisProperties;

    public Optional<IdempotencyRecord> find(
        String idempotencyKey
    ) {
        String redisKey =
            buildIdempotencyRedisKey(
                idempotencyKey
            );

        try {
            String value =
                stringRedisTemplate
                    .opsForValue()
                    .get(redisKey);

            if (value == null || value.isBlank()) {
                return Optional.empty();
            }

            return parseRecord(value);

        } catch (
            DataAccessException
                | IllegalArgumentException exception
        ) {
            log.warn(
                "Could not read idempotency record " +
                "from Redis. idempotencyKeyHash={}, " +
                "reason={}",
                hashKey(idempotencyKey),
                exception.getMessage()
            );

            return Optional.empty();
        }
    }

    public void save(
        String idempotencyKey,
        String fingerprint,
        UUID requestId
    ) {
        String redisKey =
            buildIdempotencyRedisKey(
                idempotencyKey
            );

        String redisValue =
            fingerprint
                + VALUE_SEPARATOR
                + requestId;

        try {
            stringRedisTemplate
                .opsForValue()
                .set(
                    redisKey,
                    redisValue,
                    redisProperties
                        .getIdempotencyTtl()
                );

        } catch (DataAccessException exception) {
            log.warn(
                "Could not save idempotency record " +
                "to Redis. requestId={}, reason={}",
                requestId,
                exception.getMessage()
            );
        }
    }

    public void delete(
        String idempotencyKey
    ) {
        try {
            stringRedisTemplate.delete(
                buildIdempotencyRedisKey(
                    idempotencyKey
                )
            );

        } catch (DataAccessException exception) {
            log.warn(
                "Could not delete Redis idempotency " +
                "record. reason={}",
                exception.getMessage()
            );
        }
    }

    public String buildLockKey(
        String idempotencyKey
    ) {
        return redisProperties
            .getLockKeyPrefix()
            + hashKey(idempotencyKey);
    }

    private String buildIdempotencyRedisKey(
        String idempotencyKey
    ) {
        return redisProperties
            .getIdempotencyKeyPrefix()
            + hashKey(idempotencyKey);
    }

    private Optional<IdempotencyRecord> parseRecord(
        String value
    ) {
        int separatorIndex =
            value.indexOf(VALUE_SEPARATOR);

        if (
            separatorIndex <= 0
                || separatorIndex
                >= value.length() - 1
        ) {
            return Optional.empty();
        }

        String fingerprint =
            value.substring(0, separatorIndex);

        UUID requestId =
            UUID.fromString(
                value.substring(separatorIndex + 1)
            );

        return Optional.of(
            new IdempotencyRecord(
                fingerprint,
                requestId
            )
        );
    }

    private String hashKey(
        String value
    ) {
        try {
            MessageDigest messageDigest =
                MessageDigest.getInstance(
                    "SHA-256"
                );

            byte[] hash =
                messageDigest.digest(
                    value.getBytes(
                        StandardCharsets.UTF_8
                    )
                );

            return HexFormat
                .of()
                .formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "SHA-256 algorithm is unavailable",
                exception
            );
        }
    }

    public record IdempotencyRecord(
        String fingerprint,
        UUID requestId
    ) {
    }
}