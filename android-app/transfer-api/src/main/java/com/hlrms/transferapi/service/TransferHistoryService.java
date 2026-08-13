package com.hlrms.transferapi.service;

import com.hlrms.transferapi.dto.TransferHistoryResponse;
import com.hlrms.transferapi.entity.TransferTransactionEntity;
import com.hlrms.transferapi.repository.TransferTransactionRepository;
import com.hlrms.transferapi.security.CurrentUser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransferHistoryService {

    private final TransferTransactionRepository
        transactionRepository;

    public TransferHistoryService(
            TransferTransactionRepository transactionRepository
    ) {
        this.transactionRepository =
            transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<TransferHistoryResponse> getHistory(
            CurrentUser user
    ) {

        return transactionRepository
            .findHistoryForUser(
                user.id()
            )
            .stream()
            .limit(100)
            .map(
                transaction ->
                    toResponse(
                        transaction,
                        user
                    )
            )
            .toList();
    }

    private TransferHistoryResponse toResponse(
            TransferTransactionEntity transaction,
            CurrentUser user
    ) {

        boolean outgoing =
            transaction
                .getSenderUserId()
                .equals(
                    user.id()
                );

        return new TransferHistoryResponse(
            transaction.getId(),
            transaction.getRequestId(),

            outgoing
                ? "OUTGOING"
                : "INCOMING",

            outgoing
                ? transaction.getRecipientDisplayName()
                : transaction.getSenderDisplayName(),

            outgoing
                ? transaction.getRecipientTransferId()
                : transaction.getSenderTransferId(),

            transaction.getAmount(),
            transaction.getCurrency(),
            "COMPLETED",
            transaction.getCreatedAt()
        );
    }
}
