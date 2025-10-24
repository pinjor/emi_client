package com.example.emilockerclient.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

/**
 * Helper class to check internet connectivity and verify backend reachability
 */
object ConnectivityHelper {

    private const val TAG = "ConnectivityHelper"
    private const val PING_TIMEOUT_MS = 5000 // 5 seconds

    /**
     * Check if device has an active internet connection
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo != null && networkInfo.isConnected
        }
    }

    /**
     * Ping backend server to verify actual internet connectivity
     * This runs a real HTTP request to ensure the device can reach the internet
     *
     * @param serverUrl The backend server URL to ping (e.g., "https://your-backend.com/api/ping")
     * @return True if server is reachable, false otherwise
     */
    fun pingBackendServer(serverUrl: String): Boolean {
        return try {
            val url = URL(serverUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = PING_TIMEOUT_MS
            connection.readTimeout = PING_TIMEOUT_MS
            connection.connect()

            val responseCode = connection.responseCode
            connection.disconnect()

            val success = responseCode in 200..299 || responseCode == 404 // 404 is OK, means server is reachable
            Log.i(TAG, "Backend ping result: $responseCode (${if (success) "reachable" else "unreachable"})")
            success

        } catch (e: Exception) {
            Log.w(TAG, "Backend ping failed: ${e.message}")
            false
        }
    }

    /**
     * Comprehensive internet check:
     * 1. Check if network is available
     * 2. Ping a reliable server (Google DNS or your backend)
     *
     * @return True if device has working internet, false otherwise
     */
    fun hasWorkingInternet(context: Context, backendUrl: String? = null): Boolean {
        // First check: Network available?
        if (!isNetworkAvailable(context)) {
            Log.i(TAG, "No network connection available")
            return false
        }

        // Second check: Can we actually reach the internet?
        val pingUrl = backendUrl ?: "https://www.google.com"
        return pingBackendServer(pingUrl)
    }
}

