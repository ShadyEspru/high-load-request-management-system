package com.hlrms.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hlrms.mobile.ui.auth.LoginScreen
import com.hlrms.mobile.ui.auth.RegisterScreen
import com.hlrms.mobile.ui.home.HomeScreen
import com.hlrms.mobile.ui.splash.SplashScreen

@Composable
fun HlrmsNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Splash.route
    ) {
        composable(AppDestination.Splash.route) {
            SplashScreen(
                onFinished = {
                    navController.navigate(
                        AppDestination.Login.route
                    ) {
                        popUpTo(AppDestination.Splash.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(AppDestination.Login.route) {
            LoginScreen(
                onLoginClick = {
                    navController.navigate(
                        AppDestination.Home.route
                    ) {
                        popUpTo(AppDestination.Login.route) {
                            inclusive = true
                        }
                    }
                },
                onRegisterClick = {
                    navController.navigate(
                        AppDestination.Register.route
                    )
                }
            )
        }

        composable(AppDestination.Register.route) {
            RegisterScreen(
                onRegisterClick = {
                    navController.navigate(
                        AppDestination.Home.route
                    ) {
                        popUpTo(AppDestination.Login.route) {
                            inclusive = true
                        }
                    }
                },
                onBackToLoginClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(AppDestination.Home.route) {
            HomeScreen(
                onLogoutClick = {
                    navController.navigate(
                        AppDestination.Login.route
                    ) {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}