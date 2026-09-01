package com.hlrms.transferapi.repository;

import com.hlrms.transferapi.entity.TransferTransactionEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransferTransactionRepository
        extends JpaRepository<
            TransferTransactionEntity,
            UUID
        > {

    Optional<TransferTransactionEntity>
        findByRequestId(
            UUID requestId
        );

    @Query("""
        SELECT transaction
        FROM TransferTransactionEntity transaction
        WHERE transaction.senderUserId = :userId
           OR transaction.recipientUserId = :userId
        ORDER BY transaction.createdAt DESC
    """)
    List<TransferTransactionEntity>
        findHistoryForUser(
            @Param("userId")
            UUID userId
        );
}
