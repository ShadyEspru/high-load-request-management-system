package com.hlrms.requestservice.service;

import com.hlrms.requestservice.messaging.RequestEventPublisher;
import com.hlrms.requestservice.dto.CreateRequestDto;
import com.hlrms.requestservice.dto.CreateRequestResult;
import com.hlrms.requestservice.dto.PageResponseDto;
import com.hlrms.requestservice.dto.RequestResponseDto;
import com.hlrms.requestservice.entity.RequestEntity;
import com.hlrms.requestservice.entity.RequestStatus;
import com.hlrms.requestservice.exception.IdempotencyConflictException;
import com.hlrms.requestservice.exception.RequestNotFoundException;
import com.hlrms.requestservice.repository.RequestRepository;

import lombok.RequiredArgsConstructor;

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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final RequestEventPublisher requestEventPublisher;

    @Override
    public CreateRequestResult createRequest(
        CreateRequestDto createRequestDto,
        String idempotencyKey
    ) {
        String normalizedKey = idempotencyKey.trim();

        String normalizedRequestType =
            createRequestDto.requestType().trim();

        String normalizedPayload =
            createRequestDto.payload().trim();

        String fingerprint = generateFingerprint(
            normalizedRequestType,
            normalizedPayload
        );

        var existingRequest =
            requestRepository.findByIdempotencyKey(
                normalizedKey
            );

        if (existingRequest.isPresent()) {
            return handleExistingRequest(
                existingRequest.get(),
                fingerprint
            );
        }

        RequestEntity requestEntity =
            RequestEntity.builder()
                .idempotencyKey(normalizedKey)
                .idempotencyFingerprint(fingerprint)
                .requestType(normalizedRequestType)
                .payload(normalizedPayload)
                .status(RequestStatus.PENDING)
                .build();

        try {
            RequestEntity savedRequest =
                requestRepository.saveAndFlush(requestEntity);

            requestEventPublisher.publishRequestCreated(
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
                    .findByIdempotencyKey(normalizedKey)
                    .orElseThrow(() -> exception);

            return handleExistingRequest(
                concurrentRequest,
                fingerprint
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RequestResponseDto getRequestById(
        UUID requestId
    ) {
        RequestEntity requestEntity =
            requestRepository
                .findById(requestId)
                .orElseThrow(
                    () -> new RequestNotFoundException(
                        requestId
                    )
                );

        return toResponseDto(requestEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<RequestResponseDto>
    getAllRequests(
        RequestStatus status,
        int page,
        int size
    ) {
        Pageable pageable = PageRequest.of(
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

    private CreateRequestResult handleExistingRequest(
        RequestEntity existingRequest,
        String incomingFingerprint
    ) {
        if (!existingRequest
            .getIdempotencyFingerprint()
            .equals(incomingFingerprint)) {

            throw new IdempotencyConflictException(
                "The Idempotency-Key has already " +
                "been used with a different " +
                "request payload"
            );
        }

        return new CreateRequestResult(
            toResponseDto(existingRequest),
            true
        );
    }

    private String generateFingerprint(
        String requestType,
        String payload
    ) {
        String fingerprintSource =
            requestType + "\n" + payload;

        try {
            MessageDigest messageDigest =
                MessageDigest.getInstance("SHA-256");

            byte[] hash = messageDigest.digest(
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