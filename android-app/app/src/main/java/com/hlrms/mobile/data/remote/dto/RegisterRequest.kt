package com.hlrms.mobile.data.remote.dto

data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String
)