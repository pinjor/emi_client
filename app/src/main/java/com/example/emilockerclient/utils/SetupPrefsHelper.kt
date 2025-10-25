package com.example.emilockerclient.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Helper class to manage setup preferences for both Device Owner and Admin modes
 */
object SetupPrefsHelper {
    private const val PREFS_NAME = "emi_setup_prefs"

    // Keys
    private const val KEY_IS_DEVICE_OWNER = "is_device_owner"
    private const val KEY_DEVICE_MODE = "device_mode"
    private const val KEY_SETUP_COMPLETED = "setup_completed"
    private const val KEY_REGISTRATION_COMPLETED = "registration_completed"
    private const val KEY_REGISTERED_IMEI = "registered_imei"
    private const val KEY_REGISTERED_FCM_TOKEN = "registered_fcm_token"
    private const val KEY_ALL_PERMISSIONS_GRANTED = "all_permissions_granted"
    private const val KEY_LAST_PERMISSION_CHECK = "last_permission_check_timestamp"
    private const val KEY_DEVICE_ADMIN_ACTIVATED = "device_admin_activated"

    // Device modes
    const val MODE_DEVICE_OWNER = "device_owner"
    const val MODE_ADMIN = "admin"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Device mode
    fun setDeviceMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_DEVICE_MODE, mode).apply()
    }

    fun getDeviceMode(context: Context): String? {
        return getPrefs(context).getString(KEY_DEVICE_MODE, null)
    }

    fun isDeviceOwnerMode(context: Context): Boolean {
        return getDeviceMode(context) == MODE_DEVICE_OWNER
    }

    fun isAdminMode(context: Context): Boolean {
        return getDeviceMode(context) == MODE_ADMIN
    }

    // Setup status
    fun setSetupCompleted(context: Context, completed: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SETUP_COMPLETED, completed).apply()
    }

    fun isSetupCompleted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SETUP_COMPLETED, false)
    }

    // Registration status
    fun setRegistrationCompleted(context: Context, completed: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_REGISTRATION_COMPLETED, completed).apply()
    }

    fun isRegistrationCompleted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_REGISTRATION_COMPLETED, false)
    }

    // Registered IMEI
    fun setRegisteredImei(context: Context, imei: String) {
        getPrefs(context).edit().putString(KEY_REGISTERED_IMEI, imei).apply()
    }

    fun getRegisteredImei(context: Context): String? {
        return getPrefs(context).getString(KEY_REGISTERED_IMEI, null)
    }

    // FCM Token
    fun setRegisteredFcmToken(context: Context, token: String) {
        getPrefs(context).edit().putString(KEY_REGISTERED_FCM_TOKEN, token).apply()
    }

    fun getRegisteredFcmToken(context: Context): String? {
        return getPrefs(context).getString(KEY_REGISTERED_FCM_TOKEN, null)
    }

    // Permissions
    fun setAllPermissionsGranted(context: Context, granted: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ALL_PERMISSIONS_GRANTED, granted).apply()
    }

    fun areAllPermissionsGranted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ALL_PERMISSIONS_GRANTED, false)
    }

    fun setLastPermissionCheck(context: Context, timestamp: Long) {
        getPrefs(context).edit().putLong(KEY_LAST_PERMISSION_CHECK, timestamp).apply()
    }

    fun getLastPermissionCheck(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_PERMISSION_CHECK, 0)
    }

    // Device Admin
    fun setDeviceAdminActivated(context: Context, activated: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DEVICE_ADMIN_ACTIVATED, activated).apply()
    }

    fun isDeviceAdminActivated(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DEVICE_ADMIN_ACTIVATED, false)
    }

    // Clear all setup data (useful for testing or reset)
    fun clearAllSetupData(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}

