package com.hlrms.mobile.data.remote.exchange

import java.math.BigDecimal

data class ExchangeRatesResponse(

    val updatedAt: String,

    val nextUpdateAt: String,

    val provider: String,

    val rates:
        Map<String, Map<String, BigDecimal>>
)
