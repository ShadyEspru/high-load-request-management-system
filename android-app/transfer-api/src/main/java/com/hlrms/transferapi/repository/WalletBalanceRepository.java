package com.hlrms.transferapi.repository;

import com.hlrms.transferapi.entity.WalletBalanceEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletBalanceRepository
        extends JpaRepository<
            WalletBalanceEntity,
            Long
        > {

    List<WalletBalanceEntity>
        findByUserId(
            UUID userId
        );

    Optional<WalletBalanceEntity>
        findByUserIdAndCurrency(
            UUID userId,
            String currency
        );

    /*
     * سنستخدم هذا أثناء التحويل:
     * SELECT ... FOR UPDATE
     */
    @Lock(
        LockModeType.PESSIMISTIC_WRITE
    )
    @Query("""
        SELECT balance
        FROM WalletBalanceEntity balance
        WHERE balance.userId = :userId
          AND balance.currency = :currency
    """)
    Optional<WalletBalanceEntity>
        findForUpdate(
            @Param("userId")
            UUID userId,

            @Param("currency")
            String currency
        );

    /*
     * إنشاء الرصيد إذا لم يكن موجودًا.
     * ON CONFLICT يمنع إنشاء Wallet مكرر.
     */
    @Modifying
    @Query(
        value = """
            INSERT INTO wallet_balances (
                user_id,
                currency,
                balance,
                version,
                created_at,
                updated_at
            )
            VALUES (
                :userId,
                :currency,
                :balance,
                0,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            ON CONFLICT (
                user_id,
                currency
            )
            DO NOTHING
            """,

        nativeQuery = true
    )
    int ensureBalance(
        @Param("userId")
        UUID userId,

        @Param("currency")
        String currency,

        @Param("balance")
        BigDecimal balance
    );
}