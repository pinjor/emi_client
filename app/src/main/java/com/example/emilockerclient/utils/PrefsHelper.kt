package com.example.emilockerclient.utils

import android.content.Context
import android.content.SharedPreferences

object PrefsHelper {

    private const val PREFS_NAME = "emi_prefs"
    private const val KEY_LOCKED = "locked"
    private const val KEY_LOCK_TITLE = "lock_title"
    private const val KEY_LOCK_MSG = "lock_message"
    private const val KEY_LAST_HEARTBEAT = "last_heartbeat"
    private const val KEY_DIALER_ACTIVE = "dialer_active" // New flag for dialer state

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setLocked(context: Context, locked: Boolean) {
        prefs(context).edit().putBoolean(KEY_LOCKED, locked).apply()
    }

    fun isLocked(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_LOCKED, false)
    }

    fun setLockTitle(context: Context, title: String) {
        prefs(context).edit().putString(KEY_LOCK_TITLE, title).apply()
    }

    fun getLockTitle(context: Context): String {
        return prefs(context).getString(KEY_LOCK_TITLE, "Payment Required") ?: "Payment Required"
    }

    fun setLockMessage(context: Context, message: String) {
        prefs(context).edit().putString(KEY_LOCK_MSG, message).apply()
    }

    fun getLockMessage(context: Context): String {
        return prefs(context).getString(KEY_LOCK_MSG, "Your EMI payment is overdue. Contact seller.") ?: ""
    }

    fun setLastHeartbeatTime(context: Context, time: Long) {
        prefs(context).edit().putLong(KEY_LAST_HEARTBEAT, time).apply()
    }

    fun getLastHeartbeatTime(context: Context): Long {
        return prefs(context).getLong(KEY_LAST_HEARTBEAT, 0L)
    }

    // New methods for dialer state management
    fun setDialerActive(context: Context, active: Boolean) {
        prefs(context).edit().putBoolean(KEY_DIALER_ACTIVE, active).apply()
    }

    fun isDialerActive(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_DIALER_ACTIVE, false)
    }
}
