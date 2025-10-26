package com.example.emilockerclient.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.example.emilockerclient.R
import com.example.emilockerclient.ui.LockScreenActivity
import com.example.emilockerclient.utils.PrefsHelper

/**
 * LockMonitorService: A foreground service that ensures the lock screen stays active
 * and cannot be bypassed by the user. It continuously monitors if the lock screen
 * is in the foreground and relaunches it if the user tries to navigate away.
 */
class LockMonitorService : Service() {

    companion object {
        private const val CHANNEL_ID = "emi_lock_monitor_channel"
        private const val NOTIF_ID = 1002
        private const val CHECK_INTERVAL = 3000L // Check every 3 seconds (increased from 2)
        private const val TAG = "LockMonitorService"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isMonitoring = false

    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (!isMonitoring) return

            try {
                // Check if device is still in locked state
                if (PrefsHelper.isLocked(this@LockMonitorService)) {
                    // Device is locked - relaunch lock screen
                    val title = PrefsHelper.getLockTitle(this@LockMonitorService)
                    val message = PrefsHelper.getLockMessage(this@LockMonitorService)
                    val lockIntent = Intent(this@LockMonitorService, LockScreenActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra("LOCK_TITLE", title)
                        putExtra("LOCK_MESSAGE", message)
                    }

                    startActivity(lockIntent)
                    Log.d(TAG, "🔒 Lock screen relaunched")
                } else {
                    // Device is no longer locked, stop monitoring
                    Log.i(TAG, "Device unlocked, stopping monitor service")
                    stopSelf()
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error in monitor loop: ${e.message}")
            }

            // Schedule next check
            if (isMonitoring) {
                handler.postDelayed(this, CHECK_INTERVAL)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "LockMonitorService onCreate()")

        createNotificationChannel()

        // Create a minimal, silent notification (required for foreground service)
        // This notification is intentionally minimal and low-priority
        // Silence is controlled by the channel (IMPORTANCE_MIN with no sound)
        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("EMI Locker")
                .setContentText("Service running")
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock) // Use system lock icon (minimal)
                .setOngoing(true)
                .setShowWhen(false) // Don't show time
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("EMI Locker")
                .setContentText("Service running")
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setPriority(Notification.PRIORITY_MIN)
                .build()
        }

        startForeground(NOTIF_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "LockMonitorService onStartCommand()")

        if (!isMonitoring) {
            isMonitoring = true
            handler.post(monitorRunnable)
            Log.i(TAG, "Started monitoring lock screen")
        }

        return START_STICKY // Service will be restarted if killed
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "LockMonitorService onDestroy()")
        isMonitoring = false
        handler.removeCallbacks(monitorRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "EMI Lock Monitor",
                NotificationManager.IMPORTANCE_MIN // Minimal - only in expanded notification shade
            ).apply {
                description = "Monitors and maintains device lock screen"
                setShowBadge(false)
                setSound(null, null) // No sound
                enableLights(false) // No LED
                enableVibration(false) // No vibration
            }
            manager.createNotificationChannel(channel)
        }
    }
}
