package com.example.emilockerclient.fcm

import android.util.Log
import com.example.emilockerclient.commands.CommandHandler
import com.example.emilockerclient.managers.DeviceControlManager
import com.example.emilockerclient.network.ServerCommand
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson

class EmiFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i("FCM", "New token: $token")
        // send token to your backend
        DeviceControlManager(applicationContext).let { manager ->
            // Use RetrofitClient to send token to backend in background
            // Best: do this in a WorkManager job; simplified below:
            try {
                val deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)
                val req = com.example.emilockerclient.network.FcmTokenRegisterRequest(deviceId, token)
                val call = com.example.emilockerclient.network.RetrofitClient.instance.registerFcm(req)
                call.enqueue(object : retrofit2.Callback<com.example.emilockerclient.network.RegisterResponse> {
                    override fun onResponse(call: retrofit2.Call<com.example.emilockerclient.network.RegisterResponse>, response: retrofit2.Response<com.example.emilockerclient.network.RegisterResponse>) {
                        Log.i("FCM", "Token registered: ${response.body()?.message}")
                    }
                    override fun onFailure(call: retrofit2.Call<com.example.emilockerclient.network.RegisterResponse>, t: Throwable) {
                        Log.e("FCM", "Token register failed: ${t.message}")
                    }
                })
            } catch (e: Exception) {
                Log.w("FCM", "Failed to register token: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i("FCM", "Message received: ${remoteMessage.data}")

        // Prefer server sending structured JSON in data payload:
        // data: { "command": "{\"type\":\"LOCK_DEVICE\",\"payload\":{\"message\":\"...\"}}"}
        val data = remoteMessage.data
        val gson = Gson()
        // Two ways: direct put fields or nested JSON string
        val cmdJson = data["command"] ?: data["cmd"] ?: data["payload"]
        if (cmdJson != null) {
            try {
                val cmd = gson.fromJson(cmdJson, ServerCommand::class.java)
                CommandHandler.handle(applicationContext, cmd)
            } catch (e: Exception) {
                Log.w("FCM", "Failed parse command JSON: ${e.message}")
            }
        } else {
            // fallback: handle simple key/values
            val type = data["type"]
            val message = data["message"]
            if (type != null) {
                val cmd = ServerCommand(type, payload = if (message != null) mapOf("message" to message) else null)
                CommandHandler.handle(applicationContext, cmd)
            }
        }
    }
}
