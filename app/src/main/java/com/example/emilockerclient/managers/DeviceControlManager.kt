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

class DeviceControlManager(private val context: Context) {
    private val TAG = "DeviceControlManager"

    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val compName = ComponentName(context, EmiAdminReceiver::class.java)

    fun isAdminActive(): Boolean {
        val active = try {
            dpm.isAdminActive(compName)
        } catch (e: Exception) {
            Log.w(TAG, "isAdminActive() failed: ${e.message}")
            false
        }
        Log.i(TAG, "isAdminActive() = $active")
        return active
    }

    fun isDeviceOwner(): Boolean {
        val isOwner = try {
            dpm.isDeviceOwnerApp(context.packageName)
        } catch (e: Exception) {
            Log.w(TAG, "isDeviceOwner() failed: ${e.message}")
            false
        }
        Log.i(TAG, "isDeviceOwner() = $isOwner")
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

    /**
     * Show the custom lock screen and set local locked flag.
     * This is idempotent and safe to call multiple times.
     */
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

        // If we're an admin, also call lockNow() — best effort
        if (isAdminActive()) {
            try {
                dpm.lockNow()
            } catch (e: Exception) {
                Log.w(TAG, "dpm.lockNow() exception: ${e.message}")
            }
        }

        // Ensure backend is notified quickly
        try {
            val job = OneTimeWorkRequestBuilder<HeartbeatWorker>().build()
            WorkManager.getInstance(context).enqueue(job)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to enqueue heartbeat job: ${e.message}")
        }
    }

    /**
     * Clear local lock state and broadcast unlock action.
     * Backend should also be updated by enqueuing a heartbeat.
     */
    fun clearLock() {
        Log.i(TAG, "clearLock() called")
        try {
            PrefsHelper.setLocked(context, false)
            PrefsHelper.setLockMessage(context, "")
            val unlockIntent = Intent("com.example.emilockerclient.ACTION_UNLOCK")
            context.sendBroadcast(unlockIntent)
        } catch (e: Exception) {
            Log.w(TAG, "clearLock() failed: ${e.message}")
        }

        try {
            val job = OneTimeWorkRequestBuilder<HeartbeatWorker>().build()
            WorkManager.getInstance(context).enqueue(job)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to enqueue heartbeat job: ${e.message}")
        }
    }

    /**
     * Toggle FRP-style protection (disallow account removal for given accountType).
     * accountType examples: "com.google" (Google accounts)
     */
    fun enforceFrpProtection(lock: Boolean, accountType: String = "com.google") {
        if (!isDeviceOwner()) {
            Log.w(TAG, "Not a device owner, cannot toggle FRP lock.")
            return
        }

        try {
            dpm.setAccountManagementDisabled(compName, accountType, lock)
            Log.i(TAG, "FRP toggle: $accountType -> $lock")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to toggle FRP account lock: ${e.message}")
        }
    }


    /**
     * Apply the minimal set of always-on restrictions (we keep this conservative).
     * We avoid aggressive policies that would break legitimate user flow unless client explicitly requests them.
     *
     * This function should be called only when Device Owner is active.
     */

    fun applyRestrictions() {
        if (!isDeviceOwner()) {
            Log.w(TAG, "Not a device owner, cannot apply restrictions.")
            return
        }

        Log.i(TAG, "Applying restrictions (best-effort)...")

        try {
            // Keep these minimal and business-focused:

            // Optional: disable mounting external storage if needed
            dpm.addUserRestriction(compName, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)

            // Keyguard restrictions: disable some features (safe)

            // Keyguard features
            // This constant is a BigInteger, so we convert it to an Int.
            // It's the simplest way to disable all keyguard features.
            dpm.setKeyguardDisabledFeatures(compName, DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_ALL.toInt())

            // Disable camera and Bluetooth (if needed)
            dpm.setCameraDisabled(compName, true)
            dpm.addUserRestriction(compName, UserManager.DISALLOW_BLUETOOTH)

            // Password policies --- currently not enforced, will see later if needed
            //        dpm.setPasswordQuality(compName, DevicePolicyManager.PASSWORD_QUALITY_NUMERIC)
            //        dpm.setPasswordMinimumLength(compName, 6)
            //        dpm.setPasswordHistoryLength(compName, 5)


            // Remote wipe is also possible using the wipeData() method
            // dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE)

        }catch (e : Exception){
            Log.w(TAG, "applyRestrictions failed: ${e.message}")


        }
    }


    /**
     * Clear the restrictions applied above.
     */
    fun clearRestrictions() {
        if (!isDeviceOwner()) {
            Log.w(TAG, "Not a device owner, cannot clear restrictions.")
            return
        }

        Log.i(TAG, "Clearing restrictions...")

        try {
            // User restrictions
            dpm.setCameraDisabled(compName, false)
            dpm.clearUserRestriction(compName, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
            dpm.clearUserRestriction(compName, UserManager.DISALLOW_BLUETOOTH)
            dpm.setKeyguardDisabledFeatures(compName, DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_NONE.toInt())


            // Password policies -- will see later if we really need to reset these
//        dpm.setPasswordQuality(compName, DevicePolicyManager.PASSWORD_QUALITY_NUMERIC)
//        dpm.setPasswordMinimumLength(compName, 0)
//        dpm.setPasswordHistoryLength(compName, 0)

            // Keyguard features
            dpm.setKeyguardDisabledFeatures(compName, DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_NONE.toInt())
        }catch(e: Exception){
            Log.w(TAG, "clearRestrictions failed: ${e.message}")

        }

    }
}
