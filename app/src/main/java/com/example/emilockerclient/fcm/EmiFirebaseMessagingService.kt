package com.example.emilockerclient.fcm

import android.content.ComponentName
import android.util.Log
import com.example.emilockerclient.admin.EmiAdminReceiver
import com.example.emilockerclient.managers.DeviceIdentifierFetcher
import com.example.emilockerclient.network.*
import com.example.emilockerclient.commands.CommandHandler
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EmiFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "EmiFCMService"
    private val gson = Gson()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "🆕 New FCM token received: $token")

        try {
            val adminReceiver = ComponentName(this, EmiAdminReceiver::class.java)
            val identifierFetcher = DeviceIdentifierFetcher(this, adminReceiver)
            Log.i(TAG, "🔍 Fetching device identifiers for registration...")
            val serial = identifierFetcher.getSerialNumber()
            val imei = identifierFetcher.getImei(0) // Primary SIM IMEI

            val req = DeviceRegisterRequest(
                serial_number = serial,
                imei1 = imei,
                fcm_token = token
            )

            Log.i(TAG, "📤 Registering device with payload: ${gson.toJson(req)}")

            RetrofitClient.api.registerDevice(req)
                .enqueue(object : Callback<DeviceRegistrationResponse> { // <-- Use new specific type
                    override fun onResponse(call: Call<DeviceRegistrationResponse>, response: Response<DeviceRegistrationResponse>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            // Accessing the structured data is now safe!
                            val customerName = response.body()?.data?.device?.customer_name
                            val isLocked = response.body()?.data?.device?.device_status?.is_locked
                            val id = response.body()?.data?.device?.customer_id
                            Log.i(TAG, "🆔 Registered to customer ID: $id")

                            Log.i(TAG, "✅ Device registration successful! Customer: $customerName, Locked: $isLocked")
                        } else {
                            Log.w(TAG, "⚠️ Device registration failed: HTTP ${response.code()} → ${response.errorBody()?.string()}")
                        }
                    }

                    override fun onFailure(call: Call<DeviceRegistrationResponse>, t: Throwable) {
                        Log.e(TAG, "❌ Device registration request failed: ${t.message}", t)
                    }
                })

        } catch (e: SecurityException) {
            Log.e(TAG, "🔒 Permission error while fetching device identifiers: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error preparing registration payload: ${e.message}", e)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "📩 FCM message received: ${gson.toJson(remoteMessage.data)}")

        val data =
            remoteMessage.data // data is the map of key-value pairs sent in the message from the server
        Log.i(TAG, "🔍 Message data payload: ${gson.toJson(data)}")

//        if (data.isEmpty()) {
//            Log.w(TAG, "⚠️ Received FCM message with no data payload")
//            return
//        }
//
//        try {
//            val cmdJson = data["command"] ?: data["cmd"] ?: data["payload"]
//            if (cmdJson != null) {
//                val cmd = gson.fromJson(cmdJson, ServerCommand::class.java)
//                Log.i(TAG, "🧩 Parsed ServerCommand: ${gson.toJson(cmd)}")
//                CommandHandler.handle(applicationContext, cmd)
//            } else {
//                val commandType = data["command"] ?: data["type"] ?: ""
//                if (commandType.isNotBlank()) {
//                    val cmd = ServerCommand(commandType, payload = data)
//                    Log.i(TAG, "⚙️ Executing command (simple mode): ${gson.toJson(cmd)}")
//                    CommandHandler.handle(applicationContext, cmd)
//                } else {
//                    Log.w(TAG, "⚠️ Unknown message structure, skipping: ${gson.toJson(data)}")
//                }
//            }
//
//        } catch (e: Exception) {
//            Log.e(TAG, "💥 Failed to parse or handle command: ${e.message}", e)
//        }
    }
}
