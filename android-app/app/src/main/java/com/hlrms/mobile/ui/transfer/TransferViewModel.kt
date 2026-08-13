package com.hlrms.mobile.ui.transfer

import android.app.Application
import com.hlrms.mobile.R
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hlrms.mobile.data.local.SessionManager
import com.hlrms.mobile.data.remote.ApiClient
import com.hlrms.mobile.data.remote.TransferApiClient
import com.hlrms.mobile.data.repository.RequestRepository
import com.hlrms.mobile.data.repository.TransferProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class TransferViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val sessionManager =
        SessionManager(application)

    private val requestRepository =
        RequestRepository(
            requestApi =
                ApiClient.requestApi,

            sessionManager =
                sessionManager
        )

    private val transferProfileRepository =
        TransferProfileRepository(
            transferApi =
                TransferApiClient.transferApi,

            sessionManager =
                sessionManager
        )

    private val _uiState =
        MutableStateFlow(
            TransferUiState()
        )

    val uiState: StateFlow<TransferUiState> =
        _uiState.asStateFlow()

    fun findRecipient(
        transferId: String
    ) {

        val normalizedId =
            transferId
                .trim()
                .uppercase()

        if (normalizedId.length != 16) {

            _uiState.value =
                _uiState.value.copy(
                    recipient = null,
                    recipientErrorRes =
                        R.string.transfer_id_length_error
                )

            return
        }

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isSearchingRecipient = true,
                    recipient = null,
                    recipientErrorRes = null,
                    sendErrorRes = null
                )

            try {

                val recipient =
                    transferProfileRepository
                        .findRecipient(
                            normalizedId
                        )

                _uiState.value =
                    _uiState.value.copy(
                        isSearchingRecipient = false,
                        recipient = recipient,
                        recipientErrorRes = null
                    )

            } catch (exception: HttpException) {

                _uiState.value =
                    _uiState.value.copy(
                        isSearchingRecipient = false,
                        recipient = null,
                        recipientErrorRes =
                            when (exception.code()) {

                                404 ->
                                    R.string.recipient_not_found

                                else ->
                                    R.string.recipient_verification_failed
                            }
                    )

            } catch (exception: IOException) {

                _uiState.value =
                    _uiState.value.copy(
                        isSearchingRecipient = false,
                        recipient = null,
                        recipientErrorRes =
                            R.string.transfer_service_unreachable
                    )

            } catch (exception: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isSearchingRecipient = false,
                        recipient = null,
                        recipientErrorRes =
                            R.string.recipient_verification_failed
                    )
            }
        }
    }

    fun sendTransfer(
        amount: Double,
        currency: String
    ) {

        val recipient =
            _uiState.value.recipient
                ?: return

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isSending = true,
                    sendErrorRes = null
                )

            try {

                val request =
                    requestRepository.createTransfer(
                        recipientTransferId =
                            recipient.transferId,

                        recipientName =
                            recipient.displayName,

                        amount =
                            amount,

                        currency =
                            currency
                    )

                _uiState.value =
                    _uiState.value.copy(
                        isSending = false,
                        createdRequest = request,
                        sendErrorRes = null
                    )

            } catch (exception: HttpException) {

                _uiState.value =
                    _uiState.value.copy(
                        isSending = false,

                        sendErrorRes =
                            when (exception.code()) {

                                401 ->
                                    R.string.session_unauthorized

                                429 ->
                                    R.string.rate_limit_error

                                503 ->
                                    R.string.service_temporarily_unavailable

                                else ->
                                    R.string.transfer_send_failed
                            }
                    )

            } catch (exception: IOException) {

                _uiState.value =
                    _uiState.value.copy(
                        isSending = false,
                        sendErrorRes =
                            R.string.server_unreachable
                    )

            } catch (exception: Exception) {

                _uiState.value =
                    _uiState.value.copy(
                        isSending = false,
                        sendErrorRes =
                            R.string.unexpected_error
                    )
            }
        }
    }

    fun clearRecipient() {

        _uiState.value =
            _uiState.value.copy(
                recipient = null,
                recipientErrorRes = null,
                sendErrorRes = null
            )
    }

    fun clearState() {

        _uiState.value =
            TransferUiState()
    }
}