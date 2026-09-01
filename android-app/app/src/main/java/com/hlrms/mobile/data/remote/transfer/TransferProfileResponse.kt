package com.hlrms.mobile.data.remote.transfer

data class TransferProfileResponse(
    val transferId: String,
    val displayName: String,
    val qrContent: String
)