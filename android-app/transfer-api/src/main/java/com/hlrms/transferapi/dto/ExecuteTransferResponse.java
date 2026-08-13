package com.hlrms.transferapi.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExecuteTransferResponse(

    UUID transactionId,
    UUID requestId,

    UUID senderUserId,
    UUID recipientUserId,

    String senderTransferId,
    String recipientTransferId,

    BigDecimal amount,
    String currency,

    Instant createdAt,
    boolean alreadyProcessed

) {
}
