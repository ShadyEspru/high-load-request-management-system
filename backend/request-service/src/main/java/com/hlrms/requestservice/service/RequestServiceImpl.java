package com.hlrms.requestservice.service;

import com.hlrms.requestservice.dto.CreateRequestDto;
import com.hlrms.requestservice.dto.CreateRequestResult;
import com.hlrms.requestservice.dto.PageResponseDto;
import com.hlrms.requestservice.dto.RequestResponseDto;
import com.hlrms.requestservice.entity.RequestEntity;
import com.hlrms.requestservice.entity.RequestStatus;
import com.hlrms.requestservice.exception.IdempotencyConflictException;
import com.hlrms.requestservice.exception.RequestNotFoundException;
import com.hlrms.requestservice.repository.RequestRepository;
import com.hlrms.requestservice.service.RedisDistributedLockService.LockAttempt;
import com.hlrms.requestservice.service.RedisIdempotencyService.IdempotencyRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;

    private final RequestCreationTransactionService
        requestCreationTransactionService;

    private final RedisIdempotencyService
        redisIdempotencyService;

    private final RedisDistributedLockService
        redisDistributedLockService;

    private final RequestCacheService
        requestCacheService;

    @Override
    public CreateRequestResult createRequest(
        CreateRequestDto createRequestDto,
        String idempotencyKey
    ) {
        String normalizedKey =
            idempotencyKey.trim();

        String normalizedRequestType =
            createRequestDto
                .requestType()
                .trim();

        String normalizedPayload =
            createRequestDto
                .payload()
                .trim();

        String fingerprint =
            generateFingerprint(
                normalizedRequestType,
                normalizedPayload
            );

        Optional<CreateRequestResult>
            redisReplay =
            findReplayFromRedis(
                normalizedKey,
                fingerprint
            );

        if (redisReplay.isPresent()) {
            return redisReplay.get();
        }

        String lockKey =
            redisIdempotencyService
                .buildLockKey(normalizedKey);

        LockAttempt lockAttempt =
            redisDistributedLockService
                .tryAcquire(lockKey);

        if (
            lockAttempt.redisAvailable()
                && !lockAttempt.acquired()
        ) {
            redisDistributedLockService
                .waitUntilUnlocked(lockKey);

            Optional<CreateRequestResult>
                concurrentReplay =
                findExistingRequest(
                    normalizedKey,
                    fingerprint
                );

            if (concurrentReplay.isPresent()) {
                return concurrentReplay.get();
            }
        }

        try {
            Optional<CreateRequestResult>
                existingRequest =
                findExistingRequest(
                    normalizedKey,
                    fingerprint
                );

            if (existingRequest.isPresent()) {
                return existingRequest.get();
            }

            RequestEntity savedRequest =
                requestCreationTransactionService
                    .createRequestWithOutboxEvent(
                        normalizedKey,
                        fingerprint,
                        normalizedRequestType,
                        normalizedPayload
                    );

            redisIdempotencyService.save(
                normalizedKey,
                fingerprint,
                savedRequest.getId()
            );

            log.info(
                "Request created with distributed " +
                "idempotency protection. " +
                "requestId={}",
                savedRequest.getId()
            );

            return new CreateRequestResult(
                toResponseDto(savedRequest),
                false
            );

        } catch (
            DataIntegrityViolationException exception
        ) {
            RequestEntity concurrentRequest =
                requestRepository
                    .findByIdempotencyKey(
                        normalizedKey
                    )
                    .orElseThrow(() -> exception);

            CreateRequestResult replayResult =
                handleExistingRequest(
                    concurrentRequest,
                    fingerprint
                );

            redisIdempotencyService.save(
                normalizedKey,
                fingerprint,
                concurrentRequest.getId()
            );

            return replayResult;

        } finally {
            redisDistributedLockService.release(
                lockAttempt
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RequestResponseDto getRequestById(
        UUID requestId
    ) {
        Optional<RequestResponseDto> cachedRequest =
            requestCacheService.find(requestId);

        if (cachedRequest.isPresent()) {
            log.debug(
                "Request returned from Redis cache. " +
                "requestId={}",
                requestId
            );

            return cachedRequest.get();
        }

        RequestEntity requestEntity =
            requestRepository
                .findById(requestId)
                .orElseThrow(
                    () -> new RequestNotFoundException(
                        requestId
                    )
                );

        RequestResponseDto response =
            toResponseDto(requestEntity);

        requestCacheService.saveIfTerminal(response);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<RequestResponseDto>
    getAllRequests(
        RequestStatus status,
        int page,
        int size
    ) {
        Pageable pageable =
            PageRequest.of(
                page,
                size,
                Sort.by(
                    Sort.Direction.DESC,
                    "createdAt"
                )
            );

        Page<RequestEntity> requestPage;

        if (status == null) {
            requestPage =
                requestRepository.findAll(pageable);
        } else {
            requestPage =
                requestRepository.findAllByStatus(
                    status,
                    pageable
                );
        }

        return new PageResponseDto<>(
            requestPage
                .getContent()
                .stream()
                .map(this::toResponseDto)
                .toList(),
            requestPage.getNumber(),
            requestPage.getSize(),
            requestPage.getTotalElements(),
            requestPage.getTotalPages(),
            requestPage.isFirst(),
            requestPage.isLast(),
            requestPage.hasNext(),
            requestPage.hasPrevious()
        );
    }

    private Optional<CreateRequestResult>
    findReplayFromRedis(
        String idempotencyKey,
        String incomingFingerprint
    ) {
        Optional<IdempotencyRecord> cachedRecord =
            redisIdempotencyService.find(
                idempotencyKey
            );

        if (cachedRecord.isEmpty()) {
            return Optional.empty();
        }

        IdempotencyRecord record =
            cachedRecord.get();

        validateFingerprint(
            record.fingerprint(),
            incomingFingerprint
        );

        Optional<RequestEntity> request =
            requestRepository.findById(
                record.requestId()
            );

        if (request.isEmpty()) {
            redisIdempotencyService.delete(
                idempotencyKey
            );

            return Optional.empty();
        }

        return Optional.of(
            new CreateRequestResult(
                toResponseDto(request.get()),
                true
            )
        );
    }

    private Optional<CreateRequestResult>
    findExistingRequest(
        String idempotencyKey,
        String incomingFingerprint
    ) {
        Optional<RequestEntity> existingRequest =
            requestRepository
                .findByIdempotencyKey(
                    idempotencyKey
                );

        if (existingRequest.isEmpty()) {
            return Optional.empty();
        }

        RequestEntity request =
            existingRequest.get();

        CreateRequestResult result =
            handleExistingRequest(
                request,
                incomingFingerprint
            );

        redisIdempotencyService.save(
            idempotencyKey,
            request.getIdempotencyFingerprint(),
            request.getId()
        );

        return Optional.of(result);
    }

    private CreateRequestResult handleExistingRequest(
        RequestEntity existingRequest,
        String incomingFingerprint
    ) {
        validateFingerprint(
            existingRequest
                .getIdempotencyFingerprint(),
            incomingFingerprint
        );

        return new CreateRequestResult(
            toResponseDto(existingRequest),
            true
        );
    }

    private void validateFingerprint(
        String existingFingerprint,
        String incomingFingerprint
    ) {
        if (
            !existingFingerprint.equals(
                incomingFingerprint
            )
        ) {
            throw new IdempotencyConflictException(
                "The Idempotency-Key has already " +
                "been used with a different " +
                "request payload"
            );
        }
    }

    private String generateFingerprint(
        String requestType,
        String payload
    ) {
        String fingerprintSource =
            requestType + "\n" + payload;

        try {
            MessageDigest messageDigest =
                MessageDigest.getInstance(
                    "SHA-256"
                );

            byte[] hash =
                messageDigest.digest(
                    fingerprintSource.getBytes(
                        StandardCharsets.UTF_8
                    )
                );

            return HexFormat
                .of()
                .formatHex(hash);

        } catch (
            NoSuchAlgorithmException exception
        ) {
            throw new IllegalStateException(
                "SHA-256 algorithm is not available",
                exception
            );
        }
    }

    private RequestResponseDto toResponseDto(
        RequestEntity requestEntity
    ) {
        return new RequestResponseDto(
            requestEntity.getId(),
            requestEntity.getIdempotencyKey(),
            requestEntity.getRequestType(),
            requestEntity.getPayload(),
            requestEntity.getStatus(),
            requestEntity.getResult(),
            requestEntity.getErrorMessage(),
            requestEntity.getCreatedAt(),
            requestEntity.getUpdatedAt(),
            requestEntity.getCompletedAt(),
            requestEntity.getVersion()
        );
    }
}