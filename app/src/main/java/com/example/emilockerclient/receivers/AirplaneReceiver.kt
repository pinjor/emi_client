package com.example.emilockerclient.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.emilockerclient.managers.DeviceControlManager

class AirplaneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_AIRPLANE_MODE_CHANGED) {
            val state = intent.getBooleanExtra("state", false)
            Log.i("AirplaneReceiver", "Airplane mode changed: $state")

            if (state) {
                val manager = DeviceControlManager(context)
                manager.showLockScreen("⚠️ Airplane Mode enabled! Device locked.")

                // ✅ Trigger immediate heartbeat/alert
                androidx.work.WorkManager.getInstance(context)
                    .enqueue(androidx.work.OneTimeWorkRequestBuilder<com.example.emilockerclient.workers.HeartbeatWorker>().build())


            }
        }
    }
}
