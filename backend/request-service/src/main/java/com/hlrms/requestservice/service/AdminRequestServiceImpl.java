package com.hlrms.requestservice.service;

import com.hlrms.requestservice.dto.PageResponseDto;
import com.hlrms.requestservice.dto.RequestResponseDto;
import com.hlrms.requestservice.entity.RequestEntity;
import com.hlrms.requestservice.entity.RequestStatus;
import com.hlrms.requestservice.exception.RequestNotFoundException;
import com.hlrms.requestservice.repository.RequestRepository;
import com.hlrms.requestservice.security.RoleAuthorizationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminRequestServiceImpl
        implements AdminRequestService {

    private final RequestRepository requestRepository;

    private final RoleAuthorizationService
            roleAuthorizationService;

    public AdminRequestServiceImpl(
            RequestRepository requestRepository,
            RoleAuthorizationService roleAuthorizationService
    ) {
        this.requestRepository = requestRepository;
        this.roleAuthorizationService =
                roleAuthorizationService;
    }

    @Override
    public PageResponseDto<RequestResponseDto>
    getAllRequests(
            RequestStatus status,
            int page,
            int size
    ) {
        roleAuthorizationService.requireAdmin();

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

        return toPageResponse(requestPage);
    }

    @Override
    public RequestResponseDto getRequestById(
            UUID requestId
    ) {
        roleAuthorizationService.requireAdmin();

        RequestEntity entity =
                requestRepository
                        .findById(requestId)
                        .orElseThrow(
                                () ->
                                        new RequestNotFoundException(
                                                requestId
                                        )
                        );

        return toResponseDto(entity);
    }

    private PageResponseDto<RequestResponseDto>
    toPageResponse(
            Page<RequestEntity> requestPage
    ) {
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