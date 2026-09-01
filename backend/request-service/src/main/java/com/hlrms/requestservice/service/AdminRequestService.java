package com.hlrms.requestservice.service;

import com.hlrms.requestservice.dto.PageResponseDto;
import com.hlrms.requestservice.dto.RequestResponseDto;
import com.hlrms.requestservice.entity.RequestStatus;

import java.util.UUID;

public interface AdminRequestService {

    PageResponseDto<RequestResponseDto>
    getAllRequests(
            RequestStatus status,
            int page,
            int size
    );

    RequestResponseDto
    getRequestById(
            UUID requestId
    );
}