package com.hlrms.mobile

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.hlrms.mobile.data.local.AppLocaleManager
import com.hlrms.mobile.data.local.UiPreferences
import com.hlrms.mobile.navigation.HlrmsNavHost
import com.hlrms.mobile.ui.theme.HLRMSTheme
import kotlinx.coroutines.launch

class MainActivity :
    ComponentActivity() {

    override fun attachBaseContext(
        newBase: Context
    ) {

        super.attachBaseContext(
            AppLocaleManager.wrapContext(
                newBase
            )
        )
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()

        setContent {

            val systemDarkMode =
                isSystemInDarkTheme()

            val uiPreferences =
                remember {

                    UiPreferences(
                        applicationContext
                    )
                }

            val savedDarkMode by
            uiPreferences
                .darkMode
                .collectAsState(
                    initial = null
                )

            val darkMode =
                savedDarkMode
                    ?: systemDarkMode

            val scope =
                rememberCoroutineScope()

            val appLanguage =
                resources
                    .configuration
                    .locales[0]
                    .language
                    .let { language ->

                        if (
                            language ==
                            AppLocaleManager.ENGLISH
                        ) {

                            AppLocaleManager.ENGLISH

                        } else {

                            AppLocaleManager.ARABIC
                        }
                    }

            HLRMSTheme(
                darkTheme =
                    darkMode
            ) {

                HlrmsNavHost(

                    appLanguage =
                        appLanguage,

                    onLanguageChange = {
                            language ->

                        if (
                            language !=
                            appLanguage
                        ) {

                            AppLocaleManager
                                .setLanguage(
                                    this@MainActivity,
                                    language
                                )

                            recreate()
                        }
                    },

                    isDarkTheme =
                        darkMode,

                    onDarkThemeChange = {
                            enabled ->

                        scope.launch {

                            uiPreferences
                                .setDarkMode(
                                    enabled
                                )
                        }
                    }
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.TIRAMISU
        ) {
            return
        }

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ActivityCompat.requestPermissions(
            this,

            arrayOf(
                Manifest.permission.POST_NOTIFICATIONS
            ),

            1001
        )
    }

}