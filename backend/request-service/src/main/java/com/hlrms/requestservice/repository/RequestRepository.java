package com.hlrms.requestservice.repository;

import com.hlrms.requestservice.entity.RequestEntity;
import com.hlrms.requestservice.entity.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RequestRepository
    extends JpaRepository<RequestEntity, UUID> {

    Page<RequestEntity> findAllByStatus(
        RequestStatus status,
        Pageable pageable
    );

    Optional<RequestEntity> findByIdempotencyKey(
        String idempotencyKey
    );
}