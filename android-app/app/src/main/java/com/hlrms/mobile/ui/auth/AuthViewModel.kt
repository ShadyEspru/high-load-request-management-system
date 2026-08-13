package com.hlrms.mobile.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hlrms.mobile.data.local.SessionManager
import com.hlrms.mobile.data.remote.ApiClient
import com.hlrms.mobile.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import com.hlrms.mobile.data.remote.TransferApiClient
import com.hlrms.mobile.data.repository.TransferProfileRepository

class AuthViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val sessionManager =
        SessionManager(application)

    private val repository =
        AuthRepository(
            authApi = ApiClient.authApi,
            sessionManager = sessionManager
        )


    private val transferProfileRepository =
        TransferProfileRepository(
            transferApi =
                TransferApiClient.transferApi,
            sessionManager =
                sessionManager
        )

    private val _uiState =
        MutableStateFlow(AuthUiState())

    val uiState: StateFlow<AuthUiState> =
        _uiState.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {

            val accessToken =
                sessionManager.accessToken.first()

            _uiState.value =
                AuthUiState(
                    isSessionChecked = true,
                    isAuthenticated =
                        !accessToken.isNullOrBlank()
                )
        }
    }

    fun login(
        email: String,
        password: String
    ) {
        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null
                )

            try {
                repository.login(
                    email = email.trim(),
                    password = password
                )

                _uiState.value =
                    AuthUiState(
                        loginSucceeded = true,
                        isSessionChecked = true,
                        isAuthenticated = true
                    )

            } catch (exception: HttpException) {

                _uiState.value =
                    AuthUiState(
                        errorMessage =
                            "بيانات تسجيل الدخول غير صحيحة",
                        isSessionChecked = true
                    )

            } catch (exception: IOException) {

                _uiState.value =
                    AuthUiState(
                        errorMessage =
                            "تعذر الاتصال بالإنترنت",
                        isSessionChecked = true
                    )

            } catch (exception: Exception) {

                _uiState.value =
                    AuthUiState(
                        errorMessage =
                            "حدث خطأ غير متوقع",
                        isSessionChecked = true
                    )
            }
        }
    }

    fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ) {
        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null
                )

            try {

                val normalizedEmail =
                    email.trim()

                val normalizedFirstName =
                    firstName.trim()

                val normalizedLastName =
                    lastName.trim()

                repository.register(
                    email =
                        normalizedEmail,

                    password =
                        password,

                    firstName =
                        normalizedFirstName,

                    lastName =
                        normalizedLastName
                )

                /*
                 * Login تلقائي حتى يصبح لدينا Access Token.
                 */
                repository.login(
                    email =
                        normalizedEmail,

                    password =
                        password
                )

                /*
                 * إنشاء Transfer Profile بصمت.
                 * المستخدم لا يختار Transfer ID.
                 */
                try {

                    transferProfileRepository
                        .createProfile(
                            "$normalizedFirstName $normalizedLastName"
                                .trim()
                        )

                } catch (
                    ignored: Exception
                ) {

                    /*
                     * لا نفشل إنشاء الحساب إذا كانت
                     * transfer-api متوقفة لحظيًا.
                     *
                     * GET /transfer/me سيقوم لاحقًا
                     * بالإنشاء التلقائي كـ fallback.
                     */
                }

                _uiState.value =
                    AuthUiState(
                        registrationSucceeded =
                            true,

                        isSessionChecked =
                            true,

                        isAuthenticated =
                            true
                    )

            } catch (exception: HttpException) {

                _uiState.value =
                    AuthUiState(
                        errorMessage =
                            "تعذر إنشاء الحساب",
                        isSessionChecked = true
                    )

            } catch (exception: IOException) {

                _uiState.value =
                    AuthUiState(
                        errorMessage =
                            "تعذر الاتصال بالإنترنت",
                        isSessionChecked = true
                    )

            } catch (exception: Exception) {

                _uiState.value =
                    AuthUiState(
                        errorMessage =
                            "حدث خطأ غير متوقع",
                        isSessionChecked = true
                    )
            }
        }
    }

    fun logout(
        onCompleted: () -> Unit
    ) {
        viewModelScope.launch {

            repository.logout()

            _uiState.value =
                AuthUiState(
                    isSessionChecked = true,
                    isAuthenticated = false
                )

            onCompleted()
        }
    }

    fun clearState() {
        _uiState.value =
            _uiState.value.copy(
                isLoading = false,
                errorMessage = null,
                loginSucceeded = false,
                registrationSucceeded = false
            )
    }
}