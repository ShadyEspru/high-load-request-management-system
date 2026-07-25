package com.hlrms.requestservice.service;

import com.hlrms.requestservice.dto.CreateRequestDto;
import com.hlrms.requestservice.dto.RequestResponseDto;
import com.hlrms.requestservice.entity.RequestStatus;

import java.util.List;
import java.util.UUID;

public interface RequestService {

    RequestResponseDto createRequest(CreateRequestDto createRequestDto);

    RequestResponseDto getRequestById(UUID requestId);

    List<RequestResponseDto> getAllRequests(RequestStatus status);
}