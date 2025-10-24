package com.example.emilockerclient.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.util.Log

/**
 * Helper utilities for fetching device metadata (battery level, network type)
 */
object DeviceMetadataHelper {

    private const val TAG = "DeviceMetadataHelper"

    /**
     * Get current battery level as a percentage (0-100)
     */
    fun getBatteryLevel(context: Context): Int {
        return try {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (batteryIntent != null) {
                val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    ((level.toFloat() / scale.toFloat()) * 100).toInt()
                } else {
                    -1
                }
            } else {
                -1
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get battery level: ${e.message}")
            -1
        }
    }

    /**
     * Get current network type (WiFi, 4G, 5G, 3G, 2G, etc.)
     */
    fun getNetworkType(context: Context): String {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return "None"
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Unknown"

                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        // Try to get cellular network type
                        getCellularNetworkType(context)
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                    else -> "Unknown"
                }
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                when (networkInfo?.type) {
                    ConnectivityManager.TYPE_WIFI -> "WiFi"
                    ConnectivityManager.TYPE_MOBILE -> getCellularNetworkType(context)
                    ConnectivityManager.TYPE_ETHERNET -> "Ethernet"
                    else -> "Unknown"
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get network type: ${e.message}")
            "Unknown"
        }
    }

    /**
     * Get specific cellular network type (4G, 5G, 3G, etc.)
     */
    @Suppress("DEPRECATION")
    private fun getCellularNetworkType(context: Context): String {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Wrap in try-catch to handle SecurityException
                try {
                    when (telephonyManager.dataNetworkType) {
                        android.telephony.TelephonyManager.NETWORK_TYPE_NR -> "5G"
                        android.telephony.TelephonyManager.NETWORK_TYPE_LTE -> "4G"
                        android.telephony.TelephonyManager.NETWORK_TYPE_EHRPD,
                        android.telephony.TelephonyManager.NETWORK_TYPE_HSPAP,
                        android.telephony.TelephonyManager.NETWORK_TYPE_HSPA,
                        android.telephony.TelephonyManager.NETWORK_TYPE_HSUPA,
                        android.telephony.TelephonyManager.NETWORK_TYPE_HSDPA,
                        android.telephony.TelephonyManager.NETWORK_TYPE_UMTS,
                        android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_0,
                        android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_A,
                        android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_B -> "3G"
                        android.telephony.TelephonyManager.NETWORK_TYPE_GPRS,
                        android.telephony.TelephonyManager.NETWORK_TYPE_EDGE,
                        android.telephony.TelephonyManager.NETWORK_TYPE_CDMA,
                        android.telephony.TelephonyManager.NETWORK_TYPE_1xRTT,
                        android.telephony.TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
                        else -> "Mobile"
                    }
                } catch (se: SecurityException) {
                    Log.w(TAG, "Permission denied for network type: ${se.message}")
                    "Mobile"
                }
            } else {
                "Mobile"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get cellular network type: ${e.message}")
            "Mobile"
        }
    }
}
