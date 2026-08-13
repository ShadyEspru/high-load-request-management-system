package com.hlrms.mobile.ui.history

data class TransferHistoryItem(

    val id: String,

    val direction: String,

    val recipientName: String,

    val recipientTransferId: String,

    val amount: String,

    val currency: String,

    val status: String,

    val createdAt: String?
)
