package com.example.emilockerclient.receivers
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.emilockerclient.managers.DeviceControlManager
import com.example.emilockerclient.services.LockService
import com.example.emilockerclient.utils.PrefsHelper
import com.example.emilockerclient.workers.OfflineCheckWorker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            Log.w("BootReceiver", "⚠️ App is NOT Device Owner. Skipping boot tasks.")
            return
        }

        Log.i("BootReceiver", "✅ Boot completed: Restarting services and applying security policies.")

        val manager = DeviceControlManager(context)

        // 1. 🔐 Re-apply USB & ADB restrictions
//        manager.disableUSBDataTransfer()

        // 2. 🔒 Restore lock screen if previously locked
        if (PrefsHelper.isLocked(context)) {
            manager.showLockScreen( PrefsHelper.getLockTitle(context)  ,PrefsHelper.getLockMessage(context))
        }

        // 3. 🔁 Restart periodic offline check worker
        try {
            val offlineCheckWork = PeriodicWorkRequestBuilder<OfflineCheckWorker>(
                1, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "OfflineCheckWorker",
                ExistingPeriodicWorkPolicy.REPLACE,
                offlineCheckWork
            )

            Log.i("BootReceiver", "🔁 OfflineCheckWorker scheduled.")
        } catch (e: Exception) {
            Log.e("BootReceiver", "❌ Failed to schedule OfflineCheckWorker: ${e.message}")
        }

        // 4. 🚨 Restart LockService
        try {
            val serviceIntent = Intent(context, LockService::class.java)
            context.startForegroundService(serviceIntent)
            Log.i("BootReceiver", "🚨 LockService restarted.")
        } catch (e: Exception) {
            try {
                context.startService(Intent(context, LockService::class.java))
                Log.i("BootReceiver", "⚠️ LockService started (fallback to startService).")
            } catch (ex: Exception) {
                Log.e("BootReceiver", "❌ Failed to start LockService: ${ex.message}")
            }
        }
    }
}
