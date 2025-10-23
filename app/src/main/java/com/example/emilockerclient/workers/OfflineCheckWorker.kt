package com.example.emilockerclient.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.emilockerclient.managers.DeviceControlManager
import com.example.emilockerclient.utils.PrefsHelper

class OfflineCheckWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    companion object {
        private const val OFFLINE_LOCK_HOURS = 24 // 🔒 Lock if no heartbeat for 24h
    }

    override fun doWork(): Result {
        val context = applicationContext
        val last = PrefsHelper.getLastHeartbeatTime(context)
        val now = System.currentTimeMillis()

        if (last == 0L) {
            Log.w("OfflineCheckWorker", "No heartbeat recorded yet → skipping check.")
            return Result.success()
        }

        val hours = (now - last) / (1000 * 60 * 60)
        Log.i("OfflineCheckWorker", "Last heartbeat was $hours hours ago.")

        return try {
            if (hours >= OFFLINE_LOCK_HOURS) {
                Log.w(
                    "OfflineCheckWorker",
                    "⚠️ Device offline > $OFFLINE_LOCK_HOURS hours → enforcing lock."
                )
                val manager = DeviceControlManager(context)
                manager.showLockScreen("⚠️ No internet/heartbeat for 24h. Device locked.",
                    "To ensure security and compliance with company policies, the device has been locked due to prolonged offline status. Please reconnect to the internet and contact your administrator to unlock the device."
                    )
            } else {
                Log.i(
                    "OfflineCheckWorker",
                    "✅ Device is within safe offline window (< $OFFLINE_LOCK_HOURS hours)."
                )
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("OfflineCheckWorker", "Error applying offline lock: ${e.message}")
            Result.retry()
        }
    }
}
