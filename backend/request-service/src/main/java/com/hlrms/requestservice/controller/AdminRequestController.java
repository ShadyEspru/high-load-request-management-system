package com.hlrms.requestservice.controller;

import com.hlrms.requestservice.dto.PageResponseDto;
import com.hlrms.requestservice.dto.RequestResponseDto;
import com.hlrms.requestservice.entity.RequestStatus;
import com.hlrms.requestservice.service.AdminRequestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/requests")
public class AdminRequestController {

    private final AdminRequestService adminRequestService;

    public AdminRequestController(
            AdminRequestService adminRequestService
    ) {
        this.adminRequestService = adminRequestService;
    }

    @GetMapping
    public PageResponseDto<RequestResponseDto> getAllRequests(

            @RequestParam(required = false)
            RequestStatus status,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        return adminRequestService.getAllRequests(
                status,
                page,
                size
        );
    }

    @GetMapping("/{requestId}")
    public RequestResponseDto getRequestById(
            @PathVariable
            java.util.UUID requestId
    ) {

        return adminRequestService.getRequestById(
                requestId
        );
    }
}