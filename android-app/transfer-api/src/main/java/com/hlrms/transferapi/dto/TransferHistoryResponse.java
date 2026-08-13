package com.hlrms.transferapi.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferHistoryResponse(
    UUID id,
    UUID requestId,
    String direction,
    String counterpartName,
    String counterpartTransferId,
    BigDecimal amount,
    String currency,
    String status,
    Instant createdAt
) {
}
