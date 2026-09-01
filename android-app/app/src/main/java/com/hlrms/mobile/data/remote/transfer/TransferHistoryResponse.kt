package com.hlrms.mobile.data.remote.transfer

data class TransferHistoryResponse(

    val id: String,

    val requestId: String,

    val direction: String,

    val counterpartName: String,

    val counterpartTransferId: String,

    val amount: String,

    val currency: String,

    val status: String,

    val createdAt: String?
)
