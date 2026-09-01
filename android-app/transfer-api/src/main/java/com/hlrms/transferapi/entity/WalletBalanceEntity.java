package com.hlrms.transferapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "wallet_balances",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_wallet_balances_user_currency",
            columnNames = {
                "user_id",
                "currency"
            }
        )
    }
)
public class WalletBalanceEntity {

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
        name = "user_id",
        nullable = false
    )
    private UUID userId;

    @Column(
        nullable = false,
        length = 3
    )
    private String currency;

    @Column(
        nullable = false,
        precision = 20,
        scale = 2
    )
    private BigDecimal balance;

    @Version
    @Column(
        nullable = false
    )
    private Long version;

    @Column(
        name = "created_at",
        nullable = false
    )
    private Instant createdAt;

    @Column(
        name = "updated_at",
        nullable = false
    )
    private Instant updatedAt;

    protected WalletBalanceEntity() {
    }

    public Long getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /*
     * سنستخدمهما في الدفعة التالية
     * عند تنفيذ التحويل الحقيقي.
     */
    public void debit(
            BigDecimal amount,
            Instant now
    ) {

        if (
            amount == null ||
            amount.signum() <= 0
        ) {
            throw new IllegalArgumentException(
                "Debit amount must be positive"
            );
        }

        if (
            balance.compareTo(
                amount
            ) < 0
        ) {
            throw new IllegalStateException(
                "Insufficient balance"
            );
        }

        balance =
            balance.subtract(
                amount
            );

        updatedAt =
            now;
    }

    public void credit(
            BigDecimal amount,
            Instant now
    ) {

        if (
            amount == null ||
            amount.signum() <= 0
        ) {
            throw new IllegalArgumentException(
                "Credit amount must be positive"
            );
        }

        balance =
            balance.add(
                amount
            );

        updatedAt =
            now;
    }
}