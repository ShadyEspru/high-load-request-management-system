package com.hlrms.requestservice.controller;

import com.hlrms.requestservice.dto.CreateRequestDto;
import com.hlrms.requestservice.dto.CreateRequestResult;
import com.hlrms.requestservice.dto.PageResponseDto;
import com.hlrms.requestservice.dto.RequestResponseDto;
import com.hlrms.requestservice.entity.RequestStatus;
import com.hlrms.requestservice.exception.IdempotencyConflictException;
import com.hlrms.requestservice.exception.RequestNotFoundException;
import com.hlrms.requestservice.service.RequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "hlrms.outbox.publisher-enabled=false"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequestControllerIntegrationTest {

    private static final String BASE_URL =
        "/api/v1/requests";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequestService requestService;

    @Test
    void shouldCreateRequestAndReturn201() throws Exception {

        UUID requestId = UUID.randomUUID();

        RequestResponseDto response =
            createResponse(
                requestId,
                RequestStatus.PENDING
            );

        when(
            requestService.createRequest(
                any(CreateRequestDto.class),
                eq("create-key-1")
            )
        )
        .thenReturn(
            new CreateRequestResult(
                response,
                false
            )
        );

        mockMvc.perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header(
                    "Idempotency-Key",
                    "create-key-1"
                )
                .content(
                    """
                    {
                      "requestType": "IMAGE_PROCESSING",
                      "payload": "{\\"imageId\\":\\"123\\"}"
                    }
                    """
                )
        )
        .andExpect(status().isCreated())
        .andExpect(
            header().string(
                "Idempotency-Replayed",
                "false"
            )
        )
        .andExpect(
            header().string(
                "Location",
                "http://localhost/api/v1/requests/"
                    + requestId
            )
        )
        .andExpect(
            content().contentTypeCompatibleWith(
                MediaType.APPLICATION_JSON
            )
        )
        .andExpect(
            jsonPath("$.id")
                .value(requestId.toString())
        )
        .andExpect(
            jsonPath("$.idempotencyKey")
                .value("test-idempotency-key")
        )
        .andExpect(
            jsonPath("$.requestType")
                .value("IMAGE_PROCESSING")
        )
        .andExpect(
            jsonPath("$.status")
                .value("PENDING")
        );

        verify(requestService)
            .createRequest(
                any(CreateRequestDto.class),
                eq("create-key-1")
            );
    }

    @Test
    void shouldReturn200ForIdempotencyReplay()
        throws Exception {

        UUID requestId = UUID.randomUUID();

        RequestResponseDto response =
            createResponse(
                requestId,
                RequestStatus.PENDING
            );

        when(
            requestService.createRequest(
                any(CreateRequestDto.class),
                eq("replay-key")
            )
        )
        .thenReturn(
            new CreateRequestResult(
                response,
                true
            )
        );

        mockMvc.perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header(
                    "Idempotency-Key",
                    "replay-key"
                )
                .content(
                    """
                    {
                      "requestType": "IMAGE_PROCESSING",
                      "payload": "{\\"imageId\\":\\"123\\"}"
                    }
                    """
                )
        )
        .andExpect(status().isOk())
        .andExpect(
            header().string(
                "Idempotency-Replayed",
                "true"
            )
        )
        .andExpect(
            header().doesNotExist("Location")
        )
        .andExpect(
            jsonPath("$.id")
                .value(requestId.toString())
        );
    }

    @Test
    void shouldRejectCreateRequestWithoutIdempotencyKey()
        throws Exception {

        mockMvc.perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "requestType": "IMAGE_PROCESSING",
                      "payload": "{\\"imageId\\":\\"123\\"}"
                    }
                    """
                )
        )
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.status")
                .value(400)
        )
        .andExpect(
            jsonPath("$.error")
                .value("Bad Request")
        )
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Required request header is missing"
                )
        )
        .andExpect(
            jsonPath(
                "$.details['Idempotency-Key']"
            )
            .value(
                "Idempotency-Key header is required"
            )
        );

        verify(
            requestService,
            never()
        )
        .createRequest(
            any(),
            any()
        );
    }

    @Test
    void shouldRejectBlankIdempotencyKey()
        throws Exception {

        mockMvc.perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header(
                    "Idempotency-Key",
                    "   "
                )
                .content(
                    """
                    {
                      "requestType": "IMAGE_PROCESSING",
                      "payload": "{\\"imageId\\":\\"123\\"}"
                    }
                    """
                )
        )
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.status")
                .value(400)
        )
        .andExpect(
            jsonPath("$.message")
                .value("Validation failed")
        );
    }

    @Test
    void shouldRejectInvalidCreateRequestBody()
        throws Exception {

        mockMvc.perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header(
                    "Idempotency-Key",
                    "validation-key"
                )
                .content(
                    """
                    {
                      "requestType": "",
                      "payload": "valid payload"
                    }
                    """
                )
        )
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.status")
                .value(400)
        )
        .andExpect(
            jsonPath("$.message")
                .value("Validation failed")
        )
        .andExpect(
            jsonPath("$.details.createRequestDto")
                .value("Request type is required")
        );

        verify(
            requestService,
            never()
        )
        .createRequest(
            any(),
            any()
        );
    }

    @Test
    void shouldRejectRequestTypeLongerThanLimit()
        throws Exception {

        String longRequestType =
            "x".repeat(101);

        mockMvc.perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header(
                    "Idempotency-Key",
                    "long-type-key"
                )
                .content(
                    """
                    {
                      "requestType": "%s",
                      "payload": "valid payload"
                    }
                    """
                    .formatted(longRequestType)
                )
        )
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.details.createRequestDto")
                .value(
                    "Request type must not exceed "
                        + "100 characters"
                )
        );
    }

    @Test
    void shouldReturn409ForIdempotencyConflict()
        throws Exception {

        when(
            requestService.createRequest(
                any(CreateRequestDto.class),
                eq("conflict-key")
            )
        )
        .thenThrow(
            new IdempotencyConflictException(
                "The Idempotency-Key has already "
                    + "been used with a different "
                    + "request payload"
            )
        );

        mockMvc.perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header(
                    "Idempotency-Key",
                    "conflict-key"
                )
                .content(
                    """
                    {
                      "requestType": "IMAGE_PROCESSING",
                      "payload": "different payload"
                    }
                    """
                )
        )
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.status")
                .value(409)
        )
        .andExpect(
            jsonPath("$.error")
                .value("Conflict")
        )
        .andExpect(
            jsonPath("$.message")
                .value(
                    "The Idempotency-Key has already "
                        + "been used with a different "
                        + "request payload"
                )
        );
    }

    @Test
    void shouldGetRequestById() throws Exception {

        UUID requestId = UUID.randomUUID();

        RequestResponseDto response =
            createResponse(
                requestId,
                RequestStatus.COMPLETED
            );

        when(
            requestService.getRequestById(requestId)
        )
        .thenReturn(response);

        mockMvc.perform(
            get(BASE_URL + "/{id}", requestId)
        )
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.id")
                .value(requestId.toString())
        )
        .andExpect(
            jsonPath("$.status")
                .value("COMPLETED")
        )
        .andExpect(
            jsonPath("$.result")
                .value(
                    "Request processed successfully"
                )
        );

        verify(requestService)
            .getRequestById(requestId);
    }

    @Test
    void shouldReturn404WhenRequestDoesNotExist()
        throws Exception {

        UUID requestId = UUID.randomUUID();

        when(
            requestService.getRequestById(requestId)
        )
        .thenThrow(
            new RequestNotFoundException(requestId)
        );

        mockMvc.perform(
            get(BASE_URL + "/{id}", requestId)
        )
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.status")
                .value(404)
        )
        .andExpect(
            jsonPath("$.error")
                .value("Not Found")
        )
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Request not found with id: "
                        + requestId
                )
        );
    }

    @Test
    void shouldRejectInvalidRequestId()
        throws Exception {

        mockMvc.perform(
            get(BASE_URL + "/not-a-uuid")
        )
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.status")
                .value(400)
        )
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Invalid value for parameter: id"
                )
        );
    }

    @Test
    void shouldGetPagedRequests()
        throws Exception {

        RequestResponseDto first =
            createResponse(
                UUID.randomUUID(),
                RequestStatus.PENDING
            );

        RequestResponseDto second =
            createResponse(
                UUID.randomUUID(),
                RequestStatus.PENDING
            );

        PageResponseDto<RequestResponseDto> page =
            new PageResponseDto<>(
                List.of(first, second),
                0,
                20,
                2,
                1,
                true,
                true,
                false,
                false
            );

        when(
            requestService.getAllRequests(
                RequestStatus.PENDING,
                0,
                20
            )
        )
        .thenReturn(page);

        mockMvc.perform(
            get(BASE_URL)
                .param("status", "PENDING")
                .param("page", "0")
                .param("size", "20")
        )
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.content.length()")
                .value(2)
        )
        .andExpect(
            jsonPath("$.page")
                .value(0)
        )
        .andExpect(
            jsonPath("$.size")
                .value(20)
        )
        .andExpect(
            jsonPath("$.totalElements")
                .value(2)
        )
        .andExpect(
            jsonPath("$.totalPages")
                .value(1)
        )
        .andExpect(
            jsonPath("$.first")
                .value(true)
        )
        .andExpect(
            jsonPath("$.last")
                .value(true)
        );

        verify(requestService)
            .getAllRequests(
                RequestStatus.PENDING,
                0,
                20
            );
    }

    @Test
    void shouldUseDefaultPaginationValues()
        throws Exception {

        PageResponseDto<RequestResponseDto> page =
            new PageResponseDto<>(
                List.of(),
                0,
                20,
                0,
                0,
                true,
                true,
                false,
                false
            );

        when(
            requestService.getAllRequests(
                null,
                0,
                20
            )
        )
        .thenReturn(page);

        mockMvc.perform(
            get(BASE_URL)
        )
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.page")
                .value(0)
        )
        .andExpect(
            jsonPath("$.size")
                .value(20)
        )
        .andExpect(
            jsonPath("$.content.length()")
                .value(0)
        );

        verify(requestService)
            .getAllRequests(
                null,
                0,
                20
            );
    }

    @Test
    void shouldRejectNegativePageNumber()
        throws Exception {

        mockMvc.perform(
            get(BASE_URL)
                .param("page", "-1")
                .param("size", "20")
        )
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.status")
                .value(400)
        )
        .andExpect(
            jsonPath("$.message")
                .value("Validation failed")
        );
    }

    @Test
    void shouldRejectPageSizeGreaterThan100()
        throws Exception {

        mockMvc.perform(
            get(BASE_URL)
                .param("page", "0")
                .param("size", "101")
        )
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.status")
                .value(400)
        )
        .andExpect(
            jsonPath("$.message")
                .value("Validation failed")
        );
    }

    @Test
    void shouldRejectInvalidStatusValue()
        throws Exception {

        mockMvc.perform(
            get(BASE_URL)
                .param(
                    "status",
                    "UNKNOWN_STATUS"
                )
        )
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.status")
                .value(400)
        )
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Invalid value for parameter: status"
                )
        );
    }

    private RequestResponseDto createResponse(
        UUID requestId,
        RequestStatus status
    ) {
        Instant now = Instant.now();

        boolean completed =
            status == RequestStatus.COMPLETED;

        boolean failed =
            status == RequestStatus.FAILED;

        return new RequestResponseDto(
            requestId,
            "test-idempotency-key",
            "IMAGE_PROCESSING",
            "{\"imageId\":\"123\"}",
            status,
            completed
                ? "Request processed successfully"
                : null,
            failed
                ? "Request processing failed"
                : null,
            now.minusSeconds(5),
            now,
            completed || failed
                ? now
                : null,
            0L
        );
    }
}