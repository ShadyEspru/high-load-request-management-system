package com.hlrms.mobile.ui.transfer

import com.hlrms.mobile.data.remote.request.RequestResponseDto

data class TransferStatusUiState(
    val isLoading: Boolean = true,
    val request: RequestResponseDto? = null,
    val errorMessageRes: Int? = null
)