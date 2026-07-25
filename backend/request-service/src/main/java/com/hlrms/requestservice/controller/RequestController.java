package com.hlrms.requestservice.controller;

import com.hlrms.requestservice.dto.CreateRequestDto;
import com.hlrms.requestservice.dto.RequestResponseDto;
import com.hlrms.requestservice.entity.RequestStatus;
import com.hlrms.requestservice.service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @PostMapping
    public ResponseEntity<RequestResponseDto> createRequest(
        @Valid @RequestBody CreateRequestDto createRequestDto
    ) {
        RequestResponseDto createdRequest =
            requestService.createRequest(createRequestDto);

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(createdRequest.id())
            .toUri();

        return ResponseEntity
            .created(location)
            .body(createdRequest);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RequestResponseDto> getRequestById(
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
            requestService.getRequestById(id)
        );
    }

    @GetMapping
    public ResponseEntity<List<RequestResponseDto>> getAllRequests(
        @RequestParam(required = false)
        RequestStatus status
    ) {
        return ResponseEntity.ok(
            requestService.getAllRequests(status)
        );
    }
}