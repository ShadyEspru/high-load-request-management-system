package com.hlrms.mobile.ui.receive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hlrms.mobile.data.local.SessionManager
import com.hlrms.mobile.data.remote.TransferApiClient
import com.hlrms.mobile.data.repository.TransferProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class ReceiveViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        TransferProfileRepository(
            transferApi =
                TransferApiClient.transferApi,

            sessionManager =
                SessionManager(application)
        )

    private val _uiState =
        MutableStateFlow(
            ReceiveUiState()
        )

    val uiState: StateFlow<ReceiveUiState> =
        _uiState.asStateFlow()

    fun loadProfile() {

        viewModelScope.launch {

            _uiState.value =
                ReceiveUiState(
                    isLoading = true
                )

            try {

                /*
                 * لا يوجد إعداد استقبال من المستخدم.
                 *
                 * Backend:
                 * - يعيد الـProfile الموجود.
                 * - أو ينشئ Transfer ID تلقائيًا
                 *   للحساب القديم إذا لم يكن موجودًا.
                 */
                val profile =
                    repository
                        .getMyProfile()

                _uiState.value =
                    ReceiveUiState(
                        profile =
                            profile
                    )

            } catch (
                exception: HttpException
            ) {

                _uiState.value =
                    ReceiveUiState(
                        errorMessage =
                            when (
                                exception.code()
                            ) {

                                401 ->
                                    "انتهت جلسة تسجيل الدخول"

                                else ->
                                    "تعذر تحميل بيانات الاستقبال (${exception.code()})"
                            }
                    )

            } catch (
                exception: IOException
            ) {

                _uiState.value =
                    ReceiveUiState(
                        errorMessage =
                            "تعذر الاتصال بالإنترنت"
                    )

            } catch (
                exception: Exception
            ) {

                _uiState.value =
                    ReceiveUiState(
                        errorMessage =
                            exception.message
                                ?: "تعذر تحميل بيانات الاستقبال"
                    )
            }
        }
    }

    fun clear() {

        _uiState.value =
            ReceiveUiState()
    }
}