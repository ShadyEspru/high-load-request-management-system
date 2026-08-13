package com.hlrms.transferapi.dto;

import java.math.BigDecimal;

public record WalletBalanceResponse(
    String currency,
    BigDecimal balance
) {
}