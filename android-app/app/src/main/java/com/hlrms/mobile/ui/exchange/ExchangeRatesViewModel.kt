package com.hlrms.mobile.ui.exchange

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hlrms.mobile.data.remote.TransferApiClient
import com.hlrms.mobile.data.repository.ExchangeRatesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExchangeRatesViewModel :
    ViewModel() {

    private val repository =
        ExchangeRatesRepository(
            transferApi =
                TransferApiClient.transferApi
        )

    private val _uiState =
        MutableStateFlow(
            ExchangeRatesUiState()
        )

    val uiState:
        StateFlow<ExchangeRatesUiState> =
        _uiState.asStateFlow()

    fun loadRates() {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    hasError = false
                )

            try {

                val response =
                    repository
                        .getExchangeRates()

                _uiState.value =
                    ExchangeRatesUiState(
                        isLoading = false,
                        response = response,
                        hasError = false
                    )

            } catch (
                exception: Exception
            ) {

                _uiState.value =
                    ExchangeRatesUiState(
                        isLoading = false,
                        response = null,
                        hasError = true
                    )
            }
        }
    }
}
