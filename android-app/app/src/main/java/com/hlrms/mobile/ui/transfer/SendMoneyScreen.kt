package com.hlrms.mobile.ui.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.style.TextDirection
import com.hlrms.mobile.R

@Composable
fun SendMoneyScreen(
    uiState: TransferUiState,

    onFindRecipient: (
        transferId: String
    ) -> Unit,

    onClearRecipient: () -> Unit,

    onSendClick: (
        amount: Double,
        currency: String
    ) -> Unit,

    onRequestCreated: (
        requestId: String
    ) -> Unit,

    onBackClick: () -> Unit
) {

    val recipientError =
        uiState.recipientErrorRes
            ?.let { resourceId ->
                stringResource(resourceId)
            }

    val sendError =
        uiState.sendErrorRes
            ?.let { resourceId ->
                stringResource(resourceId)
            }


    var transferId by
    rememberSaveable {
        mutableStateOf("")
    }

    var amount by
    rememberSaveable {
        mutableStateOf("")
    }

    var selectedCurrency by
    rememberSaveable {
        mutableStateOf("USD")
    }

    var reviewMode by
    rememberSaveable {
        mutableStateOf(false)
    }

    val parsedAmount =
        amount.toDoubleOrNull()

    LaunchedEffect(
        uiState.createdRequest?.id
    ) {

        val requestId =
            uiState.createdRequest?.id

        if (requestId != null) {
            onRequestCreated(
                requestId
            )
        }
    }

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
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 20.dp,
                    vertical = 12.dp
                )
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
            ) {

                TextButton(
                    onClick =
                        onBackClick,

                    modifier =
                        Modifier.align(
                            Alignment.CenterStart
                        )
                ) {}

                Text(
                    text =
                        if (reviewMode) {
                            stringResource(R.string.review_transfer)
                        } else {
                            stringResource(R.string.search_by_transfer_id)
                        },

                    modifier =
                        Modifier.align(
                            Alignment.Center
                        ),

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onBackground,

                    textAlign =
                        TextAlign.Center
                )
            }

            TextButton(
                onClick =
                    onBackClick
            ) {

                Text(
                    stringResource(R.string.back)
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(18.dp)
        )

        if (!reviewMode) {

            if (uiState.recipient == null) {

                RecipientLookupContent(
                    transferId =
                        transferId,

                    onTransferIdChange = {

                        transferId =
                            it.uppercase()
                                .filter { char ->
                                    char.isLetterOrDigit()
                                }
                                .take(16)

                    },

                    isSearching =
                        uiState.isSearchingRecipient,

                    errorMessage =
                        recipientError,

                    onFindClick = {
                        onFindRecipient(
                            transferId
                        )
                    }
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

            } else {

                val recipient =
                    uiState.recipient

                Text(
                    text = stringResource(R.string.recipient),
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                            Modifier.padding(18.dp)
                    ) {

                        Text(
                            text =
                                recipient.displayName,

                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                recipient.transferId,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }

                TextButton(
                    onClick = {

                        onClearRecipient()

                        transferId = ""

                        amount = ""
                    }
                ) {

                    Text(
                        stringResource(R.string.change_recipient)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )

                Text(
                    text = stringResource(R.string.currency),

                    fontWeight =
                        FontWeight.SemiBold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onBackground
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    listOf(
                        "USD",
                        "EUR",
                        "TRY",
                        "SYP"
                    ).forEach { currency ->

                        FilterChip(
                            selected =
                                selectedCurrency ==
                                        currency,

                            onClick = {
                                selectedCurrency =
                                    currency
                            },

                            label = {
                                Text(currency)
                            },

                            enabled =
                                !uiState.isSending
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )

                OutlinedTextField(
                    value =
                        amount,

                    onValueChange = { value ->

                        amount =
                            value.filter { char ->

                                char.isDigit() ||
                                        char == '.'
                            }
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    enabled =
                        !uiState.isSending,

                    label = {
                        Text(stringResource(R.string.amount))
                    },

                    suffix = {
                        Text(
                            selectedCurrency
                        )
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
                                KeyboardType.Decimal
                        )
                )

                if (
                    sendError != null
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(14.dp)
                    )

                    Text(
                        text =
                            sendError,

                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(28.dp)
                )

                Button(
                    onClick = {
                        reviewMode = true
                    },

                    enabled =
                        parsedAmount != null &&
                                parsedAmount > 0.0 &&
                                !uiState.isSending,

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(stringResource(R.string.continue_action))
                }
            }

        } else {

            val recipient =
                uiState.recipient

            if (
                recipient != null &&
                parsedAmount != null
            ) {

                Text(
                    text =
                        stringResource(R.string.review_transfer_hint),
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
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
                                stringResource(R.string.recipient)
                        )

                        Text(
                            text =
                                recipient.displayName,

                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                recipient.transferId,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )

                        Spacer(
                            modifier =
                                Modifier.height(24.dp)
                        )

                        Text(
                            text =
                                stringResource(R.string.amount)
                        )

                        Text(
                            text =
                                "$amount $selectedCurrency",

                            style =
                                MaterialTheme
                                    .typography
                                    .headlineSmall,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }

                if (
                    sendError != null
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    Text(
                        text =
                            sendError,

                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(28.dp)
                )

                Button(
                    onClick = {

                        onSendClick(
                            parsedAmount,
                            selectedCurrency
                        )
                    },

                    enabled =
                        !uiState.isSending,

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    if (
                        uiState.isSending
                    ) {

                        CircularProgressIndicator()

                    } else {

                        Text(
                            stringResource(R.string.confirm_send)
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                OutlinedButton(
                    onClick = {
                        reviewMode = false
                    },

                    enabled =
                        !uiState.isSending,

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        stringResource(R.string.edit_details)
                    )
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        if (!reviewMode) {

            TextButton(
                onClick =
                    onBackClick,

                enabled =
                    !uiState.isSending &&
                            !uiState.isSearchingRecipient,

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(stringResource(R.string.back))
            }
        }
    }
}

@Composable
private fun RecipientLookupContent(
    transferId: String,
    onTransferIdChange: (String) -> Unit,
    isSearching: Boolean,
    errorMessage: String?,
    onFindClick: () -> Unit
) {

    Text(
        text =
            stringResource(R.string.enter_recipient_transfer_id),
        modifier =
            Modifier.fillMaxWidth(),
        style =
            MaterialTheme.typography
                .titleMedium,
        fontWeight =
            FontWeight.SemiBold,
                    color =
                    MaterialTheme
                    .colorScheme
                    .onBackground,
        textAlign = TextAlign.Start
    )

    Spacer(
        modifier =
            Modifier.height(8.dp)
    )

    Text(
        text =
            stringResource(R.string.transfer_id_manual_hint),
        modifier = Modifier.fillMaxWidth(),
        color =
            MaterialTheme
                .colorScheme
                .onSurfaceVariant,
        textAlign = TextAlign.Start
    )

    Spacer(
        modifier =
            Modifier.height(18.dp)
    )

    OutlinedTextField(
        value =
            transferId,

        onValueChange =
            onTransferIdChange,

        modifier =
            Modifier.fillMaxWidth(),

        enabled =
            !isSearching,

        label = {
            Text(
                stringResource(R.string.transfer_id)
            )
        },

        supportingText = {
            Text(
                "${transferId.length}/16"
            )
        },

        textStyle =
            LocalTextStyle.current.copy(
                textAlign = TextAlign.Left,
                textDirection = TextDirection.Ltr
            ),

        singleLine = true
    )

    if (
        errorMessage != null
    ) {

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
    }

    Spacer(
        modifier =
            Modifier.height(20.dp)
    )

    Button(
        onClick =
            onFindClick,

        enabled =
            transferId.length == 16 &&
                    !isSearching,

        modifier =
            Modifier.fillMaxWidth()
    ) {

        if (isSearching) {

            CircularProgressIndicator()

        } else {

            Text(
                stringResource(R.string.verify_recipient)
            )
        }
    }
}