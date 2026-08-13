package com.hlrms.mobile.data.repository

import com.hlrms.mobile.data.local.SessionManager
import com.hlrms.mobile.data.remote.TransferApi
import com.hlrms.mobile.data.remote.transfer.CreateTransferProfileRequest
import com.hlrms.mobile.data.remote.transfer.RecipientResponse
import com.hlrms.mobile.data.remote.transfer.TransferProfileResponse
import kotlinx.coroutines.flow.first

class TransferProfileRepository(
    private val transferApi: TransferApi,
    private val sessionManager: SessionManager
) {

    suspend fun createProfile(
        displayName: String
    ): TransferProfileResponse {

        return transferApi.createProfile(
            authorization = bearerToken(),
            request =
                CreateTransferProfileRequest(
                    displayName =
                        displayName.trim()
                )
        )
    }

    suspend fun getMyProfile():
            TransferProfileResponse {

        return transferApi.getMyProfile(
            authorization = bearerToken()
        )
    }

    suspend fun findRecipient(
        transferId: String
    ): RecipientResponse {

        return transferApi.findRecipient(
            transferId =
                transferId
                    .trim()
                    .uppercase()
        )
    }

    private suspend fun bearerToken():
            String {

        val token =
            sessionManager.accessToken.first()
                ?: throw IllegalStateException(
                    "No active session"
                )

        return "Bearer $token"
    }
}