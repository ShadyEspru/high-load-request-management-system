package com.hlrms.requestservice.controller;

import com.hlrms.requestservice.dto.PageResponseDto;
import com.hlrms.requestservice.dto.RequestResponseDto;
import com.hlrms.requestservice.entity.RequestStatus;
import com.hlrms.requestservice.exception.ForbiddenException;
import com.hlrms.requestservice.exception.RequestNotFoundException;
import com.hlrms.requestservice.service.AdminRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class AdminRequestControllerIntegrationTest {

    private static final String BASE_URL =
        "/api/v1/admin/requests";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminRequestService adminRequestService;

    @Test
    void shouldReturnAllRequests() throws Exception {

        RequestResponseDto first =
            createResponse(
                UUID.randomUUID(),
                RequestStatus.PENDING
            );

        RequestResponseDto second =
            createResponse(
                UUID.randomUUID(),
                RequestStatus.COMPLETED
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
            adminRequestService.getAllRequests(
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
            content().contentTypeCompatibleWith(
                MediaType.APPLICATION_JSON
            )
        )
        .andExpect(
            jsonPath("$.content.length()")
                .value(2)
        )
        .andExpect(
            jsonPath("$.content[0].status")
                .value("PENDING")
        )
        .andExpect(
            jsonPath("$.content[1].status")
                .value("COMPLETED")
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
        );

        verify(adminRequestService)
            .getAllRequests(
                null,
                0,
                20
            );
    }

    @Test
    void shouldFilterRequestsByStatus()
        throws Exception {

        RequestResponseDto failedRequest =
            createResponse(
                UUID.randomUUID(),
                RequestStatus.FAILED
            );

        PageResponseDto<RequestResponseDto> page =
            new PageResponseDto<>(
                List.of(failedRequest),
                1,
                10,
                1,
                1,
                false,
                true,
                true,
                false
            );

        when(
            adminRequestService.getAllRequests(
                RequestStatus.FAILED,
                1,
                10
            )
        )
        .thenReturn(page);

        mockMvc.perform(
            get(BASE_URL)
                .param("status", "FAILED")
                .param("page", "1")
                .param("size", "10")
        )
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.content.length()")
                .value(1)
        )
        .andExpect(
            jsonPath("$.content[0].status")
                .value("FAILED")
        )
        .andExpect(
            jsonPath("$.page")
                .value(1)
        )
        .andExpect(
            jsonPath("$.size")
                .value(10)
        );

        verify(adminRequestService)
            .getAllRequests(
                RequestStatus.FAILED,
                1,
                10
            );
    }

    @Test
    void shouldReturnRequestById()
        throws Exception {

        UUID requestId =
            UUID.randomUUID();

        RequestResponseDto response =
            createResponse(
                requestId,
                RequestStatus.COMPLETED
            );

        when(
            adminRequestService.getRequestById(
                requestId
            )
        )
        .thenReturn(response);

        mockMvc.perform(
            get(
                BASE_URL + "/{requestId}",
                requestId
            )
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

        verify(adminRequestService)
            .getRequestById(requestId);
    }

    @Test
    void shouldReturn404WhenRequestDoesNotExist()
        throws Exception {

        UUID requestId =
            UUID.randomUUID();

        when(
            adminRequestService.getRequestById(
                requestId
            )
        )
        .thenThrow(
            new RequestNotFoundException(
                requestId
            )
        );

        mockMvc.perform(
            get(
                BASE_URL + "/{requestId}",
                requestId
            )
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
    void shouldReturn403WhenListingIsForbidden()
        throws Exception {

        when(
            adminRequestService.getAllRequests(
                null,
                0,
                20
            )
        )
        .thenThrow(
            new ForbiddenException(
                "Administrator privileges are required."
            )
        );

        mockMvc.perform(
            get(BASE_URL)
        )
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.status")
                .value(403)
        )
        .andExpect(
            jsonPath("$.error")
                .value("Forbidden")
        )
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Administrator privileges are required."
                )
        );
    }

    @Test
    void shouldReturn403WhenReadingIsForbidden()
        throws Exception {

        UUID requestId =
            UUID.randomUUID();

        when(
            adminRequestService.getRequestById(
                requestId
            )
        )
        .thenThrow(
            new ForbiddenException(
                "Administrator privileges are required."
            )
        );

        mockMvc.perform(
            get(
                BASE_URL + "/{requestId}",
                requestId
            )
        )
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.status")
                .value(403)
        )
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Administrator privileges are required."
                )
        );
    }

    @Test
    void shouldRejectInvalidRequestId()
        throws Exception {

        mockMvc.perform(
            get(
                BASE_URL + "/not-a-uuid"
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
                    "Invalid value for parameter: requestId"
                )
        );

        verify(
            adminRequestService,
            never()
        )
        .getRequestById(any());
    }

    @Test
    void shouldRejectInvalidStatus()
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

        verify(
            adminRequestService,
            never()
        )
        .getAllRequests(
            any(),
            any(Integer.class),
            any(Integer.class)
        );
    }

    @Test
    void shouldPassCustomPaginationValues()
        throws Exception {

        PageResponseDto<RequestResponseDto> page =
            new PageResponseDto<>(
                List.of(),
                3,
                50,
                0,
                0,
                false,
                true,
                true,
                false
            );

        when(
            adminRequestService.getAllRequests(
                null,
                3,
                50
            )
        )
        .thenReturn(page);

        mockMvc.perform(
            get(BASE_URL)
                .param("page", "3")
                .param("size", "50")
        )
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.page")
                .value(3)
        )
        .andExpect(
            jsonPath("$.size")
                .value(50)
        );

        verify(adminRequestService)
            .getAllRequests(
                null,
                3,
                50
            );
    }

    @Test
    void shouldReturn500ForUnexpectedServiceFailure()
        throws Exception {

        when(
            adminRequestService.getAllRequests(
                null,
                0,
                20
            )
        )
        .thenThrow(
            new IllegalStateException(
                "Unexpected database failure"
            )
        );

        mockMvc.perform(
            get(BASE_URL)
        )
        .andExpect(
            status().isInternalServerError()
        )
        .andExpect(
            jsonPath("$.status")
                .value(500)
        )
        .andExpect(
            jsonPath("$.error")
                .value(
                    "Internal Server Error"
                )
        )
        .andExpect(
            jsonPath("$.message")
                .value(
                    "An unexpected error occurred"
                )
        );
    }

    private RequestResponseDto createResponse(
        UUID requestId,
        RequestStatus status
    ) {
        Instant now =
            Instant.now();

        boolean completed =
            status == RequestStatus.COMPLETED;

        boolean failed =
            status == RequestStatus.FAILED;

        return new RequestResponseDto(
            requestId,
            "admin-test-" + UUID.randomUUID(),
            "ADMIN_TEST",
            "{\"message\":\"admin controller test\"}",
            status,
            completed
                ? "Request processed successfully"
                : null,
            failed
                ? "Request processing failed"
                : null,
            now.minusSeconds(10),
            now,
            completed || failed
                ? now
                : null,
            0L
        );
    }
}