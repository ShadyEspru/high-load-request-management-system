package com.hlrms.mobile.navigation

sealed class AppDestination(
    val route: String
) {

    data object Splash :
        AppDestination("splash")

    data object Login :
        AppDestination("login")

    data object Register :
        AppDestination("register")

    data object Home :
        AppDestination("home")

    data object SendQr :
        AppDestination("send-qr")

    data object SendMoney :
        AppDestination("send-money")

    data object ReceiveMoney :
        AppDestination("receive-money")

    data object ExchangeRates :
        AppDestination("exchange-rates")

    data object TransferHistory :
        AppDestination("transfer-history")

    data object RequestStatus :
        AppDestination("request-status/{requestId}") {

        fun createRoute(
            requestId: String
        ): String {

            return "request-status/$requestId"
        }
    }
}
