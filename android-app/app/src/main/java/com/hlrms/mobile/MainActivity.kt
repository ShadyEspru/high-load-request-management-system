package com.hlrms.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hlrms.mobile.navigation.HlrmsNavHost
import com.hlrms.mobile.ui.theme.HLRMSTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            HLRMSTheme {
                HlrmsNavHost()
            }
        }
    }
}