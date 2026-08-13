package com.hlrms.mobile.ui.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SplashScreen(
    isSessionChecked: Boolean,
    isAuthenticated: Boolean,
    onAuthenticated: () -> Unit,
    onUnauthenticated: () -> Unit
) {
    LaunchedEffect(
        isSessionChecked,
        isAuthenticated
    ) {
        if (!isSessionChecked) {
            return@LaunchedEffect
        }

        if (isAuthenticated) {
            onAuthenticated()
        } else {
            onUnauthenticated()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {
        Text(
            text = "HLRMS",
            style =
                MaterialTheme.typography
                    .displaySmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text =
                "High-Load Financial Transfer System",
            style =
                MaterialTheme.typography
                    .bodyLarge
        )

        CircularProgressIndicator(
            modifier =
                Modifier.padding(top = 24.dp)
        )
    }
}