package com.hlrms.mobile.ui.exchange

import com.hlrms.mobile.data.remote.exchange.ExchangeRatesResponse

data class ExchangeRatesUiState(

    val isLoading: Boolean = true,

    val response:
        ExchangeRatesResponse? = null,

    val hasError: Boolean = false
)
