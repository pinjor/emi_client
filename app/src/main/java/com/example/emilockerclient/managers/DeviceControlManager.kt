// managers/DeviceControlManager.kt
package com.example.emilockerclient.managers

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

    // Hide / unhide app (requires device owner)
    fun hideApp(packageName: String, hide: Boolean) {
        if (!isDeviceOwner()) { Log.w(TAG, "hideApp: not device owner"); return }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dpm.setApplicationHidden(compName, packageName, hide)
                Log.i(TAG, "setApplicationHidden($packageName,$hide) OK")
            } else {
                // setApplicationHidden exists earlier too (API 24+) but be defensive
                dpm.setApplicationHidden(compName, packageName, hide)
                Log.i(TAG, "setApplicationHidden($packageName,$hide) OK (legacy path)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "hideApp failed for $packageName: ${e.message}")
        }
    }

    /**
     * Remove/uninstall app (best-effort). Many OEMs restrict silent uninstall even for Device Owner.
     * We try to block/unblock uninstall flag then use package installer (may prompt UI).
     */
    fun removeApp(packageName: String) {
        if (!isDeviceOwner()) { Log.w(TAG, "removeApp: not device owner"); return }
        try {
            // Best-effort: ask DevicePolicyManager to uninstall (some API levels support dpm.uninstallPackage)
            // If not available, try to block/unblock uninstall or attempt package removal via PackageInstaller (may require user)
            // Keep this conservative: just try to block/unblock for now or log
            // Example: dpm.setUninstallBlocked(compName, packageName, false) // allow uninstall (if previously blocked)
            try { dpm.setUninstallBlocked(compName, packageName, false) } catch(ignore: Exception){}
            Log.i(TAG, "removeApp: attempted to allow uninstall for $packageName. Silent uninstall not universally supported.")
        } catch (e: Exception) {
            Log.w(TAG, "removeApp failed: ${e.message}")
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
                callback("LOCATION_NOT_IMPLEMENTED")
            } catch (e: Exception) {
                Log.w(TAG, "requestLocation failed: ${e.message}")
                callback("ERROR:${e.message}")
            }
        }
    }
}
