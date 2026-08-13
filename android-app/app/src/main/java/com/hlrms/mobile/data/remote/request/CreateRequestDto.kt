package com.hlrms.mobile.data.remote.request

data class CreateRequestDto(
    val requestType: String,
    val payload: String
)