package com.hlrms.mobile.ui.transfer

import com.hlrms.mobile.data.remote.request.RequestResponseDto
import com.hlrms.mobile.data.remote.transfer.RecipientResponse

data class TransferUiState(

    val isSearchingRecipient: Boolean = false,

    val isSending: Boolean = false,

    val recipient: RecipientResponse? = null,

    val recipientErrorRes: Int? = null,

    val sendErrorRes: Int? = null,

    val createdRequest: RequestResponseDto? = null
)