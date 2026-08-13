package com.hlrms.mobile.data.repository

import com.hlrms.mobile.data.local.SessionManager
import com.hlrms.mobile.data.remote.TransferApi
import com.hlrms.mobile.data.remote.transfer.TransferHistoryResponse

import kotlinx.coroutines.flow.first

class TransferHistoryRepository(

    private val transferApi: TransferApi,

    private val sessionManager: SessionManager

) {

    suspend fun getTransfers():
            List<TransferHistoryResponse> {

        return transferApi
            .getTransferHistory(
                authorization =
                    bearerToken()
            )
    }

    private suspend fun bearerToken():
            String {

        val token =
            sessionManager
                .accessToken
                .first()
                ?: throw IllegalStateException(
                    "No active session"
                )

        return "Bearer $token"
    }
}
