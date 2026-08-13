package com.hlrms.mobile.ui.exchange

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.hlrms.mobile.R
import com.hlrms.mobile.data.remote.exchange.ExchangeRatesResponse
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val exchangeCurrencyOrder =
    listOf(
        "USD",
        "EUR",
        "TRY",
        "SYP"
    )

@Composable
fun ExchangeRatesScreen(

    uiState: ExchangeRatesUiState,

    onRetry: () -> Unit,

    onBackClick: () -> Unit
) {

    CompositionLocalProvider(
        LocalLayoutDirection provides
                LayoutDirection.Ltr
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

        ExchangeTopBar(
            onBackClick =
                onBackClick
        )

        when {

            uiState.isLoading -> {

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize(),

                    contentAlignment =
                        Alignment.Center
                ) {

                    CircularProgressIndicator()
                }
            }

            uiState.hasError -> {

                ExchangeError(
                    onRetry =
                        onRetry
                )
            }

            uiState.response != null -> {

                ExchangeRatesContent(
                    response =
                        uiState.response
                )
            }
        }
    }
    }
}

@Composable
private fun ExchangeTopBar(
    onBackClick: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 10.dp
                ),

        horizontalArrangement =
            Arrangement.SpaceBetween,

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        TextButton(
            onClick =
                onBackClick
        ) {

            Text(
                text =
                    stringResource(
                        R.string.back
                    )
            )
        }

        Text(
            text =
                stringResource(
                    R.string.exchange_rates
                ),

            style =
                MaterialTheme
                    .typography
                    .titleLarge,

            fontWeight =
                FontWeight.Bold
        )
    }
}

@Composable
private fun ExchangeRatesContent(
    response: ExchangeRatesResponse
) {

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 18.dp
                ),

        verticalArrangement =
            Arrangement.spacedBy(
                12.dp
            )
    ) {

        item {

            ExchangeInformationCard(
                response =
                    response
            )
        }

        items(
            items =
                exchangeCurrencyOrder,

            key = {
                it
            }
        ) {
                baseCurrency ->

            val rates =
                response
                    .rates[baseCurrency]

            if (
                rates != null
            ) {

                ExchangeCurrencyCard(
                    baseCurrency =
                        baseCurrency,

                    rates =
                        rates
                )
            }
        }

        item {

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )

            Text(
                text =
                    stringResource(
                        R.string.exchange_reference_note
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

            Spacer(
                modifier =
                    Modifier.height(
                        20.dp
                    )
            )
        }
    }
}

@Composable
private fun ExchangeInformationCard(
    response: ExchangeRatesResponse
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {

            Text(
                text =
                    stringResource(
                        R.string.exchange_online_rates
                    ),

                fontWeight =
                    FontWeight.Bold,

                textAlign =
                    androidx.compose.ui.text.style.TextAlign.Left,

                color =
                    MaterialTheme
                        .colorScheme
                        .onPrimaryContainer
            )

            Spacer(
                modifier =
                    Modifier.height(
                        7.dp
                    )
            )

            Text(
                text =
                    stringResource(
                        R.string.exchange_last_updated,
                        formatExchangeDate(
                            response.updatedAt
                        )
                    ),

                style =
                    MaterialTheme
                        .typography
                        .bodySmall,

                color =
                    MaterialTheme
                        .colorScheme
                        .onPrimaryContainer
            )

            Text(
                text =
                    stringResource(
                        R.string.exchange_provider,
                        response.provider
                    ),

                style =
                    MaterialTheme
                        .typography
                        .bodySmall,

                color =
                    MaterialTheme
                        .colorScheme
                        .onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ExchangeCurrencyCard(

    baseCurrency: String,

    rates:
        Map<String, BigDecimal>
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                20.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    17.dp
                )
        ) {

            Text(
                text =
                    baseCurrency,

                style =
                    MaterialTheme
                        .typography
                        .titleLarge,

                fontWeight =
                    FontWeight.Bold,

                textAlign =
                    androidx.compose.ui.text.style.TextAlign.Left,

                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            HorizontalDivider()

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            exchangeCurrencyOrder
                .filter {
                    it !=
                            baseCurrency
                }
                .forEach {
                        targetCurrency ->

                    val rate =
                        rates[
                            targetCurrency
                        ]

                    if (
                        rate != null
                    ) {

                        ExchangeRateRow(
                            baseCurrency =
                                baseCurrency,

                            targetCurrency =
                                targetCurrency,

                            rate =
                                rate
                        )
                    }
                }
        }
    }
}

@Composable
private fun ExchangeRateRow(

    baseCurrency: String,

    targetCurrency: String,

    rate: BigDecimal
) {

    CompositionLocalProvider(
        LocalLayoutDirection provides
                LayoutDirection.Ltr
    ) {

        Text(
            text =
                "1 $baseCurrency = ${formatExchangeRate(rate)} $targetCurrency",

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 10.dp
                    ),

            fontWeight =
                FontWeight.SemiBold,

            textAlign =
                androidx.compose.ui.text.style.TextAlign.Left,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurface
        )
    }
}

@Composable
private fun ExchangeError(
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
                        R.string.exchange_load_failed
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
                    text =
                        stringResource(
                            R.string.retry
                        )
                )
            }
        }
    }
}

private fun formatExchangeRate(
    value: BigDecimal
): String {

    val absolute =
        value.abs()

    val scale =
        when {

            absolute >=
                    BigDecimal(
                        "1000"
                    ) ->
                2

            absolute >=
                    BigDecimal.ONE ->
                4

            absolute >=
                    BigDecimal(
                        "0.01"
                    ) ->
                6

            else ->
                8
        }

    return value
        .setScale(
            scale,
            RoundingMode.HALF_UP
        )
        .stripTrailingZeros()
        .toPlainString()
}

private fun formatExchangeDate(
    value: String
): String {

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
