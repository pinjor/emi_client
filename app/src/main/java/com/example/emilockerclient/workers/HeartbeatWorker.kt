package com.example.emilockerclient.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.emilockerclient.network.HeartbeatRequest
import com.example.emilockerclient.network.HeartbeatResponse
import com.example.emilockerclient.network.RetrofitClient
import com.example.emilockerclient.utils.PrefsHelper

class HeartbeatWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val context = applicationContext

        val deviceId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
        val model = android.os.Build.MODEL
        val locked = PrefsHelper.isLocked(context)
        val timestamp = System.currentTimeMillis()

        val request = HeartbeatRequest(
            deviceId = deviceId,
            imei = null,        // Needs privileged permission → left null
            simSerial = null,   // Same as above
            model = model,
            isLocked = locked,
            timestamp = timestamp
        )

        Log.i("HeartbeatWorker", "Sending heartbeat: $request")

        return try {
            val resp = RetrofitClient.instance.sendHeartbeat(request).execute()
            if (resp.isSuccessful) {
                val body: HeartbeatResponse? = resp.body()
                Log.i("HeartbeatWorker", "Heartbeat OK: ${body?.message}")

                // ✅ Save latest heartbeat timestamp
                PrefsHelper.setLastHeartbeatTime(context, System.currentTimeMillis())

                // ✅ Handle server command if present
                body?.command?.let { cmd ->
                    Log.i("HeartbeatWorker", "Server command received: ${cmd.type}")
                    CommandHandler.handle(applicationContext, cmd)
                }
                Result.success()
            } else {
                Log.w("HeartbeatWorker", "Heartbeat failed: HTTP ${resp.code()}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("HeartbeatWorker", "Heartbeat exception: ${e.message}")
            Result.retry()
        }
    }
}
