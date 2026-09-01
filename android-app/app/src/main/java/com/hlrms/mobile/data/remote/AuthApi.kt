package com.hlrms.mobile.data.remote

import com.hlrms.mobile.data.remote.dto.AuthResponse
import com.hlrms.mobile.data.remote.dto.LoginRequest
import com.hlrms.mobile.data.remote.dto.RefreshTokenRequest
import com.hlrms.mobile.data.remote.dto.RegisterRequest
import com.hlrms.mobile.data.remote.dto.RegisterResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): AuthResponse

    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): RegisterResponse

    @POST("api/v1/auth/refresh")
    suspend fun refresh(
        @Body request: RefreshTokenRequest
    ): AuthResponse
}