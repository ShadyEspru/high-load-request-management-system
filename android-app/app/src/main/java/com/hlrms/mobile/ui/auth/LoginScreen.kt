package com.hlrms.mobile.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hlrms.mobile.R
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onLoginClick: (
        email: String,
        password: String
    ) -> Unit,
    onRegisterClick: () -> Unit,
    onLoginSucceeded: () -> Unit
) {
    var email by rememberSaveable {
        mutableStateOf("")
    }

    var password by rememberSaveable {
        mutableStateOf("")
    }

    val formIsValid =
        email.isNotBlank() &&
                password.isNotBlank()

    LaunchedEffect(
        uiState.loginSucceeded
    ) {
        if (uiState.loginSucceeded) {
            onLoginSucceeded()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 24.dp,
                vertical = 48.dp
            ),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.login_title),
            style =
                MaterialTheme.typography
                    .headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = stringResource(R.string.login_subtitle),
            style =
                MaterialTheme.typography
                    .bodyMedium
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
            },
            modifier =
                Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
            colors = authTextFieldColors(),
            label = {
                Text(stringResource(R.string.email_address))
            },
            textStyle =
                LocalTextStyle.current.copy(
                    textAlign = TextAlign.Left,
                    textDirection = TextDirection.Ltr
                ),
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Email
                )
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            modifier =
                Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
            colors = authTextFieldColors(),
            label = {
                Text(stringResource(R.string.password))
            },
            textStyle =
                LocalTextStyle.current.copy(
                    textAlign = TextAlign.Left,
                    textDirection = TextDirection.Ltr
                ),
            singleLine = true,
            visualTransformation =
                PasswordVisualTransformation(),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Password
                )
        )

        if (
            uiState.errorMessage != null
        ) {
            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Text(
                text =
                    uiState.errorMessage,
                color =
                    MaterialTheme
                        .colorScheme
                        .error
            )
        }

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        Button(
            onClick = {
                onLoginClick(
                    email,
                    password
                )
            },
            enabled =
                formIsValid &&
                        !uiState.isLoading,
            modifier =
                Modifier.fillMaxWidth()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                Text(stringResource(R.string.login_title))
            }
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        TextButton(
            onClick =
                onRegisterClick,
            enabled =
                !uiState.isLoading
        ) {
            Text(
                stringResource(
                    R.string.no_account_create
                )
            )
        }
    }
}