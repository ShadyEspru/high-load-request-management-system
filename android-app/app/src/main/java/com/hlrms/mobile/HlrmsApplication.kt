package com.hlrms.mobile

import android.app.Application
import com.hlrms.mobile.data.remote.ApiClient
import com.hlrms.mobile.notification.TransferNotificationManager

class HlrmsApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        ApiClient.initialize(this)

        TransferNotificationManager
            .createChannel(this)
    }
}