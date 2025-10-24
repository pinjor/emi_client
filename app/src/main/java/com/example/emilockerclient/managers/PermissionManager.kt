package com.example.emilockerclient.managers

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context, private val adminReceiver: ComponentName) {

    private val TAG = "PermissionManager"

    // List all permissions your app needs
    private val REQUIRED_PERMISSIONS = listOf(
        android.Manifest.permission.READ_PHONE_STATE,
        android.Manifest.permission.READ_PHONE_NUMBERS,
        android.Manifest.permission.POST_NOTIFICATIONS,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION // For periodic location tracking
        // Add more dangerous permissions if needed
    )

    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    /**
     * Call this at app start to auto-grant and lock all required permissions.
     */
    fun ensurePermissions() {
        val pkg = context.packageName

        if (!dpm.isDeviceOwnerApp(pkg)) {
            Log.w(TAG, "App is not Device Owner. Cannot auto-grant permissions.")
            return
        }

        // Lock auto-grant policy for all permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            dpm.setPermissionPolicy(adminReceiver, DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT)
        }

        REQUIRED_PERMISSIONS.forEach { permission ->
            val current = ContextCompat.checkSelfPermission(context, permission)
            if (current == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission $permission already granted")
            } else {
                try {
                    // Auto-grant permission via DevicePolicyManager
                    dpm.setPermissionGrantState(
                        adminReceiver,
                        pkg,
                        permission,
                        DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
                    )
                    Log.i(TAG, "Permission $permission auto-granted")
                } catch (se: SecurityException) {
                    Log.e(TAG, "Cannot auto-grant $permission: ${se.message}")
                }
            }
        }
    }

    /**
     * Check if all permissions are granted
     */
    fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
