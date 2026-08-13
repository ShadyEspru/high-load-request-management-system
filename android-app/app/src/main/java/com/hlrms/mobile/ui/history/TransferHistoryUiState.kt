package com.hlrms.mobile.ui.history

data class TransferHistoryUiState(

    val isLoading: Boolean = true,

    val items:
        List<TransferHistoryItem> =
        emptyList(),

    val hasError: Boolean = false
)
