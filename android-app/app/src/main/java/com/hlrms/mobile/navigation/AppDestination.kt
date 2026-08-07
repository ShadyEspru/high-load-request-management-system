package com.hlrms.mobile.navigation

sealed class AppDestination(
    val route: String
) {
    data object Splash : AppDestination("splash")
    data object Login : AppDestination("login")
    data object Register : AppDestination("register")
    data object Home : AppDestination("home")
}