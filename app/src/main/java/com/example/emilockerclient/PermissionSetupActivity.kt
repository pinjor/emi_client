package com.example.emilockerclient

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.emilockerclient.admin.EmiAdminReceiver
import com.example.emilockerclient.managers.PermissionManager
import com.example.emilockerclient.utils.SetupPrefsHelper

/**
 * Activity for granting all required permissions in Admin Mode
 */
class PermissionSetupActivity : AppCompatActivity() {

    private val TAG = "PermissionSetupActivity"

    private lateinit var dpm: DevicePolicyManager
    private lateinit var compName: ComponentName
    private lateinit var permissionManager: PermissionManager

    // Views
    private lateinit var tvPermissionStatus: TextView
    private lateinit var tvLocationStatus: TextView
    private lateinit var tvBackgroundLocationStatus: TextView
    private lateinit var tvNotificationStatus: TextView
    private lateinit var tvPhoneStateStatus: TextView
    private lateinit var tvDeviceAdminStatus: TextView
    private lateinit var tvOverlayStatus: TextView
    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var tvBatteryOptimizationStatus: TextView
    private lateinit var tvUsageStatsStatus: TextView
    private lateinit var btnCompleteSetup: Button
    private lateinit var tvCompleteHint: TextView

    // Permission launchers
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.i(TAG, "Location permissions result: $permissions")
        updatePermissionStatus()
    }

    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.i(TAG, "Background location permission: $granted")
        updatePermissionStatus()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.i(TAG, "Notification permission: $granted")
        updatePermissionStatus()
    }

    private val phoneStatePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.i(TAG, "Phone state permission: $granted")
        updatePermissionStatus()
    }

    private val deviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.i(TAG, "Device admin activation result: ${result.resultCode}")
        updatePermissionStatus()
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.i(TAG, "Overlay permission result: ${result.resultCode}")
        updatePermissionStatus()
    }

    private val accessibilityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.i(TAG, "Accessibility permission result: ${result.resultCode}")
        updatePermissionStatus()
    }

    private val batteryOptimizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.i(TAG, "Battery optimization result: ${result.resultCode}")
        updatePermissionStatus()
    }

    private val usageStatsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.i(TAG, "Usage stats permission result: ${result.resultCode}")
        updatePermissionStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_setup)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, EmiAdminReceiver::class.java)
        permissionManager = PermissionManager(this, compName)

        initializeViews()
        setupClickListeners()
        updatePermissionStatus()
    }

    override fun onResume() {
        super.onResume()
        // Update status when returning from settings
        updatePermissionStatus()
    }

    private fun initializeViews() {
        tvPermissionStatus = findViewById(R.id.tvPermissionStatus)
        tvLocationStatus = findViewById(R.id.tvLocationStatus)
        tvBackgroundLocationStatus = findViewById(R.id.tvBackgroundLocationStatus)
        tvNotificationStatus = findViewById(R.id.tvNotificationStatus)
        tvPhoneStateStatus = findViewById(R.id.tvPhoneStateStatus)
        tvDeviceAdminStatus = findViewById(R.id.tvDeviceAdminStatus)
        tvOverlayStatus = findViewById(R.id.tvOverlayStatus)
        tvAccessibilityStatus = findViewById(R.id.tvAccessibilityStatus)
        tvBatteryOptimizationStatus = findViewById(R.id.tvBatteryOptimizationStatus)
        tvUsageStatsStatus = findViewById(R.id.tvUsageStatsStatus)
        btnCompleteSetup = findViewById(R.id.btnCompleteSetup)
        tvCompleteHint = findViewById(R.id.tvCompleteHint)
    }

    private fun setupClickListeners() {
        findViewById<CardView>(R.id.cardLocation).setOnClickListener {
            requestLocationPermissions()
        }

        findViewById<CardView>(R.id.cardBackgroundLocation).setOnClickListener {
            requestBackgroundLocationPermission()
        }

        findViewById<CardView>(R.id.cardNotification).setOnClickListener {
            requestNotificationPermission()
        }

        findViewById<CardView>(R.id.cardPhoneState).setOnClickListener {
            requestPhoneStatePermission()
        }

        findViewById<CardView>(R.id.cardDeviceAdmin).setOnClickListener {
            requestDeviceAdminActivation()
        }

        findViewById<CardView>(R.id.cardOverlay).setOnClickListener {
            requestOverlayPermission()
        }

        findViewById<CardView>(R.id.cardAccessibility).setOnClickListener {
            requestAccessibilityPermission()
        }

        findViewById<CardView>(R.id.cardBatteryOptimization).setOnClickListener {
            requestBatteryOptimizationExemption()
        }

        findViewById<CardView>(R.id.cardUsageStats).setOnClickListener {
            requestUsageStatsPermission()
        }


        btnCompleteSetup.setOnClickListener {
            completeSetup()
        }
    }

    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Must request foreground location first
            if (!permissionManager.areLocationPermissionsGranted()) {
                Toast.makeText(
                    this,
                    "Please grant Location Access first",
                    Toast.LENGTH_SHORT
                ).show()
                requestLocationPermissions()
            } else {
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestPhoneStatePermission() {
        phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
    }

    private fun requestDeviceAdminActivation() {
        if (!dpm.isAdminActive(compName)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Enable Device Admin to allow lock screen and basic device control features."
                )
            }
            deviceAdminLauncher.launch(intent)
        } else {
            Toast.makeText(this, "Device Admin already activated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                try {
                    // Try multiple approaches for maximum compatibility
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.fromParts("package", packageName, null)
                    )
                    overlayPermissionLauncher.launch(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open overlay permission settings with package URI", e)
                    try {
                        // Fallback: Try without URI
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                        overlayPermissionLauncher.launch(intent)
                    } catch (e2: Exception) {
                        Log.e(TAG, "Failed to open overlay permission settings", e2)
                        // Show manual instructions
                        android.app.AlertDialog.Builder(this)
                            .setTitle("Enable Display Over Apps")
                            .setMessage("Please manually enable 'Display over other apps' permission:\n\n" +
                                    "1. Go to Settings\n" +
                                    "2. Search for 'Display over other apps' or 'Draw over other apps'\n" +
                                    "3. Find 'ImeLocker' in the list\n" +
                                    "4. Enable the permission\n" +
                                    "5. Return to this screen")
                            .setPositiveButton("Open Settings") { _, _ ->
                                try {
                                    startActivity(Intent(Settings.ACTION_SETTINGS))
                                } catch (e3: Exception) {
                                    Log.e(TAG, "Failed to open settings", e3)
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            } else {
                Toast.makeText(this, "Overlay permission already granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestAccessibilityPermission() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            accessibilityLauncher.launch(intent)
            Toast.makeText(this, "Find ImeLocker in the list and enable it", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open accessibility settings", e)
            Toast.makeText(this, "Could not open accessibility settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.fromParts("package", packageName, null)
                batteryOptimizationLauncher.launch(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open battery optimization settings", e)
                try {
                    // Fallback to battery optimization list
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    batteryOptimizationLauncher.launch(intent)
                    Toast.makeText(this, "Find ImeLocker and disable battery optimization", Toast.LENGTH_LONG).show()
                } catch (e2: Exception) {
                    Toast.makeText(this, "Could not open battery settings", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun requestUsageStatsPermission() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            usageStatsLauncher.launch(intent)
            Toast.makeText(this, "Find ImeLocker in the list and enable it", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open usage stats settings", e)
            Toast.makeText(this, "Could not open usage stats settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePermissionStatus() {
        var grantedCount = 0
        val totalCount = 9 // Updated to 9 permissions (removed autostart)

        // Location
        val locationGranted = permissionManager.areLocationPermissionsGranted()
        tvLocationStatus.text = if (locationGranted) "✅" else "❌"
        if (locationGranted) grantedCount++

        // Background Location
        val backgroundLocationGranted = permissionManager.isBackgroundLocationGranted()
        tvBackgroundLocationStatus.text = if (backgroundLocationGranted) "✅" else "❌"
        if (backgroundLocationGranted) grantedCount++

        // Notifications
        val notificationGranted = permissionManager.isNotificationPermissionGranted()
        tvNotificationStatus.text = if (notificationGranted) "✅" else "❌"
        if (notificationGranted) grantedCount++

        // Phone State (READ_PHONE_STATE)
        val phoneStateGranted = permissionManager.isPermissionGranted(Manifest.permission.READ_PHONE_STATE)
        tvPhoneStateStatus.text = if (phoneStateGranted) "✅" else "❌"
        if (phoneStateGranted) grantedCount++

        // Device Admin
        val deviceAdminActive = dpm.isAdminActive(compName)
        tvDeviceAdminStatus.text = if (deviceAdminActive) "✅" else "❌"
        if (deviceAdminActive) grantedCount++

        // Overlay
        val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
        tvOverlayStatus.text = if (overlayGranted) "✅" else "❌"
        if (overlayGranted) grantedCount++

        // Accessibility
        val accessibilityGranted = isAccessibilityServiceEnabled()
        tvAccessibilityStatus.text = if (accessibilityGranted) "✅" else "❌"
        if (accessibilityGranted) grantedCount++

        // Battery Optimization
        val batteryOptimizationExempt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            powerManager.isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }
        tvBatteryOptimizationStatus.text = if (batteryOptimizationExempt) "✅" else "❌"
        if (batteryOptimizationExempt) grantedCount++

        // Usage Stats
        val usageStatsGranted = isUsageStatsPermissionGranted()
        tvUsageStatsStatus.text = if (usageStatsGranted) "✅" else "❌"
        if (usageStatsGranted) grantedCount++

        // Update status text
        tvPermissionStatus.text = "$grantedCount/$totalCount Granted"

        // Enable/disable complete button
        val allGranted = grantedCount == totalCount
        btnCompleteSetup.isEnabled = allGranted

        if (allGranted) {
            tvCompleteHint.text = "All permissions granted! Tap to continue"
            tvCompleteHint.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            tvCompleteHint.text = "Grant all permissions to continue (${totalCount - grantedCount} remaining)"
            tvCompleteHint.setTextColor(getColor(android.R.color.holo_red_dark))
        }

        Log.i(TAG, "Permission status updated: $grantedCount/$totalCount granted")
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(
                contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            ) == 1
        } catch (e: Exception) {
            false
        }

        if (!accessibilityEnabled) return false

        val serviceName = "$packageName/${packageName}.services.EmiAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )

        return enabledServices?.contains(serviceName) == true
    }

    private fun isUsageStatsPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    packageName
                )
            }
            return mode == android.app.AppOpsManager.MODE_ALLOWED
        }
        return true
    }

    private fun completeSetup() {
        Log.i(TAG, "Setup completed! All permissions granted.")

        // Save completion status
        SetupPrefsHelper.setAllPermissionsGranted(this, true)
        SetupPrefsHelper.setSetupCompleted(this, true)
        SetupPrefsHelper.setDeviceAdminActivated(this, dpm.isAdminActive(compName))
        SetupPrefsHelper.setLastPermissionCheck(this, System.currentTimeMillis())

        Toast.makeText(
            this,
            "✅ Setup completed successfully!",
            Toast.LENGTH_LONG
        ).show()

        // Navigate to MainActivity
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

