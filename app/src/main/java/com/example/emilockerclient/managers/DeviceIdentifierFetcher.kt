package com.example.emilockerclient.managers

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import com.example.emilockerclient.admin.EmiAdminReceiver



class DeviceIdentifierFetcher(
    private val context: Context,
    private val adminReceiver: ComponentName
) {

    class DeviceIdAccessException(message: String) : SecurityException(message)

    private val permissionManager by lazy { PermissionManager(context, adminReceiver) }

    fun getSerialNumber(): String {
        permissionManager.ensurePermissions()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Build.getSerial()
            else @Suppress("DEPRECATION") Build.SERIAL
        } catch (se: SecurityException) {
            throw DeviceIdAccessException(
                "Access Denied. Serial Number requires Device Owner or Admin privileges."
            )
        }
    }

    fun getImei(slotIndex: Int): String {
        permissionManager.ensurePermissions()
        return try {
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> telephonyManager.getImei(slotIndex)
                    ?: "N/A (Slot $slotIndex)"
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> telephonyManager.getImei(slotIndex)
                    ?: "N/A (Slot $slotIndex)"
                else -> @Suppress("DEPRECATION")
                if (slotIndex == 0) telephonyManager.deviceId ?: "N/A (Legacy Slot 0)"
                else "N/A (Legacy Dual SIM check required)"
            }
        } catch (se: SecurityException) {
            throw DeviceIdAccessException(
                "Access Denied. IMEI requires Device Owner or Admin privileges."
            )
        } catch (e: Exception) {
            "Error retrieving IMEI for slot $slotIndex: ${e.message}"
        }
    }
}
