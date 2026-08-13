package com.hlrms.mobile.ui.auth

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,

    val loginSucceeded: Boolean = false,
    val registrationSucceeded: Boolean = false,

    val isSessionChecked: Boolean = false,
    val isAuthenticated: Boolean = false
)