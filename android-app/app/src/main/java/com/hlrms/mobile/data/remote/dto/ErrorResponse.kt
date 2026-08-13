package com.hlrms.mobile.data.remote.dto

data class ErrorResponse(
    val timestamp: String?,
    val status: Int?,
    val error: String?,
    val message: String?,
    val path: String?,
    val validationErrors: List<String>?
)