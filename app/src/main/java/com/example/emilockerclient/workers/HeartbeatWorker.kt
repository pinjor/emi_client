package com.example.emilockerclient.workers


import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.emilockerclient.network.HeartbeatRequest
import com.example.emilockerclient.network.RetrofitClient
import com.example.emilockerclient.utils.PrefsHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HeartbeatWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val context = applicationContext

        // Use stable ID that works without special permissions
        val deviceId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )

        val model = android.os.Build.MODEL
        val locked = PrefsHelper.isLocked(context)
        val timestamp = System.currentTimeMillis()

        val request = HeartbeatRequest(
            deviceId = deviceId,
            imei = null,         // skipped → requires privileged perms
            simSerial = null,    // skipped → requires privileged perms
            model = model,
            isLocked = locked,
            timestamp = timestamp
        )

        Log.i("HeartbeatWorker", "Sending heartbeat: $request")

        // Mock API call
        RetrofitClient.instance.sendHeartbeat(request)
            .enqueue(object : Callback<com.example.emilockerclient.network.HeartbeatResponse> {
                override fun onResponse(
                    call: Call<com.example.emilockerclient.network.HeartbeatResponse>,
                    response: Response<com.example.emilockerclient.network.HeartbeatResponse>
                ) {
                    Log.i("HeartbeatWorker", "Heartbeat success: ${response.body()?.message}")
                }

                override fun onFailure(
                    call: Call<com.example.emilockerclient.network.HeartbeatResponse>,
                    t: Throwable
                ) {
                    Log.e("HeartbeatWorker", "Heartbeat failed: ${t.message}")
                }
            })

        return Result.success()
    }
}