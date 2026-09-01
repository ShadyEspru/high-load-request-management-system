package com.hlrms.mobile.data.remote

import com.hlrms.mobile.data.remote.transfer.TransferHistoryResponse

import com.hlrms.mobile.data.remote.exchange.ExchangeRatesResponse

import com.hlrms.mobile.data.remote.transfer.CreateTransferProfileRequest
import com.hlrms.mobile.data.remote.transfer.RecipientResponse
import com.hlrms.mobile.data.remote.transfer.TransferProfileResponse
import com.hlrms.mobile.data.remote.wallet.WalletResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface TransferApi {

    @POST("api/v1/transfer/profile")
    suspend fun createProfile(
        @Header("Authorization")
        authorization: String,

        @Body
        request: CreateTransferProfileRequest
    ): TransferProfileResponse

    @GET("api/v1/transfer/me")
    suspend fun getMyProfile(
        @Header("Authorization")
        authorization: String
    ): TransferProfileResponse

    @GET("api/v1/transfer/recipients/{transferId}")
    suspend fun findRecipient(
        @Path("transferId")
        transferId: String
    ): RecipientResponse

    @GET("api/v1/wallet/me")
    suspend fun getMyWallet(
        @Header("Authorization")
        authorization: String
    ): WalletResponse

    @GET("api/v1/exchange-rates")
    suspend fun getExchangeRates():
        ExchangeRatesResponse


    @retrofit2.http.GET("api/v1/transfer/history")
    suspend fun getTransferHistory(
        @retrofit2.http.Header("Authorization")
        authorization: String
    ): List<TransferHistoryResponse>

}
