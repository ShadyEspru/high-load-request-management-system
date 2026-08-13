package com.hlrms.transferapi.repository;

import com.hlrms.transferapi.entity.TransferProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransferProfileRepository
        extends JpaRepository<
            TransferProfileEntity,
            UUID
        > {

    Optional<TransferProfileEntity>
    findByUserId(UUID userId);

    Optional<TransferProfileEntity>
    findByTransferId(String transferId);

    boolean existsByTransferId(String transferId);
}