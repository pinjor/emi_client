// managers/DeviceControlManager.kt
package com.example.emilockerclient.managers

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.UserManager
import android.provider.Settings
import android.util.Log
import com.example.emilockerclient.admin.EmiAdminReceiver
import com.example.emilockerclient.network.ApiResponse
import com.example.emilockerclient.network.RetrofitClient
import com.example.emilockerclient.services.LocationService
import com.example.emilockerclient.ui.LockScreenActivity
import com.example.emilockerclient.utils.PrefsHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.URL
import kotlin.concurrent.thread

class DeviceControlManager(private val context: Context) {
    private val TAG = "DeviceControlManager"
    private val dpm: DevicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val compName = ComponentName(context, EmiAdminReceiver::class.java)

    fun isDeviceOwner(): Boolean = try { dpm.isDeviceOwnerApp(context.packageName) } catch (e: Exception) { Log.w(TAG, e.message?:""); false }

    fun isAdminActive(): Boolean = try { dpm.isAdminActive(compName) } catch (e: Exception) { Log.w(TAG, e.message?:""); false }

    fun lockDevice() = try { dpm.lockNow() } catch (e: Exception) { Log.w(TAG, "lockNow failed: ${e.message}") }

    fun showLockScreen(message: String) {
        Log.i(TAG, "showLockScreen(): $message")
        try {
            PrefsHelper.setLocked(context, true)
            PrefsHelper.setLockMessage(context, message)
            val intent = Intent(context, LockScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("LOCK_MESSAGE", message)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "startActivity(lock) failed: ${e.message}")
        }
        if (isAdminActive()) try { dpm.lockNow() } catch (e: Exception) { Log.w(TAG, "dpm.lockNow() exception: ${e.message}") }
    }

    fun clearLock() {
        Log.i(TAG, "clearLock()")
        try {
            PrefsHelper.setLocked(context, false)
            PrefsHelper.setLockMessage(context, "")
            // Add a closing parenthesis to fix the syntax error
//            context.sendBroadcast(Intent("com.example.emilockerclient.ACTION_UNLOCK"), null, null)
        } catch (e: Exception) {
            Log.w(TAG, "clearLock failed: ${e.message}")
        }
    }


    // Camera
    fun disableCamera() { if (!isDeviceOwner()) return; try { dpm.setCameraDisabled(compName, true) } catch (e: Exception) { Log.w(TAG, "disableCamera failed: ${e.message}") } }
    fun enableCamera()  { if (!isDeviceOwner()) return; try { dpm.setCameraDisabled(compName, false) } catch (e: Exception) { Log.w(TAG, "enableCamera failed: ${e.message}") } }

    // Bluetooth (we use user restriction as device owner)
    fun disableBluetooth() { if (!isDeviceOwner()) return; try { dpm.addUserRestriction(compName, android.os.UserManager.DISALLOW_BLUETOOTH) } catch (e: Exception) { Log.w(TAG, "disableBluetooth failed: ${e.message}") } }
    fun enableBluetooth()  { if (!isDeviceOwner()) return; try { dpm.clearUserRestriction(compName, android.os.UserManager.DISALLOW_BLUETOOTH) } catch (e: Exception) { Log.w(TAG, "enableBluetooth failed: ${e.message}") } }

    // Toggle app visibility (hide/unhide) via DevicePolicyManager + PackageManager
    fun toggleSelfVisibility(hide: Boolean) {
        if (!isDeviceOwner()) {
            Log.w(TAG, "toggleSelfVisibility: not device owner")
            return
        }

        val packageName = context.packageName
        val pm = context.packageManager
        val mainLauncher = ComponentName(packageName, "com.example.emilockerclient.MainActivity")

        try {
            // Step 0: Check current states
            val isCurrentlyHidden = try {
                dpm.isApplicationHidden(compName, packageName)
            } catch (e: Exception) {
                Log.w(TAG, "isApplicationHidden failed: ${e.message}")
                false
            }

            val componentState = pm.getComponentEnabledSetting(mainLauncher)
            val isLauncherDisabled = componentState == PackageManager.COMPONENT_ENABLED_STATE_DISABLED

            val alreadyHidden = hide && isCurrentlyHidden && isLauncherDisabled
            val alreadyVisible = !hide && !isCurrentlyHidden && componentState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED

            if (alreadyHidden) {
                Log.i(TAG, "App is already hidden, skipping toggle.")
                return
            }

            if (alreadyVisible) {
                Log.i(TAG, "App is already visible, skipping toggle.")
                return
            }

            Log.i(TAG, "toggleSelfVisibility: hide=$hide for $packageName")

            // Step 1: Hide/unhide via DPM
            try {
                dpm.setApplicationHidden(compName, packageName, hide)
                Log.i(TAG, "DPM.setApplicationHidden($packageName,$hide) OK")
            } catch (e: Exception) {
                Log.w(TAG, "setApplicationHidden failed: ${e.message}")
            }

            // Step 2: Toggle launcher activity manually
            val newState = if (hide)
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            else
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED

            try {
                pm.setComponentEnabledSetting(mainLauncher, newState, PackageManager.DONT_KILL_APP)
                Log.i(TAG, "MainActivity launcher -> $newState")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to toggle MainActivity: ${e.message}")
            }

            // Step 3: Force launcher refresh (for unhide)
            if (!hide) {
                try {
                    val launcherIntent = Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME)
                    val resolveInfo = pm.resolveActivity(launcherIntent, 0)
                    if (resolveInfo != null) {
                        val launcherPkg = resolveInfo.activityInfo.packageName
                        Runtime.getRuntime().exec("am force-stop $launcherPkg")
                        Log.i(TAG, "Launcher ($launcherPkg) force-stopped to refresh icons")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Launcher refresh failed: ${e.message}")
                }
            }

            Log.i(TAG, "toggleSelfVisibility complete (hide=$hide)")
        } catch (e: Exception) {
            Log.e(TAG, "toggleSelfVisibility failed: ${e.message}")
        }
    }





    /**
     * Remove/uninstall app (best-effort). Many OEMs restrict silent uninstall even for Device Owner.
     * We try to block/unblock uninstall flag then use package installer (may prompt UI).
     */
    fun cleanupAndReleaseDeviceOwner() {
        val packageName = context.packageName
        Log.i(TAG, "Starting self-remove process for $packageName")

        try {
            // Step 1: Restore all device features
            try {
                enableCamera()
                enableBluetooth()
                clearUsbAdbRestrictions()
                enableOutgoingCallAndSMS()
                Log.i(TAG, "All device features restored to normal")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to restore device features: ${e.message}")
            }

            // Step 2: Clear device owner status
            try {
                if (isDeviceOwner()) {
                    dpm.clearDeviceOwnerApp(packageName)
                    Log.i(TAG, "Device owner privileges removed")
                } else {
                    Log.i(TAG, "Not device owner, nothing to clear")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear device owner: ${e.message}")
            }

            Log.i(TAG, "Self-remove process complete. App can now be uninstalled by the user.")
        } catch (e: Exception) {
            Log.e(TAG, "removeAppSelf failed: ${e.message}")
        }
    }






    // Reboot (best-effort; requires permission/device owner)
    fun rebootDevice() {
        if (!isDeviceOwner()) { Log.w(TAG, "rebootDevice: not device owner"); return }
        try {
            // DevicePolicyManager.reboot(admin) is available on some API levels - wrap in try/catch
            val m = DevicePolicyManager::class.java.getMethod("reboot", ComponentName::class.java)
            m.invoke(dpm, compName)
            Log.i(TAG, "rebootDevice invoked via reflection")
        } catch (e: Exception) {
            Log.w(TAG, "rebootDevice failed (reflection): ${e.message}")
        }
    }

    // Wipe device (very destructive)
    fun wipeDevice() {
        if (!isDeviceOwner()) { Log.w(TAG, "wipeDevice: not device owner"); return }
        try {
            dpm.wipeData(0) // this will factory reset the device immediately (very destructive action - use with caution)
            Log.i(TAG, "wipeData() invoked")
        } catch (e: Exception) {
            Log.w(TAG, "wipeDevice failed: ${e.message}")
        }
    }

    // Password management
    fun resetDevicePassword(newPassword: String) {
        if (!isDeviceOwner()) { Log.w(TAG, "resetDevicePassword: not device owner"); return }
        try {
            val success = dpm.resetPassword(newPassword, DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY)
            Log.i(TAG, "resetPassword invoked -> $success")
        } catch (e: Exception) {
            Log.w(TAG, "resetDevicePassword failed: ${e.message}")
        }
    }

    fun clearDevicePassword() {
        if (!isDeviceOwner()) { Log.w(TAG, "clearDevicePassword: not device owner"); return }
        try {
            dpm.setPasswordQuality(compName, DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED)
            dpm.resetPassword("", 0)
            Log.i(TAG, "clearDevicePassword invoked")
        } catch (e: Exception) {
            Log.w(TAG, "clearDevicePassword failed: ${e.message}")
        }
    }

    // Wallpaper utilities (download + set). Runs on background thread (best-effort).
    fun setWallpaperFromUrl(url: String) {
        thread {
            try {
                Log.i(TAG, "Downloading wallpaper: $url")
                val bitmap = BitmapFactory.decodeStream(URL(url).openStream())
                // DevicePolicyManager has setWallpaper for device owner via setWallpaper(...) on some API
                // As a fallback we can set the lockscreen/wallpaper via WallpaperManager (may need permissions)
                val wm = android.app.WallpaperManager.getInstance(context)
                wm.setBitmap(bitmap)
                Log.i(TAG, "Wallpaper set from URL")
            } catch (e: Exception) {
                Log.w(TAG, "setWallpaperFromUrl failed: ${e.message}")
            }
        }
    }

    fun removeWallpaper() {
        try {
            val wm = android.app.WallpaperManager.getInstance(context)
            wm.clear()
            Log.i(TAG, "Wallpaper cleared")
        } catch (e: Exception) {
            Log.w(TAG, "removeWallpaper failed: ${e.message}")
        }
    }

    // Audio reminder: download & play (best-effort). Keep it short and non-blocking.
    fun playAudioReminder(url: String) {
        thread {
            try {
                Log.i(TAG, "Downloading audio: $url")
                // TODO: implement secure download and play via MediaPlayer
                // For now we log the action
                Log.i(TAG, "Would play audio reminder from: $url")
            } catch (e: Exception) {
                Log.w(TAG, "playAudioReminder failed: ${e.message}")
            }
        }
    }

    // Request Location (best-effort): you must ensure location permissions are granted and the device owner policy allows location retrieval.
    fun requestLocation(callback: (String) -> Unit) {
        if (!isDeviceOwner()) { Log.w(TAG, "Device Owner is not enabled, exiting location fething..."); return }

        thread {
            try {
                Log.i(TAG, "Fetching device location...")

                // Get device serial number properly
                val identifierFetcher = DeviceIdentifierFetcher(context, compName)
                val serial = try {
                    identifierFetcher.getSerialNumber()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get serial number: ${e.message}")
                    "UNKNOWN"
                }

                Log.i(TAG, "📱 Device ID (Serial): $serial")

                val locationService = LocationService(context)
                locationService.getCurrentLocation(serial) { locationReq ->
                    if (locationReq == null) {
                        Log.w(TAG, "Failed to get location data")
                        callback("FAILED_TO_GET_LOCATION")
                        return@getCurrentLocation
                    }

                    // Use existing Retrofit API
                    Log.i(TAG, "📍 Location obtained: lat=${locationReq.data.latitude}, lon=${locationReq.data.longitude}, accuracy=${locationReq.data.accuracy}m")
                    Log.i(TAG, "📅 Timestamp: ${locationReq.data.timestamp}")
                    Log.i(TAG, "📤 Sending location to backend...")
                    Log.i(TAG, "📦 Full payload: device_id=${locationReq.device_id}, command=${locationReq.command}")
                    Log.i(TAG, "   → data: {lat=${locationReq.data.latitude}, lon=${locationReq.data.longitude}, accuracy=${locationReq.data.accuracy}, timestamp=${locationReq.data.timestamp}}")

                    val call = RetrofitClient.api.sendLocationResponse(locationReq)
                    call.enqueue(object : Callback<ApiResponse> {
                        override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                            if (response.isSuccessful) {
                                Log.i(TAG, "✅ Location sent successfully: ${response.body()?.message}")
                                callback("LOCATION_SENT")
                            } else {
                                Log.w(TAG, "❌ Failed to send location: ${response.code()} ${response.message()}")
                                callback("FAILED_TO_SEND")
                            }
                        }

                        override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                            Log.e(TAG, "🌐 Network error sending location: ${t.message}")
                            callback("NETWORK_ERROR:${t.message}")
                        }
                    })
                }

            } catch (e: Exception) {
                Log.e(TAG, "requestLocation() failed: ${e.message}", e)
                callback("ERROR:${e.message}")
            }
        }
    }




    //    disable outgoing calls and sms
    fun disableOutgoingCallAndSMS(){
        if (!isDeviceOwner()) {
            Log.w(TAG, "Cannot block: not device owner")
            return
        }
        try {
            // Block outgoing calls
            dpm.addUserRestriction(compName, UserManager.DISALLOW_OUTGOING_CALLS)
            // Block SMS sending
            dpm.addUserRestriction(compName, UserManager.DISALLOW_SMS)
            Log.i(TAG, "Outgoing calls and SMS blocked")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to block outgoing calls/SMS: ${e.message}")
        }
    }

//    enable all types of calls and SMS

    fun enableOutgoingCallAndSMS(){
        if (!isDeviceOwner()) {
            Log.w(TAG, "Cannot unblock: not device owner")
            return
        }
        try {
            // Unblock outgoing calls
            dpm.clearUserRestriction(compName, UserManager.DISALLOW_OUTGOING_CALLS)
            // Unblock SMS sending
            dpm.clearUserRestriction(compName, UserManager.DISALLOW_SMS)
            Log.i(TAG, "Outgoing calls and SMS unblocked")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unblock outgoing calls/SMS: ${e.message}")
        }
    }

    // Call to apply restrictions: disable USB file transfer, disable USB data signalling, disable ADB & wireless debugging (best-effort)
    fun applyUsbAdbRestrictions() {
        if (!isDeviceOwner()) {
            Log.w(TAG, "applyUsbAdbRestrictions: not device owner")
            return
        }

        try {
            // 1) Block USB file transfer (MTP/PTP) via user restriction
            try {
                dpm.addUserRestriction(compName, android.os.UserManager.DISALLOW_USB_FILE_TRANSFER)
                Log.i(TAG, "DISALLOW_USB_FILE_TRANSFER applied")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to add DISALLOW_USB_FILE_TRANSFER: ${e.message}")
            }

            // 2) Disable deeper USB data signaling (Android 11+ / API 30+ / may require device owner)
            try {
                // device owner API: setUsbDataSignalingEnabled(boolean)
                // guard by reflection/dpm method timeout if needed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S /* API 31 */) {
                    try {
                        // direct call if available
                        dpm.setUsbDataSignalingEnabled(false)
                        Log.i(TAG, "setUsbDataSignalingEnabled(false) invoked")
                    } catch (noMethod: NoSuchMethodError) {
                        // fallback by reflection (just in case)
                        try {
                            val m = DevicePolicyManager::class.java.getMethod("setUsbDataSignalingEnabled", Boolean::class.javaPrimitiveType)
                            m.invoke(dpm, false)
                            Log.i(TAG, "setUsbDataSignalingEnabled(false) invoked via reflection")
                        } catch (re: Exception) {
                            Log.w(TAG, "setUsbDataSignalingEnabled not available: ${re.message}")
                        }
                    }
                } else {
                    Log.i(TAG, "USB data signaling disable not supported on SDK < 31")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to disable USB data signaling: ${e.message}")
            }

            // 3) Disable ADB over USB (global setting "adb_enabled")
            try {
                // set to "0" to disable
                dpm.setGlobalSetting(compName, Settings.Global.ADB_ENABLED, "0")
                Log.i(TAG, "Global setting ADB_ENABLED set to 0")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set ADB_ENABLED global: ${e.message}")
            }

            // 4) Best-effort: disable some wireless adb globals (names vary by platform)
            // Try a couple of commonly seen keys; many OEMs don't expose a single standard key for wireless debugging.
            val wirelessKeys = listOf(
                "adb_wifi_enabled",          // used on some platforms
                "adb_wireless",              // possible key on others (best-effort)
                "airplane_adb_enabled",      // unlikely but harmless to try
                "wifi_adb_enabled"           // alternate name
            )
            try {
                for (key in wirelessKeys) {
                    try {
                        dpm.setGlobalSetting(compName, key, "0")
                        Log.i(TAG, "Attempted to set global $key -> 0")
                    } catch (e: Exception) {
                        // ignore individual failures
                    }
                }
            } catch (_: Exception) {}

            Log.i(TAG, "applyUsbAdbRestrictions complete (best-effort).")
        } catch (e: Exception) {
            Log.e(TAG, "applyUsbAdbRestrictions failed: ${e.message}")
        }
    }

    // Call to clear restrictions: enable USB data transfer, enable USB signaling, enable ADB & wireless debugging (best-effort)
    fun clearUsbAdbRestrictions() {
        if (!isDeviceOwner()) {
            Log.w(TAG, "clearUsbAdbRestrictions: not device owner")
            return
        }

        try {
            // 1) Clear USB file transfer restriction
            try {
                dpm.clearUserRestriction(compName, android.os.UserManager.DISALLOW_USB_FILE_TRANSFER)
                Log.i(TAG, "DISALLOW_USB_FILE_TRANSFER cleared")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear DISALLOW_USB_FILE_TRANSFER: ${e.message}")
            }

            // 2) Re-enable USB data signaling if supported
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S /* API 31 */) {
                    try {
                        dpm.setUsbDataSignalingEnabled(true)
                        Log.i(TAG, "setUsbDataSignalingEnabled(true) invoked")
                    } catch (noMethod: NoSuchMethodError) {
                        try {
                            val m = DevicePolicyManager::class.java.getMethod("setUsbDataSignalingEnabled", Boolean::class.javaPrimitiveType)
                            m.invoke(dpm, true)
                            Log.i(TAG, "setUsbDataSignalingEnabled(true) via reflection")
                        } catch (re: Exception) {
                            Log.w(TAG, "setUsbDataSignalingEnabled not available: ${re.message}")
                        }
                    }
                } else {
                    Log.i(TAG, "USB data signaling enable not supported on SDK < 31")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to enable USB data signaling: ${e.message}")
            }

            // 3) Re-enable ADB over USB (global setting "adb_enabled" -> "1")
            try {
                dpm.setGlobalSetting(compName, Settings.Global.ADB_ENABLED, "1")
                Log.i(TAG, "Global setting ADB_ENABLED set to 1")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set ADB_ENABLED global: ${e.message}")
            }

            // 4) Best-effort: enable wireless adb globals (may not exist)
            val wirelessKeys = listOf(
                "adb_wifi_enabled",
                "adb_wireless",
                "wifi_adb_enabled"
            )
            try {
                for (key in wirelessKeys) {
                    try {
                        dpm.setGlobalSetting(compName, key, "1")
                        Log.i(TAG, "Attempted to set global $key -> 1")
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            } catch (_: Exception) {}

            Log.i(TAG, "clearUsbAdbRestrictions complete (best-effort).")
        } catch (e: Exception) {
            Log.e(TAG, "clearUsbAdbRestrictions failed: ${e.message}")
        }
    }

}
