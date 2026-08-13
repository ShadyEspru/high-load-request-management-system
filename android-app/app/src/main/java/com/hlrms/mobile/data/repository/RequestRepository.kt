package com.hlrms.mobile.data.repository

import com.google.gson.Gson
import com.hlrms.mobile.data.local.SessionManager
import com.hlrms.mobile.data.remote.RequestApi
import com.hlrms.mobile.data.remote.request.CreateRequestDto
import com.hlrms.mobile.data.remote.request.RequestResponseDto
import com.hlrms.mobile.data.remote.request.TransferPayload
import kotlinx.coroutines.flow.first
import java.util.UUID

class RequestRepository(
    private val requestApi: RequestApi,
    private val sessionManager: SessionManager
) {

    private val gson = Gson()

    suspend fun createTransfer(
        recipientTransferId: String,
        recipientName: String,
        amount: Double,
        currency: String
    ): RequestResponseDto {

        val accessToken =
            sessionManager.accessToken.first()
                ?: throw IllegalStateException(
                    "No active session"
                )

        val transferPayload =
            TransferPayload(
                recipientTransferId =
                    recipientTransferId
                        .trim()
                        .uppercase(),

                recipientName =
                    recipientName.trim(),

                amount = amount,

                currency =
                    currency.uppercase()
            )

        val payloadJson =
            gson.toJson(transferPayload)

        val request =
            CreateRequestDto(
                requestType = "MONEY_TRANSFER",
                payload = payloadJson
            )

        val idempotencyKey =
            UUID.randomUUID().toString()

        return requestApi.createRequest(
            authorization =
                "Bearer $accessToken",

            idempotencyKey =
                idempotencyKey,

            request =
                request
        )
    }

    suspend fun getRequestById(
        requestId: String
    ): RequestResponseDto {

        val accessToken =
            sessionManager.accessToken.first()
                ?: throw IllegalStateException(
                    "No active session"
                )

        return requestApi.getRequestById(
            authorization =
                "Bearer $accessToken",

            requestId =
                requestId
        )
    }
}