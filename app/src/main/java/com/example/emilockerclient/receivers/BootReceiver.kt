package com.example.emilockerclient.receivers


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.emilockerclient.services.LockService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("BootReceiver", "Device booted: starting LockService")
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
        }
    }
}
