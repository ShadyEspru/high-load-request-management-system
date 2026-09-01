package com.hlrms.requestservice.service;

import com.hlrms.requestservice.dto.PageResponseDto;
import com.hlrms.requestservice.dto.RequestResponseDto;
import com.hlrms.requestservice.entity.RequestEntity;
import com.hlrms.requestservice.entity.RequestStatus;
import com.hlrms.requestservice.exception.ForbiddenException;
import com.hlrms.requestservice.exception.RequestNotFoundException;
import com.hlrms.requestservice.repository.RequestRepository;
import com.hlrms.requestservice.security.RoleAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRequestServiceImplTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private RoleAuthorizationService
        roleAuthorizationService;

    private AdminRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
            new AdminRequestServiceImpl(
                requestRepository,
                roleAuthorizationService
            );
    }

    @Test
    void shouldReturnAllRequestsForAdmin() {

        RequestEntity first =
            createRequest(
                UUID.randomUUID(),
                RequestStatus.PENDING
            );

        RequestEntity second =
            createRequest(
                UUID.randomUUID(),
                RequestStatus.COMPLETED
            );

        Page<RequestEntity> repositoryPage =
            new PageImpl<>(
                List.of(first, second)
            );

        when(
            requestRepository.findAll(
                any(Pageable.class)
            )
        )
        .thenReturn(repositoryPage);

        PageResponseDto<RequestResponseDto> result =
            service.getAllRequests(
                null,
                0,
                20
            );

        verify(roleAuthorizationService)
            .requireAdmin();

        assertThat(result.content())
            .hasSize(2);

        assertThat(result.content())
            .extracting(RequestResponseDto::id)
            .containsExactly(
                first.getId(),
                second.getId()
            );

        assertThat(result.totalElements())
            .isEqualTo(2);

        ArgumentCaptor<Pageable> pageableCaptor =
            ArgumentCaptor.forClass(
                Pageable.class
            );

        verify(requestRepository)
            .findAll(
                pageableCaptor.capture()
            );

        Pageable pageable =
            pageableCaptor.getValue();

        assertThat(pageable.getPageNumber())
            .isZero();

        assertThat(pageable.getPageSize())
            .isEqualTo(20);

        assertThat(
            pageable.getSort()
                .getOrderFor("createdAt")
        )
        .isNotNull();

        assertThat(
            pageable.getSort()
                .getOrderFor("createdAt")
                .isDescending()
        )
        .isTrue();
    }

    @Test
    void shouldFilterRequestsByStatus() {

        RequestEntity failedRequest =
            createRequest(
                UUID.randomUUID(),
                RequestStatus.FAILED
            );

        Page<RequestEntity> repositoryPage =
            new PageImpl<>(
                List.of(failedRequest)
            );

        when(
            requestRepository.findAllByStatus(
                org.mockito.ArgumentMatchers.eq(
                    RequestStatus.FAILED
                ),
                any(Pageable.class)
            )
        )
        .thenReturn(repositoryPage);

        PageResponseDto<RequestResponseDto> result =
            service.getAllRequests(
                RequestStatus.FAILED,
                1,
                10
            );

        verify(roleAuthorizationService)
            .requireAdmin();

        ArgumentCaptor<Pageable> pageableCaptor =
            ArgumentCaptor.forClass(
                Pageable.class
            );

        verify(requestRepository)
            .findAllByStatus(
                org.mockito.ArgumentMatchers.eq(
                    RequestStatus.FAILED
                ),
                pageableCaptor.capture()
            );

        assertThat(
            pageableCaptor
                .getValue()
                .getPageNumber()
        )
        .isEqualTo(1);

        assertThat(
            pageableCaptor
                .getValue()
                .getPageSize()
        )
        .isEqualTo(10);

        assertThat(result.content())
            .hasSize(1);

        assertThat(
            result.content()
                .getFirst()
                .status()
        )
        .isEqualTo(RequestStatus.FAILED);
    }

    @Test
    void shouldReturnRequestByIdForAdmin() {

        UUID requestId =
            UUID.randomUUID();

        RequestEntity entity =
            createRequest(
                requestId,
                RequestStatus.COMPLETED
            );

        when(
            requestRepository.findById(
                requestId
            )
        )
        .thenReturn(Optional.of(entity));

        RequestResponseDto result =
            service.getRequestById(
                requestId
            );

        verify(roleAuthorizationService)
            .requireAdmin();

        verify(requestRepository)
            .findById(requestId);

        assertThat(result.id())
            .isEqualTo(requestId);

        assertThat(result.status())
            .isEqualTo(
                RequestStatus.COMPLETED
            );

        assertThat(result.result())
            .isEqualTo(
                "Request processed successfully"
            );
    }

    @Test
    void shouldThrowWhenRequestDoesNotExist() {

        UUID requestId =
            UUID.randomUUID();

        when(
            requestRepository.findById(
                requestId
            )
        )
        .thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> service.getRequestById(
                requestId
            )
        )
        .isInstanceOf(
            RequestNotFoundException.class
        )
        .hasMessage(
            "Request not found with id: "
                + requestId
        );

        verify(roleAuthorizationService)
            .requireAdmin();

        verify(requestRepository)
            .findById(requestId);
    }

    @Test
    void shouldRejectNonAdminWhenListingRequests() {

        doThrow(
            new ForbiddenException(
                "Administrator privileges are required."
            )
        )
        .when(roleAuthorizationService)
        .requireAdmin();

        assertThatThrownBy(
            () -> service.getAllRequests(
                null,
                0,
                20
            )
        )
        .isInstanceOf(
            ForbiddenException.class
        )
        .hasMessage(
            "Administrator privileges are required."
        );

        verify(
            requestRepository,
            never()
        )
        .findAll(
            any(Pageable.class)
        );

        verify(
            requestRepository,
            never()
        )
        .findAllByStatus(
            any(),
            any(Pageable.class)
        );
    }

    @Test
    void shouldRejectNonAdminWhenReadingRequest() {

        UUID requestId =
            UUID.randomUUID();

        doThrow(
            new ForbiddenException(
                "Administrator privileges are required."
            )
        )
        .when(roleAuthorizationService)
        .requireAdmin();

        assertThatThrownBy(
            () -> service.getRequestById(
                requestId
            )
        )
        .isInstanceOf(
            ForbiddenException.class
        )
        .hasMessage(
            "Administrator privileges are required."
        );

        verify(
            requestRepository,
            never()
        )
        .findById(any());
    }

    private RequestEntity createRequest(
        UUID requestId,
        RequestStatus status
    ) {
        Instant now =
            Instant.now();

        boolean completed =
            status == RequestStatus.COMPLETED;

        boolean failed =
            status == RequestStatus.FAILED;

        return RequestEntity.builder()
            .id(requestId)
            .userId(UUID.randomUUID())
            .requestType("ADMIN_TEST")
            .payload(
                "{\"message\":\"admin test\"}"
            )
            .status(status)
            .result(
                completed
                    ? "Request processed successfully"
                    : null
            )
            .errorMessage(
                failed
                    ? "Request processing failed"
                    : null
            )
            .createdAt(
                now.minusSeconds(10)
            )
            .updatedAt(now)
            .completedAt(
                completed || failed
                    ? now
                    : null
            )
            .version(0L)
            .idempotencyKey(
                "admin-test-" + UUID.randomUUID()
            )
            .idempotencyFingerprint(
                "a".repeat(64)
            )
            .build();
    }
}