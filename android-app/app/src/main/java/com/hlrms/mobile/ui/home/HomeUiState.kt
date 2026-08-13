package com.hlrms.mobile.ui.home

import java.math.BigDecimal

data class HomeUiState(
    val isWalletLoading: Boolean = false,
    val balances: Map<String, BigDecimal> = emptyMap(),
    val walletErrorMessage: String? = null
)
