package com.example.emilockerclient.receivers

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.emilockerclient.services.LockService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

            // Only start the service if the app is the Device Owner.
            // This ensures the lock is only enforced with the correct permissions.
            if (dpm.isDeviceOwnerApp(context.packageName)) {
                Log.i("BootReceiver", "✅ Boot completed: Device Owner active. Starting LockService.")
                try {
                    val serviceIntent = Intent(context, LockService::class.java)
                    context.startForegroundService(serviceIntent) // API 26+
                } catch (e: Exception) {
                    try {
                        val serviceIntent = Intent(context, LockService::class.java)
                        context.startService(serviceIntent)
                    } catch (ex: Exception) {
                        Log.w("BootReceiver", "Failed to start LockService: ${ex.message}")
                    }
                }
            } else {
                Log.w("BootReceiver", "⚠️ Boot completed: App is NOT Device Owner. Skipping LockService.")
            }
        }
    }
}
