package com.hlrms.mobile.data.remote.dto

data class RegisterResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val createdAt: String
)