package com.hlrms.transferapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer_transactions")
public class TransferTransactionEntity {

    @Id
    private UUID id;

    @Column(
        name = "request_id",
        nullable = false,
        unique = true
    )
    private UUID requestId;

    @Column(
        name = "sender_user_id",
        nullable = false
    )
    private UUID senderUserId;

    @Column(
        name = "recipient_user_id",
        nullable = false
    )
    private UUID recipientUserId;

    @Column(
        name = "sender_transfer_id",
        nullable = false,
        length = 16
    )
    private String senderTransferId;

    @Column(
        name = "recipient_transfer_id",
        nullable = false,
        length = 16
    )
    private String recipientTransferId;

    @Column(
        name = "sender_display_name",
        nullable = false,
        length = 200
    )
    private String senderDisplayName;

    @Column(
        name = "recipient_display_name",
        nullable = false,
        length = 200
    )
    private String recipientDisplayName;

    @Column(
        nullable = false,
        precision = 20,
        scale = 2
    )
    private BigDecimal amount;

    @Column(
        nullable = false,
        length = 3
    )
    private String currency;

    @Column(
        name = "created_at",
        nullable = false
    )
    private Instant createdAt;

    protected TransferTransactionEntity() {
    }

    public TransferTransactionEntity(
            UUID id,
            UUID requestId,
            UUID senderUserId,
            UUID recipientUserId,
            String senderTransferId,
            String recipientTransferId,
            String senderDisplayName,
            String recipientDisplayName,
            BigDecimal amount,
            String currency,
            Instant createdAt
    ) {
        this.id = id;
        this.requestId = requestId;
        this.senderUserId = senderUserId;
        this.recipientUserId = recipientUserId;
        this.senderTransferId = senderTransferId;
        this.recipientTransferId = recipientTransferId;
        this.senderDisplayName = senderDisplayName;
        this.recipientDisplayName = recipientDisplayName;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public UUID getSenderUserId() {
        return senderUserId;
    }

    public UUID getRecipientUserId() {
        return recipientUserId;
    }

    public String getSenderTransferId() {
        return senderTransferId;
    }

    public String getRecipientTransferId() {
        return recipientTransferId;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    public String getRecipientDisplayName() {
        return recipientDisplayName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
