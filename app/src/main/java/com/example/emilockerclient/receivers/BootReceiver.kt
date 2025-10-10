package com.example.emilockerclient.receivers

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.emilockerclient.managers.DeviceControlManager
import com.example.emilockerclient.services.LockService
import com.example.emilockerclient.utils.PrefsHelper
//import com.example.emilockerclient.workers.HeartbeatWorker
import com.example.emilockerclient.workers.OfflineCheckWorker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

            if (dpm.isDeviceOwnerApp(context.packageName)) {
                Log.i("BootReceiver", "✅ Boot completed: restarting services...")

                // 🔹 Restore lock if device was locked before reboot
                if (PrefsHelper.isLocked(context)) {
                    val manager = DeviceControlManager(context)
                    manager.showLockScreen(PrefsHelper.getLockMessage(context))
                }

//                // 🔹 Restart workers
//                val heartbeatWork =
//                    PeriodicWorkRequestBuilder<HeartbeatWorker>(15, TimeUnit.MINUTES).build()
//                WorkManager.getInstance(context).enqueue(heartbeatWork)

                val offlineCheckWork =
                    PeriodicWorkRequestBuilder<OfflineCheckWorker>(1, TimeUnit.HOURS).build()
                WorkManager.getInstance(context).enqueue(offlineCheckWork)

                // 🔹 Restart LockService
                try {
                    val serviceIntent = Intent(context, LockService::class.java)
                    context.startForegroundService(serviceIntent)
                } catch (e: Exception) {
                    context.startService(Intent(context, LockService::class.java))
                }
            } else {
                Log.w("BootReceiver", "⚠️ App is NOT Device Owner. Skipping.")
            }
        }
    }
}
