package com.hlrms.transferapi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ExecuteTransferRequest(

    @NotNull
    UUID requestId,

    @NotNull
    UUID senderUserId,

    @NotBlank
    String recipientTransferId,

    @NotNull
    @DecimalMin(
        value = "0.01"
    )
    BigDecimal amount,

    @NotBlank
    String currency

) {
}
