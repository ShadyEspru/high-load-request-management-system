package com.hlrms.mobile.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hlrms.mobile.data.local.SessionManager
import com.hlrms.mobile.data.remote.TransferApiClient
import com.hlrms.mobile.data.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        WalletRepository(
            transferApi =
                TransferApiClient.transferApi,
            sessionManager =
                SessionManager(application)
        )

    private val _uiState =
        MutableStateFlow(
            HomeUiState()
        )

    val uiState: StateFlow<HomeUiState> =
        _uiState.asStateFlow()

    fun loadWallet() {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isWalletLoading = true,
                    walletErrorMessage = null
                )

            try {

                val wallet =
                    repository.getMyWallet()

                val balances =
                    wallet.balances
                        .associate {
                            balance ->

                            balance.currency.uppercase() to
                                    balance.balance
                        }

                _uiState.value =
                    HomeUiState(
                        balances = balances
                    )

            } catch (exception: IOException) {

                _uiState.value =
                    _uiState.value.copy(
                        isWalletLoading = false,
                        walletErrorMessage =
                            "تعذر الاتصال بالإنترنت"
                    )

            } catch (exception: HttpException) {

                _uiState.value =
                    _uiState.value.copy(
                        isWalletLoading = false,
                        walletErrorMessage =
                            "تعذر تحميل الرصيد"
                    )

            } catch (exception: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isWalletLoading = false,
                        walletErrorMessage =
                            "تعذر تحميل الرصيد"
                    )
            }
        }
    }

    fun clear() {

        _uiState.value =
            HomeUiState()
    }
}
