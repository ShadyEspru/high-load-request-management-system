package com.hlrms.mobile.data.remote.request

data class RequestResponseDto(
    val id: String,
    val idempotencyKey: String,
    val requestType: String,
    val payload: String,
    val status: String,
    val result: String?,
    val errorMessage: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val completedAt: String?,
    val version: Long?
)