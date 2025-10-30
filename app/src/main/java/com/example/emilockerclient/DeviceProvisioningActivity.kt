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
        Log.i(TAG, "Intent: ${intent}")
        
        // Check if app is already Device Owner (prevent re-provisioning)
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val isAlreadyDeviceOwner = dpm.isDeviceOwnerApp(packageName)
        
        Log.i(TAG, "Device Owner status: $isAlreadyDeviceOwner")
        Log.i(TAG, "Package name: $packageName")
        
        if (isAlreadyDeviceOwner) {
            Log.w(TAG, "⚠️ App is already Device Owner. Re-provisioning may cause conflicts.")
            Log.w(TAG, "⚠️ Returning RESULT_OK to allow system to handle gracefully")
            // Return OK to avoid system errors, but don't save data again
            setResult(RESULT_OK)
            finish()
            return
        }

        // Extract admin extras bundle to verify it's present
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
            Log.i(TAG, "📦 Admin extras bundle received with ${adminExtras.size()} items")
            // Log keys in the bundle
            adminExtras.keySet()?.forEach { key ->
                Log.i(TAG, "   Key: $key = ${adminExtras.getString(key, "N/A")}")
            }
        } else {
            Log.w(TAG, "⚠️ No admin extras bundle found - this is normal for provisioning")
        }
        
        // Note: We do NOT save provisioning data here
        // The data will be extracted and saved in EmiAdminReceiver.onProfileProvisioningComplete()
        // This avoids conflicts and ensures data is saved only after Device Owner is set
        Log.i(TAG, "📦 Admin extras bundle will be processed after Device Owner is set")
        
        // Return success to continue provisioning
        // Android will now complete the provisioning process and set app as Device Owner
        setResult(RESULT_OK)

        Log.i(TAG, "✅ Provisioning activity completed, returning RESULT_OK")
        finish()
    }

}
