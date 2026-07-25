package com.hlrms.requestservice.service;

import com.hlrms.requestservice.dto.CreateRequestDto;
import com.hlrms.requestservice.dto.RequestResponseDto;
import com.hlrms.requestservice.entity.RequestEntity;
import com.hlrms.requestservice.entity.RequestStatus;
import com.hlrms.requestservice.exception.RequestNotFoundException;
import com.hlrms.requestservice.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;

    @Override
    @Transactional
    public RequestResponseDto createRequest(
        CreateRequestDto createRequestDto
    ) {
        RequestEntity requestEntity = RequestEntity.builder()
            .requestType(createRequestDto.requestType().trim())
            .payload(createRequestDto.payload())
            .status(RequestStatus.PENDING)
            .build();

        RequestEntity savedRequest =
            requestRepository.save(requestEntity);

        return toResponseDto(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public RequestResponseDto getRequestById(UUID requestId) {
        RequestEntity requestEntity = requestRepository
            .findById(requestId)
            .orElseThrow(
                () -> new RequestNotFoundException(requestId)
            );

        return toResponseDto(requestEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequestResponseDto> getAllRequests(
        RequestStatus status
    ) {
        List<RequestEntity> requests;

        if (status == null) {
            requests =
                requestRepository.findAllByOrderByCreatedAtDesc();
        } else {
            requests =
                requestRepository
                    .findAllByStatusOrderByCreatedAtDesc(status);
        }

        return requests.stream()
            .map(this::toResponseDto)
            .toList();
    }

    private RequestResponseDto toResponseDto(
        RequestEntity requestEntity
    ) {
        return new RequestResponseDto(
            requestEntity.getId(),
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