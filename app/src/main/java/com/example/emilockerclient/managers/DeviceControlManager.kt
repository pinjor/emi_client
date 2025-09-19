package com.example.emilockerclient.managers

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
        val active = dpm.isAdminActive(compName)
        Log.i(TAG, "isAdminActive() called = $active")
        return active
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

    fun applyRestrictions() {
        // Requires Device Owner - keep commented until owner provisioning
        // dpm.addUserRestriction(compName, UserManager.DISALLOW_FACTORY_RESET)
    }

    fun clearRestrictions() {
        // Requires Device Owner
        // dpm.clearUserRestriction(compName, UserManager.DISALLOW_FACTORY_RESET)
    }
}
