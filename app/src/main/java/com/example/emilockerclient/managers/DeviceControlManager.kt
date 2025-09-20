package com.example.emilockerclient.managers

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.emilockerclient.admin.EmiAdminReceiver
import com.example.emilockerclient.ui.LockScreenActivity
import com.example.emilockerclient.workers.HeartbeatWorker
import com.example.emilockerclient.utils.PrefsHelper
import java.math.BigInteger

class DeviceControlManager(private val context: Context) {
    private val TAG = "DeviceControlManager"

    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val compName = ComponentName(context, EmiAdminReceiver::class.java)

    fun isAdminActive(): Boolean {
        val active = dpm.isAdminActive(compName)
        Log.i(TAG, "isAdminActive() called = $active")
        return active
    }

    fun isDeviceOwner(): Boolean {
        val isOwner = dpm.isDeviceOwnerApp(context.packageName)
        Log.i(TAG, "isDeviceOwner() called = $isOwner")
        return isOwner
    }

    fun lockDevice() {
        Log.i(TAG, "lockDevice() called")
        try {
            dpm.lockNow()
        } catch (e: Exception) {
            Log.w(TAG, "lockNow() failed: ${e.message}")
        }
    }

    fun showLockScreen(message: String) {
        Log.i(TAG, "showLockScreen(): $message")

        PrefsHelper.setLocked(context, true)
        PrefsHelper.setLockMessage(context, message)

        val intent = Intent(context, LockScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("LOCK_MESSAGE", message)
        }
        context.startActivity(intent)

        if (dpm.isAdminActive(compName)) {
            try {
                dpm.lockNow()
            } catch (e: Exception) {
                Log.w(TAG, "dpm.lockNow() exception: ${e.message}")
            }
        }

        val job = OneTimeWorkRequestBuilder<HeartbeatWorker>().build()
        WorkManager.getInstance(context).enqueue(job)
    }

    fun clearLock() {
        Log.i(TAG, "clearLock() called")

        PrefsHelper.setLocked(context, false)
        PrefsHelper.setLockMessage(context, "")

        val unlockIntent = Intent("com.example.emilockerclient.ACTION_UNLOCK")
        context.sendBroadcast(unlockIntent)

        val job = OneTimeWorkRequestBuilder<HeartbeatWorker>().build()
        WorkManager.getInstance(context).enqueue(job)
    }

    fun enforceFrpProtection(lock: Boolean) {
        if (!isDeviceOwner()) {
            Log.w(TAG, "Not a device owner, cannot toggle FRP lock.")
            return
        }

        try {
            dpm.setAccountManagementDisabled(compName, "com.google", lock)
            if (lock) {
                Log.i(TAG, "✅ FRP enforced: Google account removal disabled")
            } else {
                Log.i(TAG, "⚠️ FRP relaxed: Google account removal allowed")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to toggle FRP account lock: ${e.message}")
        }
    }


    fun applyRestrictions() {
        if (!isDeviceOwner()) {
            Log.w(TAG, "Not a device owner, cannot apply restrictions.")
            return
        }

        Log.i(TAG, "Applying restrictions...")
        // User restrictions
        dpm.setCameraDisabled(compName, true)
        dpm.addUserRestriction(compName, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
        dpm.addUserRestriction(compName, UserManager.DISALLOW_BLUETOOTH)

        // Password policies --- currently not enforced, will see later if needed
//        dpm.setPasswordQuality(compName, DevicePolicyManager.PASSWORD_QUALITY_NUMERIC)
//        dpm.setPasswordMinimumLength(compName, 6)
//        dpm.setPasswordHistoryLength(compName, 5)

        // Keyguard features
        // This constant is a BigInteger, so we convert it to an Int.
        // It's the simplest way to disable all keyguard features.
        dpm.setKeyguardDisabledFeatures(compName, DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_ALL.toInt())

        // Remote wipe is also possible using the wipeData() method
        // dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE)
    }

    fun clearRestrictions() {
        if (!isDeviceOwner()) {
            Log.w(TAG, "Not a device owner, cannot clear restrictions.")
            return
        }

        Log.i(TAG, "Clearing restrictions...")
        // User restrictions
        dpm.setCameraDisabled(compName, false)
        dpm.clearUserRestriction(compName, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
        dpm.clearUserRestriction(compName, UserManager.DISALLOW_BLUETOOTH)

        // Password policies -- will see later if we really need to reset these
//        dpm.setPasswordQuality(compName, DevicePolicyManager.PASSWORD_QUALITY_NUMERIC)
//        dpm.setPasswordMinimumLength(compName, 0)
//        dpm.setPasswordHistoryLength(compName, 0)

        // Keyguard features
        dpm.setKeyguardDisabledFeatures(compName, DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_NONE.toInt())
    }
}
