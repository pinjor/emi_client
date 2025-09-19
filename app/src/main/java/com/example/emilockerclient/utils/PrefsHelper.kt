package com.example.emilockerclient.utils


import android.content.Context
import android.content.SharedPreferences

object PrefsHelper {
    private const val PREFS_NAME = "emi_prefs"
    private const val KEY_LOCKED = "locked"
    private const val KEY_LOCK_MSG = "lock_message"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setLocked(context: Context, locked: Boolean) {
        prefs(context).edit().putBoolean(KEY_LOCKED, locked).apply()
    }

    fun isLocked(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_LOCKED, false)
    }

    fun setLockMessage(context: Context, message: String) {
        prefs(context).edit().putString(KEY_LOCK_MSG, message).apply()
    }

    fun getLockMessage(context: Context): String {
        return prefs(context).getString(KEY_LOCK_MSG, "Your EMI payment is overdue. Contact seller.") ?: ""
    }
}