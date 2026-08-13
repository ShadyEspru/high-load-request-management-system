package com.hlrms.mobile.data.repository

import com.hlrms.mobile.data.local.SessionManager
import com.hlrms.mobile.data.remote.AuthApi
import com.hlrms.mobile.data.remote.dto.AuthResponse
import com.hlrms.mobile.data.remote.dto.LoginRequest
import com.hlrms.mobile.data.remote.dto.RegisterRequest
import com.hlrms.mobile.data.remote.dto.RegisterResponse
import com.hlrms.mobile.data.remote.dto.RefreshTokenRequest
import kotlinx.coroutines.flow.first

class AuthRepository(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager
) {

    suspend fun login(
        email: String,
        password: String
    ): AuthResponse {

        val response =
            authApi.login(
                LoginRequest(
                    email = email,
                    password = password
                )
            )

        sessionManager.saveSession(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken
        )

        return response
    }

    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): RegisterResponse {

        return authApi.register(
            RegisterRequest(
                email = email,
                password = password,
                firstName = firstName,
                lastName = lastName
            )
        )
    }

    suspend fun logout() {
        sessionManager.clearSession()
    }

    suspend fun refreshSession(): AuthResponse {

        val refreshToken =
            sessionManager.refreshToken.first()
                ?: throw IllegalStateException(
                    "No refresh token"
                )

        val response =
            authApi.refresh(
                RefreshTokenRequest(
                    refreshToken = refreshToken
                )
            )

        sessionManager.updateSession(
            accessToken =
                response.accessToken,

            refreshToken =
                response.refreshToken
        )

        return response
    }
}