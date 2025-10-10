package com.example.emilockerclient.managers

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log
import com.example.emilockerclient.admin.EmiAdminReceiver
import com.example.emilockerclient.ui.LockScreenActivity
import com.example.emilockerclient.utils.PrefsHelper

class DeviceControlManager(private val context: Context) {
    private val TAG = "DeviceControlManager"
    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val compName = ComponentName(context, EmiAdminReceiver::class.java)

    fun isAdminActive(): Boolean = try {
        dpm.isAdminActive(compName)
    } catch (e: Exception) {
        Log.w(TAG, "isAdminActive() failed: ${e.message}")
        false
    }

    fun isDeviceOwner(): Boolean = try {
        dpm.isDeviceOwnerApp(context.packageName)
    } catch (e: Exception) {
        Log.w(TAG, "isDeviceOwner() failed: ${e.message}")
        false
    }

    fun lockDevice() {
        try { dpm.lockNow() } catch (e: Exception) { Log.w(TAG, "lockNow() failed: ${e.message}") }
    }

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
        try {
            PrefsHelper.setLocked(context, false)
            PrefsHelper.setLockMessage(context, "")
            context.sendBroadcast(Intent("com.example.emilockerclient.ACTION_UNLOCK"))
        } catch (e: Exception) {
            Log.w(TAG, "clearLock() failed: ${e.message}")
        }
    }

    fun enforceFrpProtection(lock: Boolean, accountType: String = "com.google") {
        if (!isDeviceOwner()) return
        try {
            dpm.setAccountManagementDisabled(compName, accountType, lock)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to toggle FRP account lock: ${e.message}")
        }
    }

    fun applyRestrictions() {
        if (!isDeviceOwner()) return
        try {
            dpm.addUserRestriction(compName, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
            dpm.setKeyguardDisabledFeatures(compName, DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_ALL.toInt())
            dpm.setCameraDisabled(compName, true)
            dpm.addUserRestriction(compName, UserManager.DISALLOW_BLUETOOTH)
        } catch (e: Exception) {
            Log.w(TAG, "applyRestrictions failed: ${e.message}")
        }
    }

    fun clearRestrictions() {
        if (!isDeviceOwner()) return
        try {
            dpm.setCameraDisabled(compName, false)
            dpm.clearUserRestriction(compName, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
            dpm.clearUserRestriction(compName, UserManager.DISALLOW_BLUETOOTH)
            dpm.setKeyguardDisabledFeatures(compName, DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_NONE.toInt())
        } catch (e: Exception) {
            Log.w(TAG, "clearRestrictions failed: ${e.message}")
        }
    }

    // ✅ Methods required by CommandHandler
    fun disableCamera() {
        if (!isDeviceOwner()) return
        try { dpm.setCameraDisabled(compName, true) } catch (e: Exception) { Log.w(TAG, "disableCamera() failed: ${e.message}") }
    }

    fun enableCamera() {
        if (!isDeviceOwner()) return
        try { dpm.setCameraDisabled(compName, false) } catch (e: Exception) { Log.w(TAG, "enableCamera() failed: ${e.message}") }
    }

    fun disableBluetooth() {
        if (!isDeviceOwner()) return
        try { dpm.addUserRestriction(compName, UserManager.DISALLOW_BLUETOOTH) } catch (e: Exception) { Log.w(TAG, "disableBluetooth() failed: ${e.message}") }
    }

    fun enableBluetooth() {
        if (!isDeviceOwner()) return
        try { dpm.clearUserRestriction(compName, UserManager.DISALLOW_BLUETOOTH) } catch (e: Exception) { Log.w(TAG, "enableBluetooth() failed: ${e.message}") }
    }
}
