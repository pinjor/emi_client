package com.example.emilockerclient.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.emilockerclient.managers.DeviceControlManager

class SimReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.SIM_STATE_CHANGED") {
            Log.w("SimReceiver", "SIM state changed! Possible SIM swap detected.")

            val manager = DeviceControlManager(context)
            manager.showLockScreen("⚠️ SIM card changed! Device locked.")

            // TODO: Optionally send heartbeat/alert to backend


// ✅ Trigger immediate heartbeat/alert
//            androidx.work.WorkManager.getInstance(context)
//                .enqueue(androidx.work.OneTimeWorkRequestBuilder<com.example.emilockerclient.workers.HeartbeatWorker>().build())
        }
    }
}
