package com.hlrms.transferapi.dto;

import java.util.List;

public record WalletResponse(
    List<WalletBalanceResponse> balances
) {
}