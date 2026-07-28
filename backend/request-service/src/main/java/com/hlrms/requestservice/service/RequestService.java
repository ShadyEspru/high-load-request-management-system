package com.hlrms.requestservice.service;

import com.hlrms.requestservice.dto.CreateRequestDto;
import com.hlrms.requestservice.dto.CreateRequestResult;
import com.hlrms.requestservice.dto.PageResponseDto;
import com.hlrms.requestservice.dto.RequestResponseDto;
import com.hlrms.requestservice.entity.RequestStatus;

import java.util.UUID;

public interface RequestService {

    CreateRequestResult createRequest(
        CreateRequestDto createRequestDto,
        String idempotencyKey
    );

    RequestResponseDto getRequestById(UUID requestId);

    PageResponseDto<RequestResponseDto> getAllRequests(
        RequestStatus status,
        int page,
        int size
    );
}