package com.hlrms.requestservice.service;

import com.hlrms.requestservice.config.RedisProperties;
import com.hlrms.requestservice.dto.RequestResponseDto;
import com.hlrms.requestservice.entity.RequestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestCacheService {

    private final StringRedisTemplate
        stringRedisTemplate;

    private final RedisProperties redisProperties;

    private final JsonMapper jsonMapper;

    public Optional<RequestResponseDto> find(
        UUID requestId
    ) {
        String redisKey =
            buildRequestCacheKey(requestId);

        try {
            String cachedValue =
                stringRedisTemplate
                    .opsForValue()
                    .get(redisKey);

            if (
                cachedValue == null
                    || cachedValue.isBlank()
            ) {
                return Optional.empty();
            }

            RequestResponseDto request =
                jsonMapper.readValue(
                    cachedValue,
                    RequestResponseDto.class
                );

            return Optional.of(request);

        } catch (
            DataAccessException
                | JacksonException exception
        ) {
            log.warn(
                "Could not read request from Redis " +
                "cache. requestId={}, reason={}",
                requestId,
                exception.getMessage()
            );

            delete(requestId);

            return Optional.empty();
        }
    }

    public void saveIfTerminal(
        RequestResponseDto request
    ) {
        if (!isTerminal(request.status())) {
            return;
        }

        String redisKey =
            buildRequestCacheKey(request.id());

        try {
            String serializedRequest =
                jsonMapper.writeValueAsString(
                    request
                );

            stringRedisTemplate
                .opsForValue()
                .set(
                    redisKey,
                    serializedRequest,
                    redisProperties
                        .getRequestCacheTtl()
                );

        } catch (
            DataAccessException
                | JacksonException exception
        ) {
            log.warn(
                "Could not cache terminal request. " +
                "requestId={}, reason={}",
                request.id(),
                exception.getMessage()
            );
        }
    }

    public void delete(
        UUID requestId
    ) {
        try {
            stringRedisTemplate.delete(
                buildRequestCacheKey(requestId)
            );

        } catch (DataAccessException exception) {
            log.warn(
                "Could not delete request cache. " +
                "requestId={}, reason={}",
                requestId,
                exception.getMessage()
            );
        }
    }

    private boolean isTerminal(
        RequestStatus status
    ) {
        return status == RequestStatus.COMPLETED
            || status == RequestStatus.FAILED;
    }

    private String buildRequestCacheKey(
        UUID requestId
    ) {
        return redisProperties
            .getRequestCacheKeyPrefix()
            + requestId;
    }
}