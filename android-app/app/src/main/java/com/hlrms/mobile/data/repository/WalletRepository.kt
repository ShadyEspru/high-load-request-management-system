package com.hlrms.mobile.data.repository

import com.hlrms.mobile.data.local.SessionManager
import com.hlrms.mobile.data.remote.TransferApi
import com.hlrms.mobile.data.remote.wallet.WalletResponse
import kotlinx.coroutines.flow.first

class WalletRepository(
    private val transferApi: TransferApi,
    private val sessionManager: SessionManager
) {

    suspend fun getMyWallet():
            WalletResponse {

        return transferApi.getMyWallet(
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
