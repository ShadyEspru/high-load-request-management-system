package com.hlrms.mobile.data.remote.request

data class TransferPayload(
    val recipientTransferId: String,
    val recipientName: String,
    val amount: Double,
    val currency: String
)