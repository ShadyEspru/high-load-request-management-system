package com.hlrms.mobile.data.remote.wallet

import java.math.BigDecimal

data class WalletBalanceResponse(
    val currency: String,
    val balance: BigDecimal
)
