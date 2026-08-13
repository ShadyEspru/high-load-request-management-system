package com.hlrms.mobile.ui.receive

import com.hlrms.mobile.data.remote.transfer.TransferProfileResponse

data class ReceiveUiState(
    val isLoading: Boolean = false,
    val profile: TransferProfileResponse? = null,
    val errorMessage: String? = null
)