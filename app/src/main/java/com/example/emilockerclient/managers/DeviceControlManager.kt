// managers/DeviceControlManager.kt
package com.example.emilockerclient.managers

import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.example.emilockerclient.admin.EmiAdminReceiver
import com.example.emilockerclient.ui.LockScreenActivity
import com.example.emilockerclient.utils.PrefsHelper
import java.io.File
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
                enableUSBDataTransfer()
                enableAllCallsAndSMS()
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
    fun requestLocation(callback: (String)->Unit) {
        thread {
            try {
                // TODO: implement fused location retrieval (FusedLocationProviderClient) and return a JSON string
//                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
//                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
//                    if (location != null) {
//                        val lat = location.latitude
//                        val lon = location.longitude
//                        // send to backend
//                    }
//                }


                callback("LOCATION_NOT_IMPLEMENTED")
            } catch (e: Exception) {
                Log.w(TAG, "requestLocation failed: ${e.message}")
                callback("ERROR:${e.message}")
            }
        }
    }


//    disable all types of calls and SMS
fun disableAllCallsAndSMS(){
    if (!isDeviceOwner()) { Log.w(TAG, "disableAllCallsAndSMS: not device owner"); return }
    try {
        dpm.addUserRestriction(compName, android.os.UserManager.DISALLOW_OUTGOING_CALLS)
        dpm.addUserRestriction(compName, android.os.UserManager.DISALLOW_SMS)
//        dpm.addUserRestriction(compName, android.os.UserManager.DISALLOW_MMS)
//        dpm.addUserRestriction(compName, android.os.UserManager.DISALLOW_INCOMING_CALLS)
        Log.i(TAG, "All calls and SMS disabled")
    } catch (e: Exception) {
        Log.w(TAG, "disableAllCallsAndSMS failed: ${e.message}")
    }
}

//    enable all types of calls and SMS

    fun enableAllCallsAndSMS(){
        if (!isDeviceOwner()) { Log.w(TAG, "enableCallsAndSMS: not device owner"); return }

        try {
            dpm.clearUserRestriction(compName, android.os.UserManager.DISALLOW_OUTGOING_CALLS)
            dpm.clearUserRestriction(compName, android.os.UserManager.DISALLOW_SMS)
//            dpm.clearUserRestriction(compName, android.os.UserManager.DISALLOW_MMS)
//            dpm.clearUserRestriction(compName, android.os.UserManager.DISALLOW_INCOMING_CALL
//            )
            Log.i(TAG, "All calls and SMS enabled")
        } catch (e: Exception) {
            Log.w(TAG, "enableAllCallsAndSMS failed: ${e.message}")
        }
    }

//    hide/unhide this app fully

    fun hideUnhideThisApp(hide: Boolean) {
        if (!isDeviceOwner()) { Log.w(TAG, "hideUnhideThisApp: not device owner"); return }
        try {
            val packageName = context.packageName
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dpm.setApplicationHidden(compName, packageName, hide)
                Log.i(TAG, "setApplicationHidden($packageName,$hide) OK")
            } else {
                // setApplicationHidden exists earlier too (API 24+) but be defensive
                dpm.setApplicationHidden(compName, packageName, hide)
                Log.i(TAG, "setApplicationHidden($packageName,$hide) OK (legacy path)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "hideUnhideThisApp failed: ${e.message}")
        }
    }



//    disable usb debugging, usb file transfer, otg, etc.
// Disable USB file transfer (MTP/PTP)
fun disableUSBDataTransfer() {
    if (!isDeviceOwner()) { Log.w(TAG, "disableUSBDataTransfer: not device owner"); return }
    try {
        dpm.addUserRestriction(compName, android.os.UserManager.DISALLOW_USB_FILE_TRANSFER)
        Log.i(TAG, "USB file transfer disabled")

        // Optional: deeper USB signaling block on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dpm.setUsbDataSignalingEnabled(false)
            Log.i(TAG, "USB data signaling disabled (Android 12+)")
        }
    } catch (e: Exception) {
        Log.w(TAG, "disableUSBDataTransfer failed: ${e.message}")
    }
}

    // Enable USB file transfer (optional)
    fun enableUSBDataTransfer() {
        if (!isDeviceOwner()) { Log.w(TAG, "enableUSBDataTransfer: not device owner"); return }
        try {
            dpm.clearUserRestriction(compName, android.os.UserManager.DISALLOW_USB_FILE_TRANSFER)
            Log.i(TAG, "USB file transfer enabled")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dpm.setUsbDataSignalingEnabled(true)
                Log.i(TAG, "USB data signaling enabled (Android 12+)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "enableUSBDataTransfer failed: ${e.message}")
        }
    }

    // Disable USB debugging (ADB)
    fun disableADB() {
        if (!isDeviceOwner()) { Log.w(TAG, "disableADB: not device owner"); return }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // This works only on some OEMs/devices as a global setting
                dpm.setGlobalSetting(compName, "adb_enabled", "0")
                Log.i(TAG, "ADB disabled via global setting")
            } else {
                Log.w(TAG, "disableADB: not supported on this Android version")
            }
        } catch (e: Exception) {
            Log.w(TAG, "disableADB failed: ${e.message}")
        }
    }

    // Enable ADB (optional)
    fun enableADB() {
        if (!isDeviceOwner()) { Log.w(TAG, "enableADB: not device owner"); return }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dpm.setGlobalSetting(compName, "adb_enabled", "1")
                Log.i(TAG, "ADB enabled via global setting")
            }
        } catch (e: Exception) {
            Log.w(TAG, "enableADB failed: ${e.message}")
        }
    }



}
