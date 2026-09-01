package com.hlrms.transferapi.service;

import com.hlrms.transferapi.dto.WalletBalanceResponse;
import com.hlrms.transferapi.dto.WalletResponse;
import com.hlrms.transferapi.entity.WalletBalanceEntity;
import com.hlrms.transferapi.repository.WalletBalanceRepository;
import com.hlrms.transferapi.security.CurrentUser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class WalletService {

    private static final Map<
        String,
        BigDecimal
    > INITIAL_BALANCES =
        Map.of(
            "USD",
            new BigDecimal("14280.00"),

            "EUR",
            new BigDecimal("8260.50"),

            "TRY",
            new BigDecimal("186750.00"),

            "SYP",
            new BigDecimal("250000.00")
        );

    private final WalletBalanceRepository
        walletRepository;

    /*
     * نستعمل TransferProfileService هنا حتى
     * نضمن أن الحساب القديم لديه Transfer Profile
     * ومعرف دائم قبل إنشاء Wallet.
     */
    private final TransferProfileService
        transferProfileService;

    public WalletService(
            WalletBalanceRepository walletRepository,
            TransferProfileService transferProfileService
    ) {

        this.walletRepository =
            walletRepository;

        this.transferProfileService =
            transferProfileService;
    }

    @Transactional
    public WalletResponse getMine(
            CurrentUser user
    ) {

        /*
         * لا توجد أي شاشة إعداد للمستخدم.
         * إذا كان الحساب قديمًا بدون Profile،
         * getMine ينشئه تلقائيًا.
         */
        transferProfileService
            .getMine(
                user
            );

        ensureWallet(
            user
        );

        List<WalletBalanceResponse> balances =
            walletRepository
                .findByUserId(
                    user.id()
                )
                .stream()
                .sorted(
                    Comparator.comparingInt(
                        balance ->
                            currencyOrder(
                                balance
                                    .getCurrency()
                            )
                    )
                )
                .map(
                    this::toResponse
                )
                .toList();

        return new WalletResponse(
            balances
        );
    }

    private void ensureWallet(
            CurrentUser user
    ) {

        INITIAL_BALANCES
            .forEach(
                (currency, balance) ->

                    walletRepository
                        .ensureBalance(
                            user.id(),
                            currency,
                            balance
                        )
            );
    }

    private WalletBalanceResponse toResponse(
            WalletBalanceEntity entity
    ) {

        return new WalletBalanceResponse(
            entity.getCurrency(),
            entity.getBalance()
        );
    }

    private int currencyOrder(
            String currency
    ) {

        return switch (
            currency
        ) {

            case "USD" -> 0;

            case "EUR" -> 1;

            case "TRY" -> 2;

            case "SYP" -> 3;

            default -> 99;
        };
    }
}