package com.hlrms.mobile.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme =
    darkColorScheme(

        primary =
            HlrmsBlueDark,

        onPrimary =
            HlrmsOnBlueDark,

        primaryContainer =
            HlrmsBlueContainerDark,

        onPrimaryContainer =
            HlrmsOnBlueContainerDark,

        secondary =
            HlrmsGreenDark,

        onSecondary =
            HlrmsOnGreenDark,

        background =
            HlrmsBackgroundDark,

        onBackground =
            HlrmsOnBackgroundDark,

        surface =
            HlrmsSurfaceDark,

        onSurface =
            HlrmsOnSurfaceDark,

        surfaceVariant =
            HlrmsSurfaceVariantDark,

        onSurfaceVariant =
            HlrmsOnSurfaceVariantDark,

        outline =
            HlrmsOutlineDark,

        error =
            HlrmsErrorDark
    )

private val LightColorScheme =
    lightColorScheme(

        primary =
            HlrmsBlueLight,

        onPrimary =
            HlrmsOnBlueLight,

        primaryContainer =
            HlrmsBlueContainerLight,

        onPrimaryContainer =
            HlrmsOnBlueContainerLight,

        secondary =
            HlrmsGreenLight,

        onSecondary =
            HlrmsOnGreenLight,

        background =
            HlrmsBackgroundLight,

        onBackground =
            HlrmsOnBackgroundLight,

        surface =
            HlrmsSurfaceLight,

        onSurface =
            HlrmsOnSurfaceLight,

        surfaceVariant =
            HlrmsSurfaceVariantLight,

        onSurfaceVariant =
            HlrmsOnSurfaceVariantLight,

        outline =
            HlrmsOutlineLight,

        error =
            HlrmsErrorLight
    )

@Composable
fun HLRMSTheme(
    darkTheme: Boolean =
        isSystemInDarkTheme(),

    content: @Composable () -> Unit
) {

    val colorScheme =
        if (darkTheme) {
            DarkColorScheme
        } else {
            LightColorScheme
        }

    /*
     * تحديث لون أيقونات شريط النظام
     * عند تغيير Light / Dark أثناء تشغيل التطبيق.
     */
    val view =
        LocalView.current

    if (!view.isInEditMode) {

        SideEffect {

            val window =
                (view.context as Activity)
                    .window

            val controller =
                WindowCompat
                    .getInsetsController(
                        window,
                        view
                    )

            controller
                .isAppearanceLightStatusBars =
                !darkTheme

            controller
                .isAppearanceLightNavigationBars =
                !darkTheme
        }
    }

    MaterialTheme(
        colorScheme =
            colorScheme,

        typography =
            Typography,

        content =
            content
    )
}