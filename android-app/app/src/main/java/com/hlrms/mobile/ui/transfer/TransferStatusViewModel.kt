package com.hlrms.mobile.ui.transfer

import android.app.Application
import com.hlrms.mobile.R
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hlrms.mobile.data.local.SessionManager
import com.hlrms.mobile.data.remote.ApiClient
import com.hlrms.mobile.data.repository.RequestRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class TransferStatusViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val sessionManager =
        SessionManager(application)

    private val repository =
        RequestRepository(
            requestApi =
                ApiClient.requestApi,

            sessionManager =
                sessionManager
        )

    private val _uiState =
        MutableStateFlow(
            TransferStatusUiState()
        )

    val uiState: StateFlow<TransferStatusUiState> =
        _uiState.asStateFlow()

    private var currentRequestId:
            String? = null

    fun startMonitoring(
        requestId: String
    ) {

        if (currentRequestId == requestId) {
            return
        }

        currentRequestId =
            requestId

        viewModelScope.launch {

            _uiState.value =
                TransferStatusUiState(
                    isLoading = true
                )

            while (isActive) {

                try {

                    val request =
                        repository.getRequestById(
                            requestId
                        )

                    _uiState.value =
                        TransferStatusUiState(
                            isLoading = false,
                            request = request,
                            errorMessageRes = null
                        )

                    val status =
                        request.status.uppercase()

                    if (
                        status == "COMPLETED" ||
                        status == "FAILED"
                    ) {
                        break
                    }

                    delay(1000)

                } catch (exception: HttpException) {

                    _uiState.value =
                        TransferStatusUiState(
                            isLoading = false,
                            errorMessageRes =
                                when (exception.code()) {
                                    404 ->
                                        R.string.request_not_found

                                    else ->
                                        R.string.request_status_read_failed
                                }
                        )

                    break

                } catch (exception: IOException) {

                    _uiState.value =
                        TransferStatusUiState(
                            isLoading = false,
                            errorMessageRes =
                                R.string.server_unreachable
                        )

                    break

                } catch (exception: Exception) {

                    _uiState.value =
                        TransferStatusUiState(
                            isLoading = false,
                            errorMessageRes =
                                R.string.request_tracking_error
                        )

                    break
                }
            }
        }
    }

    fun retry() {

        val requestId =
            currentRequestId
                ?: return

        currentRequestId = null

        startMonitoring(
            requestId
        )
    }
}