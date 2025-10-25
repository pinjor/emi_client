package com.example.emilockerclient.fcm

import android.content.ComponentName
import android.content.Context
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
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
            val isDeviceOwner = dpm.isDeviceOwnerApp(packageName)

            if (isDeviceOwner) {
                // Device Owner Mode - Auto-register with auto-fetched identifiers
                Log.i(TAG, "🔍 Device Owner Mode - Auto-fetching identifiers...")
                registerDeviceOwnerMode(identifierFetcher, token)
            } else {
                // Admin Mode - Check if setup completed, use manually entered IMEI
                Log.i(TAG, "⚙️ Admin Mode - Checking setup status...")
                registerAdminMode(token)
            }

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error in onNewToken: ${e.message}", e)
        }
    }

    /**
     * Register device in Device Owner Mode (auto-fetch identifiers)
     */
    private fun registerDeviceOwnerMode(identifierFetcher: DeviceIdentifierFetcher, token: String) {
        try {
            val serial = identifierFetcher.getSerialNumber()
            val imei = identifierFetcher.getImei(0) // Primary SIM IMEI

            val req = DeviceRegisterRequest(
                serial_number = serial,
                imei1 = imei,
                fcm_token = token
            )

            Log.i(TAG, "📤 Registering device (Device Owner Mode): ${gson.toJson(req)}")

            RetrofitClient.api.registerDevice(req)
                .enqueue(object : Callback<DeviceRegistrationResponse> {
                    override fun onResponse(
                        call: Call<DeviceRegistrationResponse>,
                        response: Response<DeviceRegistrationResponse>
                    ) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            val customerName = response.body()?.data?.device?.customer_name
                            val isLocked = response.body()?.data?.device?.device_status?.is_locked
                            val id = response.body()?.data?.device?.customer_id
                            Log.i(TAG, "✅ Device Owner registration successful!")
                            Log.i(TAG, "🆔 Customer ID: $id, Name: $customerName, Locked: $isLocked")
                        } else {
                            Log.w(TAG, "⚠️ Device Owner registration failed: HTTP ${response.code()} → ${response.errorBody()?.string()}")
                        }
                    }

                    override fun onFailure(call: Call<DeviceRegistrationResponse>, t: Throwable) {
                        Log.e(TAG, "❌ Device Owner registration request failed: ${t.message}", t)
                    }
                })

        } catch (e: SecurityException) {
            Log.e(TAG, "🔒 Permission error while fetching device identifiers: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error preparing Device Owner registration: ${e.message}", e)
        }
    }

    /**
     * Register device in Admin Mode (use manually entered IMEI)
     */
    private fun registerAdminMode(token: String) {
        val setupPrefs = com.example.emilockerclient.utils.SetupPrefsHelper

        // Check if registration already completed
        if (setupPrefs.isRegistrationCompleted(this)) {
            Log.i(TAG, "✅ Admin Mode: Already registered, updating token if needed")

            // Get stored IMEI
            val imei = setupPrefs.getRegisteredImei(this)
            if (imei != null) {
                // Re-register with new token
                val req = DeviceRegisterRequest(
                    serial_number = imei, // Using IMEI as identifier in Admin Mode
                    imei1 = imei,
                    fcm_token = token
                )

                Log.i(TAG, "📤 Updating FCM token (Admin Mode) for IMEI: $imei")

                RetrofitClient.api.registerDevice(req)
                    .enqueue(object : Callback<DeviceRegistrationResponse> {
                        override fun onResponse(
                            call: Call<DeviceRegistrationResponse>,
                            response: Response<DeviceRegistrationResponse>
                        ) {
                            if (response.isSuccessful && response.body()?.success == true) {
                                Log.i(TAG, "✅ Admin Mode: Token updated successfully")
                                setupPrefs.setRegisteredFcmToken(applicationContext, token)
                            } else {
                                Log.w(TAG, "⚠️ Admin Mode: Token update failed: HTTP ${response.code()}")
                            }
                        }

                        override fun onFailure(call: Call<DeviceRegistrationResponse>, t: Throwable) {
                            Log.e(TAG, "❌ Admin Mode: Token update failed: ${t.message}")
                        }
                    })
            }
        } else {
            Log.i(TAG, "⚠️ Admin Mode: Setup not completed yet, waiting for manual registration")
            // Token will be used during manual registration in RegistrationActivity
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
//        Log.i(TAG, "📩 FCM message received with payload: ${gson.toJson(remoteMessage.data)}")

        // ✅ Update heartbeat timestamp - device is online and syncing!
        try {
            com.example.emilockerclient.utils.PrefsHelper.setLastHeartbeatTime(
                applicationContext,
                System.currentTimeMillis()
            )
            Log.i(TAG, "✅ Heartbeat timestamp updated (FCM message received)")

            // Clear offline warning notification if showing (device is back online)
            com.example.emilockerclient.utils.OfflineNotificationHelper.clearOfflineWarningNotification(applicationContext)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update heartbeat: ${e.message}")
        }

        val data =
            remoteMessage.data // data is the map of key-value pairs sent in the message from the server
        Log.i(TAG, "📬 FCM Data Payload: ${gson.toJson(data)}")
        Log.i(TAG, "received data: $data")
        val commandValue = data["command"] ?: data["cmd"] ?: data["type"]
        if(!commandValue.isNullOrBlank()){
            // copy all keys into a Map<String,String>
            val params = data.mapValues { it.value ?: "" }
            val cmd = ServerCommand(command = commandValue, params = params)
            CommandHandler.handle(applicationContext, cmd)
        }else{
            Log.w(TAG, "⚠️ 'command' key missing or empty in FCM data payload: ${gson.toJson(data)}")
        }
    }
}
