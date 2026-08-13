package com.hlrms.mobile.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hlrms.mobile.R
import com.hlrms.mobile.ui.history.TransferHistoryItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private enum class TransactionSort {

    NEWEST,
    OLDEST,
    LARGEST,
    SMALLEST
}

private enum class DemoTransactionType {

    OUTGOING,
    INCOMING,
    EXCHANGE
}

private data class DemoTransaction(

    val id: String,

    val type: DemoTransactionType,

    val dateTime: LocalDateTime,

    val amount: Double,

    val currency: String,

    val counterpartName: String
)

@Composable
fun HomeScreen(

    accountDisplayName: String,

    walletUiState: HomeUiState,

    recentTransfers:
        List<TransferHistoryItem>,

    appLanguage: String,

    onLanguageChange: (
        String
    ) -> Unit,

    isDarkTheme: Boolean,

    onDarkThemeChange: (
        Boolean
    ) -> Unit,

    onLogoutClick: () -> Unit,

    onSendClick: () -> Unit = {},

    onReceiveClick: () -> Unit = {},

    onExchangeClick: () -> Unit = {},

    onHistoryClick: () -> Unit = {}
) {

    val drawerState =
        rememberDrawerState(
            initialValue =
                DrawerValue.Closed
        )

    val scope =
        rememberCoroutineScope()

    val contentDirection =
        if (
            appLanguage == "ar"
        ) {

            LayoutDirection.Rtl

        } else {

            LayoutDirection.Ltr
        }

    /*
     * uppercaseChar يجعل:
     * shady -> S
     * Shady -> S
     *
     * العربية تبقى كما هي.
     */
    val accountInitial =
        accountDisplayName
            .trim()
            .firstOrNull()
            ?.uppercaseChar()
            ?.toString()
            ?: "H"

    /*
     * الـDrawer يبقى من اليسار.
     */
    CompositionLocalProvider(
        LocalLayoutDirection provides
                LayoutDirection.Ltr
    ) {

        ModalNavigationDrawer(

            drawerState =
                drawerState,

            drawerContent = {

                CompositionLocalProvider(
                    LocalLayoutDirection provides
                            contentDirection
                ) {

                    AccountDrawer(

                        accountDisplayName =
                            accountDisplayName,

                        accountInitial =
                            accountInitial,

                        appLanguage =
                            appLanguage,

                        onLanguageChange =
                            onLanguageChange,

                        isDarkTheme =
                            isDarkTheme,

                        onDarkThemeChange =
                            onDarkThemeChange,

                        onLogoutClick =
                            onLogoutClick
                    )
                }
            }
        ) {

            CompositionLocalProvider(
                LocalLayoutDirection provides
                        contentDirection
            ) {

                HomeContent(

                    accountInitial =
                        accountInitial,

                    walletUiState =
                        walletUiState,

                    recentTransfers =
                        recentTransfers,

                    appLanguage =
                        appLanguage,

                    onProfileClick = {

                        scope.launch {

                            drawerState.open()
                        }
                    },

                    onSendClick =
                        onSendClick,

                    onReceiveClick =
                        onReceiveClick,

                    onExchangeClick =
                        onExchangeClick,

                    onHistoryClick =
                        onHistoryClick
                )
            }
        }
    }
}

@Composable
private fun HomeContent(

    accountInitial: String,

    walletUiState: HomeUiState,

    recentTransfers:
        List<TransferHistoryItem>,

    appLanguage: String,

    onProfileClick: () -> Unit,

    onSendClick: () -> Unit,

    onReceiveClick: () -> Unit,

    onExchangeClick: () -> Unit,

    onHistoryClick: () -> Unit
) {

    var currentTime by
    remember {

        mutableStateOf(
            LocalDateTime.now(
                ZoneId.systemDefault()
            )
        )
    }

    LaunchedEffect(Unit) {

        while (true) {

            currentTime =
                LocalDateTime.now(
                    ZoneId.systemDefault()
                )

            delay(
                60_000L
            )
        }
    }

    val locale =
        remember(
            appLanguage
        ) {

            if (
                appLanguage == "en"
            ) {

                Locale.ENGLISH

            } else {

                Locale.forLanguageTag(
                    "ar"
                )
            }
        }

    /*
     * أقرب للشكل الذي أرسلته:
     *
     * English:
     * Wed, August 12
     *
     * Arabic:
     * الأربعاء، 12 أغسطس
     */
    val dateFormatter =
        remember(
            locale,
            appLanguage
        ) {

            DateTimeFormatter.ofPattern(
                if (
                    appLanguage == "en"
                ) {
                    "EEE, MMMM d"
                } else {
                    "EEEE، d MMMM"
                },
                locale
            )
        }

    /*
     * بدون ثواني كي لا تبدو الساعة مزدحمة.
     */
    val timeFormatter =
        remember {

            DateTimeFormatter.ofPattern(
                "HH:mm"
            )
        }

    val dateText =
        currentTime.format(
            dateFormatter
        )

    val timeText =
        currentTime.format(
            timeFormatter
        )

    val greetingText =
        if (
            currentTime.hour < 12
        ) {

            stringResource(
                R.string.good_morning
            )

        } else {

            stringResource(
                R.string.good_evening
            )
        }

    val isDayTime =
        currentTime.hour >= 6 &&
                currentTime.hour < 19

    var transactionSort by
    rememberSaveable {

        mutableStateOf(
            TransactionSort.NEWEST
        )
    }

    var sortMenuExpanded by
    remember {

        mutableStateOf(
            false
        )
    }

    val transactions =
        remember(
            recentTransfers
        ) {

            recentTransfers
                .mapNotNull {
                        transfer ->

                    val dateTime =
                        try {

                            transfer
                                .createdAt
                                ?.let {
                                        value ->

                                    LocalDateTime
                                        .ofInstant(
                                            Instant.parse(
                                                value
                                            ),

                                            ZoneId
                                                .systemDefault()
                                        )
                                }

                        } catch (
                            exception: Exception
                        ) {

                            null
                        }

                    if (
                        dateTime == null
                    ) {

                        null

                    } else {

                        DemoTransaction(

                            id =
                                transfer.id,

                            type =
                                if (
                                    transfer
                                        .direction
                                        .uppercase() ==
                                        "INCOMING"
                                ) {

                                    DemoTransactionType
                                        .INCOMING

                                } else {

                                    DemoTransactionType
                                        .OUTGOING
                                },

                            dateTime =
                                dateTime,

                            amount =
                                transfer
                                    .amount
                                    .toDoubleOrNull()
                                    ?: 0.0,

                            currency =
                                transfer
                                    .currency
                                    .uppercase(),

                            counterpartName =
                                transfer
                                    .recipientName
                        )
                    }
                }
        }

    val sortedTransactions =
        remember(
            transactions,
            transactionSort
        ) {

            when (
                transactionSort
            ) {

                TransactionSort.NEWEST ->

                    transactions.sortedByDescending {
                        it.dateTime
                    }

                TransactionSort.OLDEST ->

                    transactions.sortedBy {
                        it.dateTime
                    }

                TransactionSort.LARGEST ->

                    transactions.sortedByDescending {
                        it.amount
                    }

                TransactionSort.SMALLEST ->

                    transactions.sortedBy {
                        it.amount
                    }
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
                .padding(
                    horizontal =
                        18.dp
                )
    ) {

        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )

        HeaderSection(

            greetingText =
                greetingText,

            accountInitial =
                accountInitial,

            onProfileClick =
                onProfileClick
        )

        Spacer(
            modifier =
                Modifier.height(
                    14.dp
                )
        )

        BalanceCard(

            walletUiState =
                walletUiState,

            dateText =
                dateText,

            timeText =
                timeText,

            isDayTime =
                isDayTime
        )

        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )

        MainActions(

            onSendClick =
                onSendClick,

            onReceiveClick =
                onReceiveClick
        )

        Spacer(
            modifier =
                Modifier.height(
                    18.dp
                )
        )

        Text(
            text =
                stringResource(
                    R.string.services
                ),

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
                    9.dp
                )
        )

        ServicesSection(

            onExchangeClick =
                onExchangeClick,

            onHistoryClick =
                onHistoryClick
        )

        Spacer(
            modifier =
                Modifier.height(
                    14.dp
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
                    stringResource(
                        R.string.recent_transactions
                    ),

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

            Box {

                TextButton(
                    onClick = {

                        sortMenuExpanded =
                            true
                    }
                ) {

                    Text(
                        text =
                            "⇅",

                        fontWeight =
                            FontWeight.Bold,

                        fontSize =
                            18.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                5.dp
                            )
                    )

                    Text(
                        text =
                            sortLabel(
                                transactionSort
                            )
                    )
                }

                DropdownMenu(

                    expanded =
                        sortMenuExpanded,

                    onDismissRequest = {

                        sortMenuExpanded =
                            false
                    }
                ) {

                    TransactionSort
                        .entries
                        .forEach {
                                option ->

                            DropdownMenuItem(

                                text = {

                                    Text(
                                        text =
                                            sortLabel(
                                                option
                                            )
                                    )
                                },

                                onClick = {

                                    transactionSort =
                                        option

                                    sortMenuExpanded =
                                        false
                                }
                            )
                        }
                }
            }
        }

        /*
         * فقط العمليات تتحرك.
         */
        LazyColumn(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(
                        1f
                    ),

            contentPadding =
                PaddingValues(
                    top =
                        8.dp,

                    bottom =
                        20.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(
                    9.dp
                )
        ) {

            items(
                items =
                    sortedTransactions,

                key = {
                        transaction ->

                    transaction.id
                }
            ) {
                    transaction ->

                RecentTransaction(

                    transaction =
                        transaction,

                    currentTime =
                        currentTime,

                    locale =
                        locale
                )
            }
        }
    }
}

@Composable
private fun sortLabel(
    sort: TransactionSort
): String {

    return when (
        sort
    ) {

        TransactionSort.NEWEST ->

            stringResource(
                R.string.newest_first
            )

        TransactionSort.OLDEST ->

            stringResource(
                R.string.oldest_first
            )

        TransactionSort.LARGEST ->

            stringResource(
                R.string.largest_first
            )

        TransactionSort.SMALLEST ->

            stringResource(
                R.string.smallest_first
            )
    }
}

@Composable
private fun HeaderSection(

    greetingText: String,

    accountInitial: String,

    onProfileClick: () -> Unit
) {

    /*
     * HLRMS Cash كلمة إنجليزية،
     * لذلك نحافظ على ترتيبها LTR دائمًا.
     */
    CompositionLocalProvider(
        LocalLayoutDirection provides
                LayoutDirection.Ltr
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Surface(
                    modifier =
                        Modifier
                            .size(
                                54.dp
                            )
                            .clickable(
                                onClick =
                                    onProfileClick
                            ),

                    shape =
                        CircleShape,

                    color =
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                ) {

                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text =
                                accountInitial,

                            /*
                             * أكبر من السابق بوضوح.
                             */
                            fontSize =
                                28.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onPrimaryContainer
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.width(
                            12.dp
                        )
                )

                Text(
                    text =
                        "HLRMS Cash",

                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall,

                    fontWeight =
                        FontWeight.SemiBold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onBackground
                )
            }

            Text(
                text =
                    greetingText,

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
        }
    }
}

@Composable
private fun BalanceCard(

    walletUiState: HomeUiState,

    dateText: String,

    timeText: String,

    isDayTime: Boolean
) {

    val cardColor =
        MaterialTheme
            .colorScheme
            .primary

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                24.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    cardColor
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    horizontal =
                        18.dp,

                    vertical =
                        14.dp
                )
        ) {

            /*
             * أعلى البطاقة:
             *
             * Clock        Available balance
             */
            CompositionLocalProvider(
                LocalLayoutDirection provides
                        LayoutDirection.Ltr
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    TimeAndDateWidget(

                        dateText =
                            dateText,

                        timeText =
                            timeText,

                        isDayTime =
                            isDayTime,

                        cardColor =
                            cardColor
                    )

                    Text(
                        text =
                            stringResource(
                                R.string.available_balance
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
                                .onPrimary
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        17.dp
                    )
            )

            /*
             * الآن USD مثل باقي العملات:
             *
             * USD
             * 1,428
             */
            CompositionLocalProvider(
                LocalLayoutDirection provides
                        LayoutDirection.Ltr
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    CurrencyBalance(
                        modifier =
                            Modifier.weight(
                                1f
                            ),

                        currency =
                            "USD",

                        value =
                            formatBalance(
                                walletUiState
                                    .balances["USD"]
                            )
                    )

                    CurrencyBalance(
                        modifier =
                            Modifier.weight(
                                1f
                            ),

                        currency =
                            "EUR",

                        value =
                            formatBalance(
                                walletUiState
                                    .balances["EUR"]
                            )
                    )

                    CurrencyBalance(
                        modifier =
                            Modifier.weight(
                                1f
                            ),

                        currency =
                            "TRY",

                        value =
                            formatBalance(
                                walletUiState
                                    .balances["TRY"]
                            )
                    )

                    CurrencyBalance(
                        modifier =
                            Modifier.weight(
                                1f
                            ),

                        currency =
                            "SYP",

                        value =
                            formatBalance(
                                walletUiState
                                    .balances["SYP"]
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeAndDateWidget(

    dateText: String,

    timeText: String,

    isDayTime: Boolean,

    cardColor: Color
) {

    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        DayNightIcon(

            isDayTime =
                isDayTime,

            backgroundColor =
                cardColor
        )

        Spacer(
            modifier =
                Modifier.width(
                    10.dp
                )
        )

        Column {

            /*
             * أقرب لشكل ساعة الهاتف
             * في الصورة الثالثة.
             */
            Text(
                text =
                    timeText,

                color =
                    MaterialTheme
                        .colorScheme
                        .onPrimary,

                fontWeight =
                    FontWeight.Light,

                fontSize =
                    34.sp,

                lineHeight =
                    36.sp
            )

            Text(
                text =
                    dateText,

                color =
                    MaterialTheme
                        .colorScheme
                        .onPrimary
                        .copy(
                            alpha =
                                0.82f
                        ),

                fontSize =
                    12.sp,

                maxLines =
                    1
            )
        }
    }
}

@Composable
private fun DayNightIcon(

    isDayTime: Boolean,

    backgroundColor: Color
) {

    /*
     * اللون الأصفر مقصود للشمس والقمر.
     */
    val celestialColor =
        Color(
            0xFFFFC857
        )

    Canvas(
        modifier =
            Modifier.size(
                38.dp
            )
    ) {

        if (
            isDayTime
        ) {

            val radius =
                size.minDimension *
                        0.19f

            drawCircle(
                color =
                    celestialColor,

                radius =
                    radius,

                center =
                    center
            )

            val startRadius =
                size.minDimension *
                        0.30f

            val endRadius =
                size.minDimension *
                        0.43f

            repeat(8) {
                    index ->

                val angle =
                    Math.toRadians(
                        index *
                                45.0
                    )

                drawLine(

                    color =
                        celestialColor,

                    start =
                        Offset(
                            x =
                                center.x +
                                        cos(
                                            angle
                                        ).toFloat() *
                                        startRadius,

                            y =
                                center.y +
                                        sin(
                                            angle
                                        ).toFloat() *
                                        startRadius
                        ),

                    end =
                        Offset(
                            x =
                                center.x +
                                        cos(
                                            angle
                                        ).toFloat() *
                                        endRadius,

                            y =
                                center.y +
                                        sin(
                                            angle
                                        ).toFloat() *
                                        endRadius
                        ),

                    strokeWidth =
                        2.2.dp.toPx()
                )
            }

        } else {

            val radius =
                size.minDimension *
                        0.31f

            /*
             * دائرة صفراء.
             */
            drawCircle(
                color =
                    celestialColor,

                radius =
                    radius,

                center =
                    center
            )

            /*
             * نقص دائرة بلون البطاقة
             * لنحصل على هلال أصفر.
             */
            drawCircle(
                color =
                    backgroundColor,

                radius =
                    radius *
                            0.91f,

                center =
                    Offset(
                        x =
                            center.x +
                                    radius *
                                    0.55f,

                        y =
                            center.y -
                                    radius *
                                    0.13f
                    )
            )

            /*
             * نجمتان صغيرتان.
             */
            drawCircle(
                color =
                    celestialColor,

                radius =
                    1.5.dp.toPx(),

                center =
                    Offset(
                        x =
                            size.width *
                                    0.82f,

                        y =
                            size.height *
                                    0.23f
                    )
            )

            drawCircle(
                color =
                    celestialColor
                        .copy(
                            alpha =
                                0.7f
                        ),

                radius =
                    1.dp.toPx(),

                center =
                    Offset(
                        x =
                            size.width *
                                    0.87f,

                        y =
                            size.height *
                                    0.45f
                    )
            )
        }
    }
}

private fun formatBalance(
    balance: BigDecimal?
): String {

    if (balance == null) {
        return "—"
    }

    val formatter =
        NumberFormat
            .getNumberInstance(
                Locale.US
            )
            .apply {
                isGroupingUsed = true
                minimumFractionDigits = 0
                maximumFractionDigits = 2
            }

    return formatter.format(
        balance
    )
}

@Composable
private fun CurrencyBalance(

    modifier: Modifier,

    currency: String,

    value: String
) {

    Column(
        modifier =
            modifier,

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text =
                currency,

            color =
                MaterialTheme
                    .colorScheme
                    .onPrimary
                    .copy(
                        alpha =
                            0.67f
                    ),

            style =
                MaterialTheme
                    .typography
                    .labelMedium,

            fontWeight =
                FontWeight.SemiBold
        )

        Spacer(
            modifier =
                Modifier.height(
                    2.dp
                )
        )

        Text(
            text =
                value,

            color =
                MaterialTheme
                    .colorScheme
                    .onPrimary,

            fontWeight =
                FontWeight.Bold,

            style =
                MaterialTheme
                    .typography
                    .bodyLarge,

            maxLines =
                1
        )
    }
}

@Composable
private fun MainActions(

    onSendClick: () -> Unit,

    onReceiveClick: () -> Unit
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.spacedBy(
                12.dp
            )
    ) {

        Button(
            onClick =
                onSendClick,

            modifier =
                Modifier
                    .weight(
                        1f
                    )
                    .height(
                        54.dp
                    ),

            shape =
                RoundedCornerShape(
                    17.dp
                )
        ) {

            Text(
                text =
                    stringResource(
                        R.string.send
                    ),

                fontWeight =
                    FontWeight.Bold
            )
        }

        Button(
            onClick =
                onReceiveClick,

            modifier =
                Modifier
                    .weight(
                        1f
                    )
                    .height(
                        54.dp
                    ),

            shape =
                RoundedCornerShape(
                    17.dp
                ),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .secondary,

                    contentColor =
                        MaterialTheme
                            .colorScheme
                            .onSecondary
                )
        ) {

            Text(
                text =
                    stringResource(
                        R.string.receive
                    ),

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ServicesSection(

    onExchangeClick: () -> Unit,

    onHistoryClick: () -> Unit
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.spacedBy(
                12.dp
            )
    ) {

        ServiceCard(

            title =
                stringResource(
                    R.string.exchange_rates
                ),

            subtitle =
                stringResource(
                    R.string.exchange_rates_subtitle
                ),

            symbol =
                "↗",

            modifier =
                Modifier.weight(
                    1f
                ),

            onClick =
                onExchangeClick
        )

        ServiceCard(

            title =
                stringResource(
                    R.string.transfer_history
                ),

            subtitle =
                stringResource(
                    R.string.all_transactions
                ),

            symbol =
                "↕",

            modifier =
                Modifier.weight(
                    1f
                ),

            onClick =
                onHistoryClick
        )
    }
}

@Composable
private fun ServiceCard(

    title: String,

    subtitle: String,

    symbol: String,

    modifier: Modifier = Modifier,

    onClick: () -> Unit
) {

    Card(
        onClick =
            onClick,

        modifier =
            modifier.height(
                108.dp
            ),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        14.dp
                    ),

            verticalArrangement =
                Arrangement.SpaceBetween
        ) {

            Surface(
                modifier =
                    Modifier.size(
                        32.dp
                    ),

                shape =
                    CircleShape,

                color =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
            ) {

                Box(
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            symbol,

                        color =
                            MaterialTheme
                                .colorScheme
                                .primary,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            Column {

                Text(
                    text =
                        title,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface,

                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge,

                    maxLines =
                        1
                )

                Text(
                    text =
                        subtitle,

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,

                    maxLines =
                        1
                )
            }
        }
    }
}

@Composable
private fun RecentTransaction(

    transaction: DemoTransaction,

    currentTime: LocalDateTime,

    locale: Locale
) {

    val title =
        when (
            transaction.type
        ) {

            DemoTransactionType.OUTGOING ->

                stringResource(
                    R.string.outgoing_transfer
                )

            DemoTransactionType.INCOMING ->

                stringResource(
                    R.string.incoming_transfer
                )

            DemoTransactionType.EXCHANGE ->

                stringResource(
                    R.string.currency_exchange
                )
        }

    val daysDifference =
        ChronoUnit.DAYS.between(
            transaction
                .dateTime
                .toLocalDate(),

            currentTime
                .toLocalDate()
        )

    val datePart =
        when (
            daysDifference
        ) {

            0L ->
                stringResource(
                    R.string.today
                )

            1L ->
                stringResource(
                    R.string.yesterday
                )

            else ->
                transaction
                    .dateTime
                    .format(
                        DateTimeFormatter
                            .ofPattern(
                                "d MMMM",
                                locale
                            )
                    )
        }

    val timePart =
        transaction
            .dateTime
            .format(
                DateTimeFormatter.ofPattern(
                    "HH:mm"
                )
            )

    val prefix =
        when (
            transaction.type
        ) {

            DemoTransactionType.OUTGOING ->
                "-"

            DemoTransactionType.INCOMING ->
                "+"

            DemoTransactionType.EXCHANGE ->
                ""
        }

    val amountText =
        if (
            transaction.amount %
            1.0 ==
            0.0
        ) {

            transaction
                .amount
                .toInt()
                .toString()

        } else {

            transaction
                .amount
                .toString()
        }

    val amountColor =
        when (
            transaction.type
        ) {

            DemoTransactionType.OUTGOING ->

                MaterialTheme
                    .colorScheme
                    .error

            DemoTransactionType.INCOMING ->

                MaterialTheme
                    .colorScheme
                    .secondary

            DemoTransactionType.EXCHANGE ->

                MaterialTheme
                    .colorScheme
                    .primary
        }

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                16.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal =
                            15.dp,

                        vertical =
                            13.dp
                    ),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column {

                Text(
                    text =
                        title,

                    fontWeight =
                        FontWeight.SemiBold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface
                )

                Text(
                    text =
                        if (
                            transaction
                                .counterpartName
                                .isNotBlank()
                        ) {

                            "${transaction.counterpartName} • $datePart • $timePart"

                        } else {

                            "$datePart • $timePart"
                        },

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

            Text(
                text =
                    "$prefix $amountText ${transaction.currency}"
                        .trim(),

                color =
                    amountColor,

                fontWeight =
                    FontWeight.Bold,

                textAlign =
                    TextAlign.End
            )
        }
    }
}

@Composable
private fun AccountDrawer(

    accountDisplayName: String,

    accountInitial: String,

    appLanguage: String,

    onLanguageChange: (
        String
    ) -> Unit,

    isDarkTheme: Boolean,

    onDarkThemeChange: (
        Boolean
    ) -> Unit,

    onLogoutClick: () -> Unit
) {

    ModalDrawerSheet(
        drawerContainerColor =
            MaterialTheme
                .colorScheme
                .surface
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        22.dp
                    )
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        34.dp
                    )
            )

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Surface(
                    modifier =
                        Modifier.size(
                            60.dp
                        ),

                    shape =
                        CircleShape,

                    color =
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                ) {

                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text =
                                accountInitial,

                            /*
                             * أكبر من S الحالية.
                             */
                            fontWeight =
                                FontWeight.Bold,

                            fontSize =
                                30.sp,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onPrimaryContainer
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.width(
                            14.dp
                        )
                )

                Column {

                    Text(
                        text =
                            "HLRMS Cash",

                        fontWeight =
                            FontWeight.Bold,

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface
                    )

                    Text(
                        text =
                            accountDisplayName
                                .ifBlank {

                                    stringResource(
                                        R.string.account
                                    )
                                },

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,

                        maxLines =
                            1
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )

            HorizontalDivider()

            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )

            Text(
                text =
                    stringResource(
                        R.string.settings
                    ),

                fontWeight =
                    FontWeight.Bold,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical =
                                8.dp
                        ),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column {

                    Text(
                        text =
                            stringResource(
                                R.string.dark_mode
                            ),

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface
                    )

                    Text(
                        text =
                            if (
                                isDarkTheme
                            ) {

                                stringResource(
                                    R.string.dark_mode_enabled
                                )

                            } else {

                                stringResource(
                                    R.string.light_mode_enabled
                                )
                            },

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

                Switch(
                    checked =
                        isDarkTheme,

                    onCheckedChange =
                        onDarkThemeChange
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            Text(
                text =
                    stringResource(
                        R.string.language
                    ),

                fontWeight =
                    FontWeight.Bold,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
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

                FilterChip(
                    selected =
                        appLanguage ==
                                "ar",

                    onClick = {

                        onLanguageChange(
                            "ar"
                        )
                    },

                    label = {

                        Text(
                            text =
                                "العربية"
                        )
                    }
                )

                FilterChip(
                    selected =
                        appLanguage ==
                                "en",

                    onClick = {

                        onLanguageChange(
                            "en"
                        )
                    },

                    label = {

                        Text(
                            text =
                                "English"
                        )
                    }
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            DrawerInformationRow(
                title =
                    stringResource(
                        R.string.account_information
                    )
            )

            DrawerInformationRow(
                title =
                    stringResource(
                        R.string.security_privacy
                    )
            )

            DrawerInformationRow(
                title =
                    stringResource(
                        R.string.help_support
                    )
            )

            Spacer(
                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            HorizontalDivider()

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )

            Button(
                onClick =
                    onLogoutClick,

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(
                        16.dp
                    ),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
            ) {

                Text(
                    text =
                        stringResource(
                            R.string.logout
                        )
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )

            Text(
                text =
                    "HLRMS Cash",

                modifier =
                    Modifier.fillMaxWidth(),

                textAlign =
                    TextAlign.Center,

                style =
                    MaterialTheme
                        .typography
                        .labelSmall,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DrawerInformationRow(
    title: String
) {

    TextButton(
        onClick = {},

        modifier =
            Modifier.fillMaxWidth()
    ) {

        Text(
            text =
                title,

            modifier =
                Modifier.fillMaxWidth(),

            textAlign =
                TextAlign.Start,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurface
        )
    }
}