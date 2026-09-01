package com.hlrms.mobile.ui.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable

@Composable
fun authTextFieldColors() =
    OutlinedTextFieldDefaults.colors(

        focusedTextColor =
            MaterialTheme.colorScheme.onSurface,

        unfocusedTextColor =
            MaterialTheme.colorScheme.onSurface,

        disabledTextColor =
            MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.60f
            ),

        focusedLabelColor =
            MaterialTheme.colorScheme.primary,

        unfocusedLabelColor =
            MaterialTheme.colorScheme.onSurfaceVariant,

        focusedBorderColor =
            MaterialTheme.colorScheme.primary,

        unfocusedBorderColor =
            MaterialTheme.colorScheme.outline,

        cursorColor =
            MaterialTheme.colorScheme.primary
    )
