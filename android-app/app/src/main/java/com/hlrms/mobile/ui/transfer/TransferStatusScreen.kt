package com.hlrms.mobile.ui.transfer

import com.hlrms.mobile.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TransferStatusScreen(
    uiState: TransferStatusUiState,
    onRetry: () -> Unit,
    onDone: () -> Unit
) {

    val errorMessage =
        uiState.errorMessageRes
            ?.let { resourceId ->
                stringResource(resourceId)
            }


    val request =
        uiState.request

    val status =
        request?.status
            ?.uppercase()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),

        verticalArrangement =
            Arrangement.Center,

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        when {

            uiState.isLoading -> {

                CircularProgressIndicator()

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )

                Text(
                    text =
                        stringResource(R.string.transfer_tracking)
                )
            }

            errorMessage != null -> {

                Text(
                    text =
                        stringResource(R.string.transfer_tracking_failed),

                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Text(
                    text =
                        errorMessage,

                    color =
                        MaterialTheme
                            .colorScheme
                            .error
                )

                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )

                Button(
                    onClick =
                        onRetry,

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        stringResource(R.string.retry)
                    )
                }
            }

            request != null -> {

                Text(
                    text =
                        when (status) {

                            "COMPLETED" ->
                                stringResource(R.string.transfer_success)

                            "FAILED" ->
                                stringResource(R.string.transfer_failed)

                            "PROCESSING" ->
                                stringResource(R.string.transfer_processing)

                            else ->
                                stringResource(R.string.transfer_request_received)
                        },

                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                            Modifier.padding(20.dp)
                    ) {

                        Text(
                            text =
                                stringResource(R.string.operation_id),

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )

                        Text(
                            text =
                                request.id,

                            fontWeight =
                                FontWeight.SemiBold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(20.dp)
                        )

                        Text(
                            text =
                                stringResource(R.string.status),

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )

                        Text(
                            text =
                                status ?: "-",

                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,

                            fontWeight =
                                FontWeight.Bold
                        )

                        if (
                            status == "FAILED" &&
                            request.errorMessage != null
                        ) {

                            Spacer(
                                modifier =
                                    Modifier.height(16.dp)
                            )

                            Text(
                                text =
                                    request.errorMessage,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .error
                            )
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(28.dp)
                )

                if (
                    status == "COMPLETED" ||
                    status == "FAILED"
                ) {

                    Button(
                        onClick =
                            onDone,

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            stringResource(R.string.back_to_home)
                        )
                    }

                } else {

                    OutlinedButton(
                        onClick =
                            onDone,

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            stringResource(R.string.continue_in_background)
                        )
                    }
                }
            }
        }
    }
}