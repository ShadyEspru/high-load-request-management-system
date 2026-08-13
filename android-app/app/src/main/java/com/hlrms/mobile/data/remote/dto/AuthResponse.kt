package com.hlrms.mobile.data.remote.dto

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String
)