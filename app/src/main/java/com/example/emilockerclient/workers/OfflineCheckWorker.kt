package com.example.emilockerclient.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.emilockerclient.managers.DeviceControlManager
import com.example.emilockerclient.utils.ConnectivityHelper
import com.example.emilockerclient.utils.OfflineNotificationHelper
import com.example.emilockerclient.utils.PrefsHelper
import kotlin.concurrent.thread

/**
 * OfflineCheckWorker - Enforces device connectivity requirements
 *
 * Enforcement Logic:
 * - Checks internet connectivity actively (not just waiting for FCM)
 * - Updates heartbeat when device is back online
 * - 24h offline: Show persistent warning notification
 * - 48h offline: Trigger full device lock
 * - Back online: Clear notifications and update heartbeat
 */
class OfflineCheckWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    companion object {
        private const val TAG = "OfflineCheckWorker"

        // Thresholds (in hours)
        private const val WARNING_THRESHOLD_HOURS = 24L  // Show warning notification
        private const val HARD_LOCK_THRESHOLD_HOURS = 48L // Trigger device lock
    }

    override fun doWork(): Result {
        val context = applicationContext
        Log.i(TAG, "🔍 Running offline check...")

        return try {
            // Step 1: Check if device has internet connectivity
            val isOnline = checkInternetConnectivity(context)

            if (isOnline) {
                Log.i(TAG, "✅ Device is ONLINE - updating heartbeat timestamp")
                // Update heartbeat immediately when online
                PrefsHelper.setLastHeartbeatTime(context, System.currentTimeMillis())

                // Clear any offline warnings
                OfflineNotificationHelper.clearOfflineWarningNotification(context)

                Log.i(TAG, "✅ Heartbeat updated, all offline restrictions lifted")
                return Result.success()
            }

            // Step 2: Device is offline - check how long
            Log.w(TAG, "⚠️ Device is OFFLINE - checking offline duration...")

            val lastHeartbeat = PrefsHelper.getLastHeartbeatTime(context)

            if (lastHeartbeat == 0L) {
                Log.w(TAG, "⚠️ No heartbeat recorded yet. Setting initial timestamp.")
                // Set initial heartbeat timestamp (first run)
                PrefsHelper.setLastHeartbeatTime(context, System.currentTimeMillis())
                return Result.success()
            }

            val currentTime = System.currentTimeMillis()
            val hoursOffline = (currentTime - lastHeartbeat) / (1000 * 60 * 60)

            Log.i(TAG, "📊 Last heartbeat was $hoursOffline hours ago")

            when {
                hoursOffline >= HARD_LOCK_THRESHOLD_HOURS -> {
                    // 48+ hours offline: HARD LOCK
                    handleHardLock(context, hoursOffline)
                }

                hoursOffline >= WARNING_THRESHOLD_HOURS -> {
                    // 24-48 hours offline: WARNING NOTIFICATION
                    handleWarning(context, hoursOffline)
                }

                else -> {
                    // < 24 hours: Clear any existing warning
                    handleNormalState(context, hoursOffline)
                }
            }

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in offline check: ${e.message}", e)
            Result.retry()
        }
    }

    /**
     * Check if device has working internet connectivity
     * This actively pings the network, not just waiting for FCM
     */
    private fun checkInternetConnectivity(context: Context): Boolean {
        return try {
            // Quick check: Is network available?
            if (!ConnectivityHelper.isNetworkAvailable(context)) {
                Log.i(TAG, "No network available (WiFi/Cellular off)")
                return false
            }

            // Deep check: Can we actually reach the internet?
            // Run in background thread to avoid blocking
            var isReachable = false
            val checkThread = thread {
                try {
                    // Try to ping your backend server
                    // TODO: Replace with your actual backend URL
                    isReachable = ConnectivityHelper.pingBackendServer("https://www.google.com")
                } catch (e: Exception) {
                    Log.w(TAG, "Internet reachability check failed: ${e.message}")
                }
            }

            // Wait for check to complete (max 6 seconds)
            checkThread.join(6000)

            isReachable

        } catch (e: Exception) {
            Log.e(TAG, "Connectivity check error: ${e.message}")
            false
        }
    }

    /**
     * Handle 48+ hours offline: Lock the device
     */
    private fun handleHardLock(context: Context, hoursOffline: Long) {
        Log.w(TAG, "🔒 HARD LOCK TRIGGERED - Device offline for $hoursOffline hours (>= $HARD_LOCK_THRESHOLD_HOURS)")

        // Clear the warning notification (lock screen will replace it)
        OfflineNotificationHelper.clearOfflineWarningNotification(context)

        // Trigger device lock
        val manager = DeviceControlManager(context)
        manager.showLockScreen(
            title = "⚠️ Device Locked - No Internet",
            message = "This device has been offline for $hoursOffline hours.\n\n" +
                    "For security and compliance, the device has been automatically locked.\n\n" +
                    "Please connect to the internet and contact your administrator to unlock."
        )

        Log.i(TAG, "✅ Device lock screen activated")
    }

    /**
     * Handle 24-48 hours offline: Show warning notification
     */
    private fun handleWarning(context: Context, hoursOffline: Long) {
        Log.w(TAG, "⚠️ WARNING - Device offline for $hoursOffline hours (>= $WARNING_THRESHOLD_HOURS)")

        val hoursUntilLock = HARD_LOCK_THRESHOLD_HOURS - hoursOffline

        // Show persistent warning notification
        OfflineNotificationHelper.showOfflineWarningNotification(context, hoursOffline)

        Log.i(TAG, "📢 Warning notification shown. Device will lock in $hoursUntilLock hours if still offline.")
    }

    /**
     * Handle normal state (< 24 hours): Clear notifications
     */
    private fun handleNormalState(context: Context, hoursOffline: Long) {
        Log.i(TAG, "✅ Device is within safe offline window ($hoursOffline hours < $WARNING_THRESHOLD_HOURS)")

        // Clear warning notification if it's showing
        if (OfflineNotificationHelper.isNotificationShowing(context)) {
            OfflineNotificationHelper.clearOfflineWarningNotification(context)
            Log.i(TAG, "✅ Cleared warning notification (device back within safe window)")
        }
    }
}
