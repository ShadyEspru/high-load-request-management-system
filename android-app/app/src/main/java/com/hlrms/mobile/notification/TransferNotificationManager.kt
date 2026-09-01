package com.hlrms.mobile.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

import com.hlrms.mobile.R
import com.hlrms.mobile.ui.history.TransferHistoryItem

object TransferNotificationManager {

    private const val CHANNEL_ID =
        "incoming_transfers"

    private const val PREFERENCES_NAME =
        "transfer_notifications"

    fun createChannel(
        context: Context
    ) {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.O
        ) {
            return
        }

        val channel =
            NotificationChannel(
                CHANNEL_ID,

                context.getString(
                    R.string.transfer_notification_channel
                ),

                NotificationManager.IMPORTANCE_HIGH
            ).apply {

                description =
                    context.getString(
                        R.string.transfer_notification_channel_description
                    )
            }

        val notificationManager =
            context.getSystemService(
                NotificationManager::class.java
            )

        notificationManager.createNotificationChannel(
            channel
        )
    }

    fun processHistory(
        context: Context,
        profileKey: String?,
        items: List<TransferHistoryItem>
    ) {

        if (
            profileKey.isNullOrBlank()
        ) {
            return
        }

        val latestIncoming =
            items
                .filter {
                    it.direction
                        .uppercase() ==
                        "INCOMING"
                }
                .maxByOrNull {
                    it.createdAt.orEmpty()
                }
                ?: return

        val preferences =
            context.getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )

        val preferenceKey =
            "last_incoming_$profileKey"

        val previousTransactionId =
            preferences.getString(
                preferenceKey,
                null
            )

        /*
         * أول تحميل للحساب:
         * نحفظ آخر حوالة فقط،
         * ولا نرسل إشعارات عن حوالات قديمة.
         */
        if (
            previousTransactionId == null
        ) {

            preferences
                .edit()
                .putString(
                    preferenceKey,
                    latestIncoming.id
                )
                .apply()

            return
        }

        /*
         * لا نكرر إشعار نفس الحوالة.
         */
        if (
            previousTransactionId ==
            latestIncoming.id
        ) {
            return
        }

        showIncomingNotification(
            context =
                context,

            item =
                latestIncoming
        )

        preferences
            .edit()
            .putString(
                preferenceKey,
                latestIncoming.id
            )
            .apply()
    }

    private fun showIncomingNotification(
        context: Context,
        item: TransferHistoryItem
    ) {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val senderName =
            item.recipientName
                .ifBlank {
                    "HLRMS Cash"
                }

        val notification =
            NotificationCompat
                .Builder(
                    context,
                    CHANNEL_ID
                )
                .setSmallIcon(
                    R.drawable.ic_notification_transfer
                )
                .setContentTitle(
                    context.getString(
                        R.string.incoming_transfer_notification_title
                    )
                )
                .setContentText(
                    context.getString(
                        R.string.incoming_transfer_notification_body,
                        item.amount,
                        item.currency,
                        senderName
                    )
                )
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setAutoCancel(true)
                .build()

        NotificationManagerCompat
            .from(context)
            .notify(
                item.id.hashCode(),
                notification
            )
    }
}
