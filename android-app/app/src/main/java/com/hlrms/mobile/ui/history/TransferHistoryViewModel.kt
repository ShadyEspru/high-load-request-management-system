package com.hlrms.mobile.ui.history

import android.app.Application

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.hlrms.mobile.data.local.SessionManager
import com.hlrms.mobile.data.remote.TransferApiClient
import com.hlrms.mobile.data.remote.transfer.TransferHistoryResponse
import com.hlrms.mobile.data.repository.TransferHistoryRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TransferHistoryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        TransferHistoryRepository(

            transferApi =
                TransferApiClient
                    .transferApi,

            sessionManager =
                SessionManager(
                    application
                )
        )

    private val _uiState =
        MutableStateFlow(
            TransferHistoryUiState()
        )

    val uiState:
        StateFlow<TransferHistoryUiState> =
        _uiState.asStateFlow()

    fun loadHistory() {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    hasError = false
                )

            try {

                val transfers =
                    repository
                        .getTransfers()

                val items =
                    transfers.map(
                        ::toHistoryItem
                    )

                _uiState.value =
                    TransferHistoryUiState(
                        isLoading = false,
                        items = items,
                        hasError = false
                    )

            } catch (
                exception: Exception
            ) {

                _uiState.value =
                    TransferHistoryUiState(
                        isLoading = false,
                        items = emptyList(),
                        hasError = true
                    )
            }
        }
    }

    private fun toHistoryItem(
        transfer: TransferHistoryResponse
    ): TransferHistoryItem {

        return TransferHistoryItem(

            id =
                transfer.id,

            direction =
                transfer.direction
                    .uppercase(),

            recipientName =
                transfer.counterpartName,

            recipientTransferId =
                transfer.counterpartTransferId,

            amount =
                transfer.amount,

            currency =
                transfer.currency
                    .uppercase(),

            status =
                transfer.status
                    .uppercase(),

            createdAt =
                transfer.createdAt
        )
    }

    fun clear() {

        _uiState.value =
            TransferHistoryUiState()
    }

}
