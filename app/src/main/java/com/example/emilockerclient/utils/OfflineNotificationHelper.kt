package com.example.emilockerclient.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.emilockerclient.R

/**
 * Helper class to manage offline warning notifications.
 * Shows persistent notification when device is offline for 24+ hours.
 */
object OfflineNotificationHelper {

    private const val CHANNEL_ID = "offline_warning_channel"
    private const val CHANNEL_NAME = "Offline Warning"
    private const val NOTIFICATION_ID = 1001

    /**
     * Create notification channel (required for Android 8.0+)
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when device has been offline for too long"
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show persistent offline warning notification
     * @param hoursOffline Number of hours the device has been offline
     */
    fun showOfflineWarningNotification(context: Context, hoursOffline: Long) {
        createNotificationChannel(context)

        // Intent to open WiFi/Network settings
        val settingsIntent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning) // Use system warning icon
            .setContentTitle("⚠️ Internet Connection Required")
            .setContentText("Device offline for $hoursOffline hours. Connect to avoid lock.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Your device has been offline for $hoursOffline hours.\n\n" +
                                "Please connect to the internet immediately to avoid automatic device lock.\n\n" +
                                "⏰ Device will be locked if offline for 48 hours."
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true) // Makes it persistent (cannot be dismissed)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        android.util.Log.i("OfflineNotification", "✅ Offline warning notification shown ($hoursOffline hours)")
    }

    /**
     * Clear/remove the offline warning notification
     */
    fun clearOfflineWarningNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        android.util.Log.i("OfflineNotification", "✅ Offline warning notification cleared")
    }

    /**
     * Check if the notification is currently showing
     */
    fun isNotificationShowing(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val activeNotifications = notificationManager.activeNotifications
            return activeNotifications.any { it.id == NOTIFICATION_ID }
        }
        // On older APIs, we can't reliably check, so return false
        return false
    }
}

