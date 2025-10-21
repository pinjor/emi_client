package com.example.emilockerclient

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.emilockerclient.admin.EmiAdminReceiver
import com.example.emilockerclient.managers.DeviceControlManager
import com.example.emilockerclient.managers.DeviceIdentifierFetcher
import com.example.emilockerclient.managers.PermissionManager
//import com.example.emilockerclient.workers.HeartbeatWorker
import com.example.emilockerclient.workers.OfflineCheckWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var compName: ComponentName
    private lateinit var deviceManager: DeviceControlManager
    private lateinit var identifierFetcher: DeviceIdentifierFetcher
    private lateinit var permissionManager: PermissionManager

    private val TAG = "MainActivityCheckingIdentifiers"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, EmiAdminReceiver::class.java)
        deviceManager = DeviceControlManager(this)
        identifierFetcher = DeviceIdentifierFetcher(this, compName)
        permissionManager = PermissionManager(this, compName)

        // 🔹 Auto-grant and lock all required permissions (Device Owner only)
        permissionManager.ensurePermissions()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Optional: fetch the current token immediately
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            Log.i(TAG, "FCM token fetch task completed")
            if (task.isSuccessful) {
                Log.i(TAG, "Initial FCM token: ${task.result}")
                Log.i(TAG, "FCM token: ${task.result}")
                val serial = try { identifierFetcher.getSerialNumber() } catch (e: Exception) { "N/A" }
                val imei1 = try { identifierFetcher.getImei(0) } catch (e: Exception) { "N/A" }

                Log.i(TAG, "Device Serial: $serial")
                Log.i(TAG, "Device IMEI1: $imei1")
            } else {
                Log.w(TAG, "Fetching FCM token failed", task.exception)
            }
        }

        // 🔹 Periodic heartbeat every 15 min
//        val heartbeatRequest =
//            PeriodicWorkRequestBuilder<HeartbeatWorker>(15, TimeUnit.MINUTES).build()
//        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
//            "heartbeatWork",
//            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
//            heartbeatRequest
//        )

        // 🔹 Offline check every 1 hour
        val offlineCheckRequest =
            PeriodicWorkRequestBuilder<OfflineCheckWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "offlineCheckWork",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            offlineCheckRequest
        )

//        // 🔹 One immediate heartbeat on app start
//        WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<HeartbeatWorker>().build())

    }


    private fun retrieveDeviceId(idName: String, fetcher: () -> String) {
        if (!deviceManager.isDeviceOwner()) {
            Toast.makeText(this, "Device Owner is required to access $idName.", Toast.LENGTH_LONG)
                .show()
            return
        }

        try {
            val result = fetcher()
            Log.d(TAG, "$idName retrieved: $result")
            Toast.makeText(this, "$idName: $result", Toast.LENGTH_LONG).show()
        } catch (e: DeviceIdentifierFetcher.DeviceIdAccessException) {
            Log.e(TAG, "Access denied for $idName: ${e.message}")
            Toast.makeText(this, "ERROR: ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error retrieving $idName: ${e.message}", e)
            Toast.makeText(
                this,
                "Unexpected Error accessing $idName: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
