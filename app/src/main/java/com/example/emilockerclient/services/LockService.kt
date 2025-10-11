package com.example.emilockerclient.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.emilockerclient.R
import com.example.emilockerclient.ui.LockScreenActivity
import com.example.emilockerclient.utils.PrefsHelper

class LockService : Service() {

    companion object {
        private const val CHANNEL_ID = "emi_lock_channel"
        private const val NOTIF_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("LockService", "onCreate()")

        createNotificationChannel()

        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("EMI Locker")
                .setContentText("Monitoring device lock state")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("EMI Locker")
                .setContentText("Monitoring device lock state")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build()
        }

        // startForeground must always run on API26+ when using startForegroundService
        startForeground(NOTIF_ID, notification)

        // If locked before reboot -> open LockScreenActivity (best-effort)
        try {
            if (PrefsHelper.isLocked(this)) {
                val message = PrefsHelper.getLockMessage(this)
                val lockIntent = Intent(this, LockScreenActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra("LOCK_MESSAGE", message)
                }
                startActivity(lockIntent)
                Log.i("LockService", "Started LockScreenActivity because device was locked")
            } else {
                Log.i("LockService", "Device not locked; no action")
            }
        } catch (e: Exception) {
            Log.w("LockService", "Failed to start LockScreenActivity from service: ${e.message}")
        }

        // stop self; this service only ensures the UI is launched after boot
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "EMI Locker Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground service for EMI lock persistence"
            }
            manager.createNotificationChannel(channel)
        }
    }
}