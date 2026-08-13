package com.hlrms.mobile.ui.history

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.hlrms.mobile.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class HistoryFilter {

    ALL,
    OUTGOING,
    INCOMING
}

@Composable
fun TransferHistoryScreen(

    uiState:
        TransferHistoryUiState,

    onRetry: () -> Unit,

    onBackClick: () -> Unit
) {

    var filter by
    rememberSaveable {

        mutableStateOf(
            HistoryFilter.ALL
        )
    }

    val visibleItems =
        uiState.items.filter {
                item ->

            when (
                filter
            ) {

                HistoryFilter.ALL ->
                    true

                HistoryFilter.OUTGOING ->
                    item.direction ==
                            "OUTGOING"

                HistoryFilter.INCOMING ->
                    item.direction ==
                            "INCOMING"
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
    ) {

        HistoryTopBar(
            onBackClick =
                onBackClick
        )

        when {

            uiState.isLoading -> {

                Box(
                    modifier =
                        Modifier.fillMaxSize(),

                    contentAlignment =
                        Alignment.Center
                ) {

                    CircularProgressIndicator()
                }
            }

            uiState.hasError -> {

                HistoryError(
                    onRetry =
                        onRetry
                )
            }

            else -> {

                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal =
                                    18.dp
                            )
                ) {

                    LazyRow(
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {

                        items(
                            HistoryFilter.entries
                        ) {
                                option ->

                            FilterChip(
                                selected =
                                    filter ==
                                            option,

                                onClick = {

                                    filter =
                                        option
                                },

                                label = {

                                    Text(
                                        historyFilterName(
                                            option
                                        )
                                    )
                                }
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(
                                12.dp
                            )
                    )

                    if (
                        visibleItems.isEmpty()
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize(),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                text =
                                    stringResource(
                                        R.string.history_empty
                                    ),

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        }

                    } else {

                        LazyColumn(
                            modifier =
                                Modifier
                                    .fillMaxSize(),

                            verticalArrangement =
                                Arrangement.spacedBy(
                                    10.dp
                                )
                        ) {

                            items(
                                items =
                                    visibleItems,

                                key = {
                                    it.id
                                }
                            ) {
                                    item ->

                                TransferHistoryCard(
                                    item =
                                        item
                                )
                            }

                            item {

                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            16.dp
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTopBar(
    onBackClick: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 10.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        TextButton(
            onClick =
                onBackClick
        ) {

            Text(
                stringResource(
                    R.string.back
                )
            )
        }

        Text(
            text =
                stringResource(
                    R.string.transfer_history
                ),

            modifier =
                Modifier.weight(
                    1f
                ),

            textAlign =
                TextAlign.Center,

            style =
                MaterialTheme
                    .typography
                    .titleLarge,

            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier
                    .padding(
                        horizontal = 24.dp
                    )
        )
    }
}

@Composable
private fun TransferHistoryCard(
    item: TransferHistoryItem
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        colors =
            CardDefaults
                .cardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surface
                )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {

                    Text(
                        text =
                            item.recipientName
                                .ifBlank {

                                    stringResource(
                                        R.string.history_recipient_unknown
                                    )
                                },

                        fontWeight =
                            FontWeight.Bold,

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )

                    if (
                        item.recipientTransferId
                            .isNotBlank()
                    ) {

                        CompositionLocalProvider(
                            LocalLayoutDirection provides
                                    LayoutDirection.Ltr
                        ) {

                            Text(
                                text =
                                    item.recipientTransferId,

                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        }
                    }
                }

                CompositionLocalProvider(
                    LocalLayoutDirection provides
                            LayoutDirection.Ltr
                ) {

                    Text(
                        text =
                            "${
                                if (
                                    item.direction ==
                                    "INCOMING"
                                ) "+"
                                else "-"
                            }${item.amount} ${item.currency}"
                                .trim(),

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text =
                        formatHistoryDate(
                            item.createdAt
                        ),

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Text(
                    text =
                        statusText(
                            item.status
                        ),

                    fontWeight =
                        FontWeight.SemiBold,

                    color =
                        statusColor(
                            item.status
                        )
                )
            }
        }
    }
}

@Composable
private fun historyFilterName(
    filter:
        HistoryFilter
): String {

    return when (
        filter
    ) {

        HistoryFilter.ALL ->
            stringResource(
                R.string.history_all
            )

        HistoryFilter.OUTGOING ->
            stringResource(
                R.string.history_outgoing
            )

        HistoryFilter.INCOMING ->
            stringResource(
                R.string.history_incoming
            )
    }
}

@Composable
private fun statusText(
    status: String
): String {

    return when (
        status
    ) {

        "COMPLETED" ->
            stringResource(
                R.string.history_completed
            )

        "FAILED",
        "CANCELLED" ->
            stringResource(
                R.string.history_failed
            )

        else ->
            stringResource(
                R.string.history_processing
            )
    }
}

@Composable
private fun statusColor(
    status: String
) =
    when (
        status
    ) {

        "COMPLETED" ->
            MaterialTheme
                .colorScheme
                .secondary

        "FAILED",
        "CANCELLED" ->
            MaterialTheme
                .colorScheme
                .error

        else ->
            MaterialTheme
                .colorScheme
                .primary
    }

@Composable
private fun HistoryError(
    onRetry: () -> Unit
) {

    Box(
        modifier =
            Modifier.fillMaxSize(),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    stringResource(
                        R.string.history_load_failed
                    ),

                color =
                    MaterialTheme
                        .colorScheme
                        .error
            )

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Button(
                onClick =
                    onRetry
            ) {

                Text(
                    stringResource(
                        R.string.retry
                    )
                )
            }
        }
    }
}

private fun formatHistoryDate(
    value: String?
): String {

    if (
        value.isNullOrBlank()
    ) {

        return ""
    }

    return try {

        DateTimeFormatter
            .ofPattern(
                "yyyy-MM-dd HH:mm"
            )
            .withZone(
                ZoneId.systemDefault()
            )
            .format(
                Instant.parse(
                    value
                )
            )

    } catch (
        exception: Exception
    ) {

        value
    }
}
