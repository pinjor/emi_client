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
import com.example.emilockerclient.workers.LocationTrackingWorker
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

        // 🔹 Check if device was provisioned via QR code
        val prefs = getSharedPreferences("emi_prefs", MODE_PRIVATE)
        val isProvisioned = prefs.getBoolean(DeviceProvisioningActivity.KEY_IS_PROVISIONED, false)
        val deviceIdFromQR = prefs.getString(DeviceProvisioningActivity.KEY_DEVICE_ID, null)
        val sellerIdFromQR = prefs.getString(DeviceProvisioningActivity.KEY_SELLER_ID, null)
        val provisionedViaQR = intent?.getBooleanExtra("provisioned_via_qr", false) ?: false

        // Get device serial number (this is our primary device identifier)
        val deviceSerial = try {
            identifierFetcher.getSerialNumber()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get serial: ${e.message}")
            "UNKNOWN"
        }

        if (isProvisioned) {
            Log.i(TAG, "📱 Device provisioned via QR code")
            Log.i(TAG, "   Device Serial: $deviceSerial")
            Log.i(TAG, "   Seller ID: ${sellerIdFromQR ?: "Not specified"}")

            if (provisionedViaQR) {
                Toast.makeText(
                    this,
                    "✅ Device provisioned!\nSerial: $deviceSerial\nSeller: ${sellerIdFromQR ?: "N/A"}",
                    Toast.LENGTH_LONG
                ).show()
            }

            // TODO: Register device with backend using serial number
            // registerDeviceWithBackend(deviceSerial, sellerIdFromQR, fcmToken)
        } else {
            Log.i(TAG, "📱 Device not provisioned via QR, using serial number: $deviceSerial")
        }

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

                val imei1 = try { identifierFetcher.getImei(0) } catch (e: Exception) { "N/A" }

                Log.i(TAG, "Device Serial: $deviceSerial")
                Log.i(TAG, "Device IMEI1: $imei1")
                Log.i(TAG, "Seller ID: ${sellerIdFromQR ?: "N/A"}")

                // TODO: Register device with backend here
                // Example API call:
                // registerDevice(
                //     serial: deviceSerial,
                //     imei: imei1,
                //     fcmToken: task.result,
                //     sellerId: sellerIdFromQR
                // )
            } else {
                Log.w(TAG, "Fetching FCM token failed", task.exception)
            }
        }

        if (dpm.isDeviceOwnerApp(packageName)) {
            val intent = Intent(this, ProvisioningTestActivity::class.java)
            startActivity(intent)
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

        // 🔹 Location tracking every 1 hour
        val locationTrackingRequest =
            PeriodicWorkRequestBuilder<LocationTrackingWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "locationTrackingWork",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            locationTrackingRequest
        )

        Log.i(TAG, "✅ Location tracking scheduled (every 1 hour)")

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
