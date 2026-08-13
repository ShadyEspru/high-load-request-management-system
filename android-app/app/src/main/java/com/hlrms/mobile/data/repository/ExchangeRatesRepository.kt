package com.hlrms.mobile.data.repository

import com.hlrms.mobile.data.remote.TransferApi
import com.hlrms.mobile.data.remote.exchange.ExchangeRatesResponse

class ExchangeRatesRepository(

    private val transferApi: TransferApi

) {

    suspend fun getExchangeRates():
        ExchangeRatesResponse {

        return transferApi
            .getExchangeRates()
    }
}
