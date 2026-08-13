package com.hlrms.mobile.ui.receive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hlrms.mobile.R

@Composable
fun ReceiveMoneyScreen(
    uiState: ReceiveUiState,
    onLoadProfile: () -> Unit,
    onBackClick: () -> Unit
) {

    LaunchedEffect(Unit) {

        onLoadProfile()
    }

    val appLayoutDirection =
        LocalLayoutDirection.current

    CompositionLocalProvider(
        LocalLayoutDirection provides
                appLayoutDirection
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme
                            .colorScheme
                            .background
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
        ) {

            ReceiveTopBar(
                onBackClick =
                    onBackClick
            )

            when {

                uiState.isLoading -> {

                    LoadingContent()
                }

                uiState.profile != null -> {

                    ProfileContent(
                        displayName =
                            uiState.profile
                                .displayName,

                        transferId =
                            uiState.profile
                                .transferId,

                        qrContent =
                            uiState.profile
                                .qrContent
                    )
                }

                uiState.errorMessage != null -> {

                    ErrorContent(
                        message =
                            uiState
                                .errorMessage,

                        onRetry =
                            onLoadProfile
                    )
                }

                else -> {

                    LoadingContent()
                }
            }
        }
    }
}

@Composable
private fun ReceiveTopBar(
    onBackClick: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        TextButton(
            onClick =
                onBackClick
        ) {

            Text(
                text =
                    stringResource(R.string.back)
            )
        }

        Text(
            text =
                stringResource(R.string.receive_money_title),

            modifier =
                Modifier
                    .weight(1f),

            textAlign =
                TextAlign.Center,

            style =
                MaterialTheme
                    .typography
                    .titleLarge,

            fontWeight =
                FontWeight.Bold,

            color =
                MaterialTheme
                    .colorScheme
                    .onBackground
        )

        /*
         * لتحقيق توازن بصري مع زر الرجوع.
         */
        Spacer(
            modifier =
                Modifier.size(
                    64.dp,
                    1.dp
                )
        )
    }
}

@Composable
private fun LoadingContent() {

    Box(
        modifier =
            Modifier.fillMaxSize(),

        contentAlignment =
            Alignment.Center
    ) {

        CircularProgressIndicator()
    }
}

@Composable
private fun ProfileContent(
    displayName: String,
    transferId: String,
    qrContent: String
) {

    val context =
        LocalContext.current

    /*
     * fallback دفاعي لو عاد qrContent فارغًا
     * لأي سبب من الـBackend.
     */
    val effectiveQrContent =
        qrContent.ifBlank {

            "hlrms://transfer/$transferId"
        }

    val qrBitmap =
        remember(
            effectiveQrContent
        ) {

            QrCodeGenerator.generate(
                effectiveQrContent
            )
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 20.dp
                ),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )

        Text(
            text =
                stringResource(R.string.receive_share_hint),

            modifier =
                Modifier.fillMaxWidth(),

            textAlign =
                TextAlign.Center,

            style =
                MaterialTheme
                    .typography
                    .bodyLarge,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Spacer(
            modifier =
                Modifier.height(
                    22.dp
                )
        )

        Card(
            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(
                    20.dp
                ),

            colors =
                CardDefaults
                    .cardColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant
                    )
        ) {

            Column(
                modifier =
                    Modifier.padding(
                        18.dp
                    )
            ) {

                Text(
                    text =
                        stringResource(R.string.your_transfer_id),

                    style =
                        MaterialTheme
                            .typography
                            .labelLarge,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )

                Text(
                    text =
                        transferId,

                    modifier =
                        Modifier.fillMaxWidth(),

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,

                    fontWeight =
                        FontWeight.Bold,

                    textAlign =
                        TextAlign.Center,

                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            16.dp
                        )
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )
                ) {

                    OutlinedButton(
                        onClick = {

                            copyTransferId(
                                context =
                                    context,

                                transferId =
                                    transferId
                            )
                        },

                        modifier =
                            Modifier
                                .weight(
                                    1f
                                )
                    ) {

                        Text(
                            stringResource(R.string.copy)
                        )
                    }

                    Button(
                        onClick = {

                            QrShareHelper
                                .share(
                                    context =
                                        context,

                                    qrBitmap =
                                        qrBitmap,

                                    transferId =
                                        transferId,

                                    qrContent =
                                        effectiveQrContent
                                )
                        },

                        modifier =
                            Modifier
                                .weight(
                                    1f
                                )
                    ) {

                        Text(
                            stringResource(R.string.share)
                        )
                    }
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(
                    24.dp
                )
        )

        Card(
            shape =
                RoundedCornerShape(
                    24.dp
                ),

            colors =
                CardDefaults
                    .cardColors(
                        containerColor =
                            androidx.compose
                                .ui
                                .graphics
                                .Color.White
                    )
        ) {

            Image(
                bitmap =
                    qrBitmap
                        .asImageBitmap(),

                contentDescription =
                    stringResource(R.string.qr_transfer_id_description),

                modifier =
                    Modifier
                        .padding(
                            14.dp
                        )
                        .size(
                            250.dp
                        )
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    18.dp
                )
        )

        Text(
            text =
                displayName,

            style =
                MaterialTheme
                    .typography
                    .titleMedium,

            fontWeight =
                FontWeight.Bold,

            color =
                MaterialTheme
                    .colorScheme
                    .onBackground
        )

        Spacer(
            modifier =
                Modifier.height(
                    5.dp
                )
        )

        Text(
            text =
                stringResource(R.string.app_name),

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Spacer(
            modifier =
                Modifier.height(
                    24.dp
                )
        )
    }
}

@Composable
private fun ProfileSetupContent(
    isLoading: Boolean,
    errorMessage: String?,
    onCreateProfile: (
        String
    ) -> Unit
) {

    var displayName by
    rememberSaveable {

        mutableStateOf("")
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 24.dp
                ),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )

        Text(
            text =
                stringResource(R.string.receive_profile_missing),

            textAlign =
                TextAlign.Center,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Spacer(
            modifier =
                Modifier.height(
                    22.dp
                )
        )

        OutlinedTextField(
            value =
                displayName,

            onValueChange = {

                displayName =
                    it.take(
                        80
                    )
            },

            modifier =
                Modifier
                    .fillMaxWidth(),

            label = {

                Text(
                    stringResource(R.string.recipient_display_name)
                )
            },

            singleLine =
                true,

            enabled =
                !isLoading
        )

        if (
            errorMessage != null
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            Text(
                text =
                    errorMessage,

                color =
                    MaterialTheme
                        .colorScheme
                        .error,

                textAlign =
                    TextAlign.Center
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    18.dp
                )
        )

        Button(
            onClick = {

                onCreateProfile(
                    displayName
                )
            },

            modifier =
                Modifier
                    .fillMaxWidth(),

            enabled =
                !isLoading &&
                        displayName
                            .trim()
                            .length >= 2
        ) {

            if (
                isLoading
            ) {

                CircularProgressIndicator(
                    modifier =
                        Modifier.size(
                            22.dp
                        ),

                    strokeWidth =
                        2.dp
                )

            } else {
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    24.dp
                ),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text =
                message,

            color =
                MaterialTheme
                    .colorScheme
                    .error,

            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.height(
                    16.dp
                )
        )

        Button(
            onClick =
                onRetry
        ) {

            Text(
                stringResource(R.string.retry)
            )
        }
    }
}

private fun copyTransferId(
    context: Context,
    transferId: String
) {

    val clipboard =
        context.getSystemService(
            Context.CLIPBOARD_SERVICE
        ) as ClipboardManager

    clipboard.setPrimaryClip(
        ClipData.newPlainText(
            "HLRMS Transfer ID",
            transferId
        )
    )
}