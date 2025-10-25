package com.example.emilockerclient.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.emilockerclient.MainActivity
import com.example.emilockerclient.R

/**
 * Helper class to manage EMI reminder notifications.
 * Shows high-priority, dismissible notifications for payment reminders.
 */
object EmiReminderNotificationHelper {

    private const val CHANNEL_ID = "emi_reminder_channel"
    private const val CHANNEL_NAME = "EMI Reminders"
    private const val NOTIFICATION_ID = 2001

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
                description = "Important payment reminders and messages from admin"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show EMI reminder notification with title and message
     * @param title Title of the notification (e.g., "Payment Due")
     * @param message Message content (e.g., "Please pay your EMI by tomorrow")
     */
    fun showEmiReminder(context: Context, title: String, message: String) {
        createNotificationChannel(context)

        // Intent to open MainActivity when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", "emi_reminder")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use system info icon
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOngoing(false) // Can be dismissed
            .setAutoCancel(true) // Auto-dismiss when tapped
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 250, 500, 250, 500)) // Triple vibration pattern
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_LIGHTS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Shows on lock screen
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        android.util.Log.i("EmiReminderNotification", "✅ EMI reminder notification shown: $title")
    }

    /**
     * Clear/remove the EMI reminder notification
     */
    fun clearEmiReminder(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        android.util.Log.i("EmiReminderNotification", "✅ EMI reminder notification cleared")
    }

    /**
     * Show a generic message notification (similar to EMI reminder but for other messages)
     * @param title Title of the message
     * @param message Message content
     */
    fun showMessage(context: Context, title: String, message: String) {
        // Uses same implementation as EMI reminder
        showEmiReminder(context, title, message)
    }
}

