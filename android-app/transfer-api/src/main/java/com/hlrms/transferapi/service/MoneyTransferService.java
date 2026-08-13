package com.hlrms.transferapi.service;

import com.hlrms.transferapi.exception.TransferBusinessException;
import com.hlrms.transferapi.dto.ExecuteTransferRequest;
import com.hlrms.transferapi.dto.ExecuteTransferResponse;
import com.hlrms.transferapi.entity.TransferProfileEntity;
import com.hlrms.transferapi.entity.TransferTransactionEntity;
import com.hlrms.transferapi.entity.WalletBalanceEntity;
import com.hlrms.transferapi.repository.TransferProfileRepository;
import com.hlrms.transferapi.repository.TransferTransactionRepository;
import com.hlrms.transferapi.repository.WalletBalanceRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MoneyTransferService {

    private static final Map<String, BigDecimal>
        INITIAL_BALANCES =
            Map.of(
                "USD", new BigDecimal("14280.00"),
                "EUR", new BigDecimal("8260.50"),
                "TRY", new BigDecimal("186750.00"),
                "SYP", new BigDecimal("250000.00")
            );

    private final TransferProfileRepository
        profileRepository;

    private final WalletBalanceRepository
        walletRepository;

    private final TransferTransactionRepository
        transactionRepository;

    public MoneyTransferService(
            TransferProfileRepository profileRepository,
            WalletBalanceRepository walletRepository,
            TransferTransactionRepository transactionRepository
    ) {
        this.profileRepository =
            profileRepository;

        this.walletRepository =
            walletRepository;

        this.transactionRepository =
            transactionRepository;
    }

    @Transactional
    public ExecuteTransferResponse execute(
            ExecuteTransferRequest request
    ) {

        var existing =
            transactionRepository
                .findByRequestId(
                    request.requestId()
                );

        if (existing.isPresent()) {
            return toResponse(
                existing.get(),
                true
            );
        }

        String currency =
            request.currency()
                .trim()
                .toUpperCase();

        BigDecimal initialBalance =
            INITIAL_BALANCES.get(
                currency
            );

        if (initialBalance == null) {
            throw new IllegalArgumentException(
                "Unsupported currency: " +
                currency
            );
        }

        BigDecimal amount =
            request.amount()
                .setScale(
                    2,
                    java.math.RoundingMode.UNNECESSARY
                );

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException(
                "Amount must be positive"
            );
        }

        TransferProfileEntity sender =
            profileRepository
                .findByUserId(
                    request.senderUserId()
                )
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Sender profile not found"
                        )
                );

        TransferProfileEntity recipient =
            profileRepository
                .findByTransferId(
                    request.recipientTransferId()
                        .trim()
                        .toUpperCase()
                )
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Recipient not found"
                        )
                );

        if (
            sender.getUserId()
                .equals(
                    recipient.getUserId()
                )
        ) {
            throw new IllegalArgumentException(
                "Cannot transfer money to yourself"
            );
        }

        /*
         * نتأكد من وجود Wallet للعملة عند الطرفين.
         */
        walletRepository.ensureBalance(
            sender.getUserId(),
            currency,
            initialBalance
        );

        walletRepository.ensureBalance(
            recipient.getUserId(),
            currency,
            initialBalance
        );

        /*
         * نقفل السجلين بترتيب ثابت
         * لتقليل احتمال Deadlock عندما تتم
         * حوالتان متعاكستان في نفس اللحظة.
         */
        List<UUID> users =
            List.of(
                sender.getUserId(),
                recipient.getUserId()
            )
            .stream()
            .sorted(
                Comparator.comparing(
                    UUID::toString
                )
            )
            .toList();

        WalletBalanceEntity first =
            walletRepository
                .findForUpdate(
                    users.get(0),
                    currency
                )
                .orElseThrow();

        WalletBalanceEntity second =
            walletRepository
                .findForUpdate(
                    users.get(1),
                    currency
                )
                .orElseThrow();

        WalletBalanceEntity senderWallet =
            first.getUserId()
                .equals(
                    sender.getUserId()
                )
                ? first
                : second;

        WalletBalanceEntity recipientWallet =
            first.getUserId()
                .equals(
                    recipient.getUserId()
                )
                ? first
                : second;

        Instant now =
            Instant.now();

        if (
            senderWallet.getBalance().signum() == 0
        ) {
            throw new TransferBusinessException(
                "ZERO_BALANCE",
                "رصيدك صفر ولا يمكنك تنفيذ هذه الحوالة"
            );
        }

        if (
            senderWallet.getBalance()
                .compareTo(amount) < 0
        ) {
            throw new TransferBusinessException(
                "INSUFFICIENT_BALANCE",
                "الرصيد غير كافٍ لإتمام الحوالة. " +
                "رصيدك الحالي " +
                senderWallet.getBalance().toPlainString() +
                " " +
                currency
            );
        }

        senderWallet.debit(
            amount,
            now
        );

        recipientWallet.credit(
            amount,
            now
        );

        TransferTransactionEntity transaction =
            new TransferTransactionEntity(
                UUID.randomUUID(),
                request.requestId(),

                sender.getUserId(),
                recipient.getUserId(),

                sender.getTransferId(),
                recipient.getTransferId(),

                sender.getDisplayName(),
                recipient.getDisplayName(),

                amount,
                currency,
                now
            );

        TransferTransactionEntity saved =
            transactionRepository.save(
                transaction
            );

        return toResponse(
            saved,
            false
        );
    }

    private ExecuteTransferResponse toResponse(
            TransferTransactionEntity entity,
            boolean alreadyProcessed
    ) {

        return new ExecuteTransferResponse(
            entity.getId(),
            entity.getRequestId(),

            entity.getSenderUserId(),
            entity.getRecipientUserId(),

            entity.getSenderTransferId(),
            entity.getRecipientTransferId(),

            entity.getAmount(),
            entity.getCurrency(),

            entity.getCreatedAt(),
            alreadyProcessed
        );
    }
}
