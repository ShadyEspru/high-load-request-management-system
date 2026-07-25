package com.hlrms.requestservice.controller;

import com.hlrms.requestservice.dto.CreateRequestDto;
import com.hlrms.requestservice.dto.PageResponseDto;
import com.hlrms.requestservice.dto.RequestResponseDto;
import com.hlrms.requestservice.entity.RequestStatus;
import com.hlrms.requestservice.service.RequestService;
import com.hlrms.requestservice.dto.CreateRequestResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @PostMapping
    public ResponseEntity<RequestResponseDto> createRequest(
        @RequestHeader("Idempotency-Key")
        @NotBlank(message = "Idempotency-Key is required")
        @Size(
            max = 100,
            message = "Idempotency-Key must not exceed 100 characters"
        )
        String idempotencyKey,

        @Valid
        @RequestBody
        CreateRequestDto createRequestDto
    ) {
        CreateRequestResult result =
            requestService.createRequest(
                createRequestDto,
                idempotencyKey
            );

        if (result.replayed()) {
            return ResponseEntity
                .ok()
                .header("Idempotency-Replayed", "true")
                .body(result.request());
        }

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(result.request().id())
            .toUri();

        return ResponseEntity
            .created(location)
            .header("Idempotency-Replayed", "false")
            .body(result.request());
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
    public ResponseEntity<PageResponseDto<RequestResponseDto>>
    getAllRequests(
        @RequestParam(required = false)
        RequestStatus status,

        @RequestParam(defaultValue = "0")
        @Min(
            value = 0,
            message = "Page number must be zero or greater"
        )
        int page,

        @RequestParam(defaultValue = "20")
        @Min(
            value = 1,
            message = "Page size must be at least 1"
        )
        @Max(
            value = 100,
            message = "Page size must not exceed 100"
        )
        int size
    ) {
        return ResponseEntity.ok(
            requestService.getAllRequests(
                status,
                page,
                size
            )
        );
    }
}