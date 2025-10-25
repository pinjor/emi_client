package com.example.emilockerclient

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log

/**
 * DeviceProvisioningActivity - Handles QR code provisioning flow
 *
 * This activity is triggered when a device scans a provisioning QR code during
 * the Android setup wizard. It receives the provisioning intent and extracts
 * custom data (device_id, seller_id) from the QR code.
 *
 * Flow:
 * 1. Device scans QR code on welcome screen
 * 2. Android downloads APK (if specified in QR)
 * 3. Android launches this activity with ACTION_PROVISION_MANAGED_DEVICE
 * 4. We extract and save custom data from PROVISIONING_ADMIN_EXTRAS_BUNDLE
 * 5. Return RESULT_OK to continue provisioning
 * 6. Android sets app as Device Owner
 * 7. onProfileProvisioningComplete() is called in EmiAdminReceiver
 */
class DeviceProvisioningActivity : Activity() {

    companion object {
        private const val TAG = "DeviceProvisioning"
        private const val PREFS_NAME = "emi_prefs"

        // Keys for SharedPreferences
        const val KEY_IS_PROVISIONED = "is_provisioned"
        const val KEY_DEVICE_ID = "provisioned_device_id"
        const val KEY_SELLER_ID = "provisioned_seller_id"
        const val KEY_SERVER_URL = "provisioned_server_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "✅ Provisioning activity started")
        Log.i(TAG, "Intent action: ${intent?.action}")

        // Extract admin extras bundle from provisioning QR code
        val adminExtras = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
                PersistableBundle::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE)
        }

        if (adminExtras != null) {
            Log.i(TAG, "📦 Admin extras bundle received")
            saveProvisioningData(adminExtras)
        } else {
            Log.w(TAG, "⚠️ No admin extras bundle found in provisioning intent")
        }

        // Return success to continue provisioning
        // Android will now complete the provisioning process and set app as Device Owner
        setResult(RESULT_OK)

        Log.i(TAG, "✅ Provisioning activity completed, returning RESULT_OK")
        finish()
    }

    /**
     * Extract and save custom data from QR code
     * This data will be used after provisioning completes
     */
    private fun saveProvisioningData(extras: PersistableBundle) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Extract custom data from QR code
        val deviceId = extras.getString("device_id", "")
        val sellerId = extras.getString("seller_id", "")
        val serverUrl = extras.getString("server_url", "")

        Log.i(TAG, "📝 Saving provisioning data:")
        Log.i(TAG, "   Device ID: $deviceId")
        Log.i(TAG, "   Seller ID: $sellerId")
        Log.i(TAG, "   Server URL: $serverUrl")

        // Save to SharedPreferences for later use
        prefs.edit().apply {
            putString(KEY_DEVICE_ID, deviceId)
            putString(KEY_SELLER_ID, sellerId)
            putString(KEY_SERVER_URL, serverUrl)
            putBoolean(KEY_IS_PROVISIONED, true)
            apply()
        }

        Log.i(TAG, "✅ Provisioning data saved successfully")
    }
}
