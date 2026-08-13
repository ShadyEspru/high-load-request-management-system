package com.hlrms.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hlrms.mobile.ui.auth.AuthViewModel
import com.hlrms.mobile.ui.auth.LoginScreen
import com.hlrms.mobile.ui.auth.RegisterScreen
import com.hlrms.mobile.ui.home.HomeScreen
import com.hlrms.mobile.ui.home.HomeViewModel
import com.hlrms.mobile.ui.exchange.ExchangeRatesScreen
import com.hlrms.mobile.ui.exchange.ExchangeRatesViewModel
import com.hlrms.mobile.ui.history.TransferHistoryScreen
import com.hlrms.mobile.ui.history.TransferHistoryViewModel
import com.hlrms.mobile.ui.splash.SplashScreen
import com.hlrms.mobile.ui.transfer.SendMoneyScreen
import com.hlrms.mobile.ui.transfer.TransferViewModel
import com.hlrms.mobile.ui.receive.ReceiveMoneyScreen
import com.hlrms.mobile.ui.receive.ReceiveViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hlrms.mobile.notification.TransferNotificationManager
import com.hlrms.mobile.ui.transfer.TransferStatusScreen
import com.hlrms.mobile.ui.transfer.TransferStatusViewModel
import com.hlrms.mobile.ui.transfer.QrScannerScreen

@Composable
fun HlrmsNavHost(
    appLanguage: String,
    onLanguageChange: (String) -> Unit,
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    navController: NavHostController =
        rememberNavController()
) {
    val authViewModel: AuthViewModel = viewModel()

    val authUiState by
    authViewModel.uiState.collectAsState()

    val transferViewModel:
            TransferViewModel = viewModel()

    val transferUiState by
    transferViewModel.uiState.collectAsState()

    val receiveViewModel:
            ReceiveViewModel = viewModel()

    val receiveUiState by
    receiveViewModel.uiState.collectAsState()

    val homeViewModel:
            HomeViewModel = viewModel()

    val homeUiState by
    homeViewModel.uiState.collectAsState()


    val homeHistoryViewModel:
            TransferHistoryViewModel =
        viewModel()

    val homeHistoryUiState by
    homeHistoryViewModel
        .uiState
        .collectAsState()

    NavHost(
        navController = navController,
        startDestination = AppDestination.Splash.route,

        enterTransition = {
            EnterTransition.None
        },

        exitTransition = {
            ExitTransition.None
        },

        popEnterTransition = {
            EnterTransition.None
        },

        popExitTransition = {
            ExitTransition.None
        }
    ) {
        composable(
            AppDestination.Splash.route
        ) {
            SplashScreen(
                isSessionChecked =
                    authUiState.isSessionChecked,

                isAuthenticated =
                    authUiState.isAuthenticated,

                onAuthenticated = {
                    navController.navigate(
                        AppDestination.Home.route
                    ) {
                        popUpTo(
                            AppDestination.Splash.route
                        ) {
                            inclusive = true
                        }
                    }
                },

                onUnauthenticated = {
                    navController.navigate(
                        AppDestination.Login.route
                    ) {
                        popUpTo(
                            AppDestination.Splash.route
                        ) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(
            AppDestination.Login.route
        ) {
            LoginScreen(
                uiState = authUiState,

                onLoginClick = { email, password ->
                    authViewModel.login(
                        email = email,
                        password = password
                    )
                },

                onRegisterClick = {
                    authViewModel.clearState()

                    navController.navigate(
                        AppDestination.Register.route
                    )
                },

                onLoginSucceeded = {
                    authViewModel.clearState()

                    navController.navigate(
                        AppDestination.Home.route
                    ) {
                        popUpTo(
                            AppDestination.Login.route
                        ) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(
            AppDestination.Register.route
        ) {
            RegisterScreen(
                uiState = authUiState,

                onRegisterClick = {
                        email,
                        password,
                        firstName,
                        lastName ->

                    authViewModel.register(
                        email = email,
                        password = password,
                        firstName = firstName,
                        lastName = lastName
                    )
                },

                onBackToLoginClick = {
                    authViewModel.clearState()

                    navController.popBackStack()
                },

                onRegistrationSucceeded = {
                    authViewModel.clearState()

                    navController.navigate(
                        AppDestination.Home.route
                    ) {
                        popUpTo(
                            AppDestination.Register.route
                        ) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(
            AppDestination.TransferHistory.route
        ) {

            val historyViewModel:
                    TransferHistoryViewModel =
                viewModel()

            val historyUiState by
            historyViewModel
                .uiState
                .collectAsState()

            LaunchedEffect(Unit) {

                historyViewModel
                    .loadHistory()
            }

            TransferHistoryScreen(
                uiState =
                    historyUiState,

                onRetry = {

                    historyViewModel
                        .loadHistory()
                },

                onBackClick = {

                    navController
                        .popBackStack()
                }
            )
        }

        composable(
            AppDestination.Home.route
        ) {

            val lifecycleOwner =
                LocalLifecycleOwner.current

            val context =
                LocalContext.current

            DisposableEffect(
                lifecycleOwner
            ) {

                val observer =
                    LifecycleEventObserver {
                            _,
                            event ->

                        if (
                            event ==
                            Lifecycle.Event.ON_RESUME
                        ) {

                            receiveViewModel
                                .loadProfile()

                            homeViewModel
                                .loadWallet()

                            homeHistoryViewModel
                                .loadHistory()
                        }
                    }

                lifecycleOwner
                    .lifecycle
                    .addObserver(
                        observer
                    )

                onDispose {

                    lifecycleOwner
                        .lifecycle
                        .removeObserver(
                            observer
                        )
                }
            }

            LaunchedEffect(Unit) {

                receiveViewModel
                    .loadProfile()

                homeViewModel
                    .loadWallet()

                homeHistoryViewModel
                    .loadHistory()
            }

            LaunchedEffect(
                homeHistoryUiState.items,
                receiveUiState.profile?.transferId
            ) {

                if (
                    !homeHistoryUiState.isLoading &&
                    !homeHistoryUiState.hasError
                ) {

                    TransferNotificationManager
                        .processHistory(
                            context =
                                context,

                            profileKey =
                                receiveUiState
                                    .profile
                                    ?.transferId,

                            items =
                                homeHistoryUiState
                                    .items
                        )
                }
            }

            HomeScreen(
                accountDisplayName =
                    receiveUiState
                        .profile
                        ?.displayName
                        .orEmpty(),

                walletUiState =
                    homeUiState,

                recentTransfers =
                    homeHistoryUiState
                        .items,

                appLanguage =
                    appLanguage,

                onLanguageChange =
                    onLanguageChange,

                isDarkTheme =
                    isDarkTheme,

                onDarkThemeChange =
                    onDarkThemeChange,

                onLogoutClick = {

                    homeViewModel.clear()

                    homeHistoryViewModel
                        .clear()

                    authViewModel.logout {

                        navController.navigate(
                            AppDestination.Login.route
                        ) {
                            popUpTo(
                                navController.graph.id
                            ) {
                                inclusive = true
                            }
                        }
                    }
                },

                onSendClick = {

                    transferViewModel.clearState()

                    navController.navigate(
                        AppDestination.SendQr.route
                    )
                },

                onReceiveClick = {

                    receiveViewModel.clear()

                    navController.navigate(
                        AppDestination.ReceiveMoney.route
                    )
                },

                  onExchangeClick = {

                      navController.navigate(
                          AppDestination.ExchangeRates.route
                      )
                  },

                  onHistoryClick = {

                      navController.navigate(
                          AppDestination.TransferHistory.route
                      )
                  }
            )
        }

        composable(
            AppDestination.ExchangeRates.route
        ) {

            val exchangeViewModel:
                    ExchangeRatesViewModel =
                viewModel()

            val exchangeUiState by
            exchangeViewModel
                .uiState
                .collectAsState()

            LaunchedEffect(Unit) {

                exchangeViewModel
                    .loadRates()
            }

            ExchangeRatesScreen(
                uiState =
                    exchangeUiState,

                onRetry = {

                    exchangeViewModel
                        .loadRates()
                },

                onBackClick = {

                    navController
                        .popBackStack()
                }
            )
        }

        composable(
            AppDestination.SendQr.route
        ) {

            QrScannerScreen(

                onBackClick = {

                    navController
                        .popBackStack()
                },

                onSearchByIdClick = {

                    transferViewModel
                        .clearState()

                    navController.navigate(
                        AppDestination.SendMoney.route
                    )
                },

                onTransferIdDetected = {
                        transferId ->

                    transferViewModel
                        .clearState()

                    transferViewModel
                        .findRecipient(
                            transferId
                        )

                    navController.navigate(
                        AppDestination.SendMoney.route
                    ) {

                        popUpTo(
                            AppDestination.SendQr.route
                        ) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(
            AppDestination.SendMoney.route
        ) {
            SendMoneyScreen(
                uiState =
                    transferUiState,

                onFindRecipient = {
                        transferId ->

                    transferViewModel
                        .findRecipient(
                            transferId
                        )
                },

                onClearRecipient = {
                    transferViewModel
                        .clearRecipient()
                },

                onSendClick = {
                        amount,
                        currency ->

                    transferViewModel
                        .sendTransfer(
                            amount = amount,
                            currency = currency
                        )
                },

                onRequestCreated = { requestId ->

                    navController.navigate(
                        AppDestination.RequestStatus
                            .createRoute(
                                requestId
                            )
                    ) {
                        popUpTo(
                            AppDestination.SendMoney.route
                        ) {
                            inclusive = true
                        }
                    }
                },

                onBackClick = {
                    transferViewModel.clearState()
                    navController.popBackStack()
                }
            )
        }

        composable(
            AppDestination.RequestStatus.route
        ) { backStackEntry ->

            val requestId =
                backStackEntry
                    .arguments
                    ?.getString("requestId")
                    ?: return@composable

            val statusViewModel:
                    TransferStatusViewModel =
                viewModel()

            val statusUiState by
            statusViewModel
                .uiState
                .collectAsState()

            LaunchedEffect(requestId) {

                statusViewModel
                    .startMonitoring(
                        requestId
                    )
            }

            TransferStatusScreen(
                uiState =
                    statusUiState,

                onRetry = {
                    statusViewModel.retry()
                },

                onDone = {

                    transferViewModel
                        .clearState()

                    homeViewModel
                        .loadWallet()

                    homeHistoryViewModel
                        .loadHistory()

                    navController.navigate(
                        AppDestination.Home.route
                    ) {
                        popUpTo(
                            AppDestination.Home.route
                        ) {
                            inclusive = false
                        }

                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            AppDestination.ReceiveMoney.route
        ) {

            ReceiveMoneyScreen(

                uiState =
                    receiveUiState,

                onLoadProfile = {

                    receiveViewModel
                        .loadProfile()
                },

                onBackClick = {

                    receiveViewModel
                        .clear()

                    navController
                        .popBackStack()
                }
            )
        }
    }
}
