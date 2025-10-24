package com.example.emilockerclient.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.emilockerclient.managers.DeviceIdentifierFetcher
import com.example.emilockerclient.network.LocationMetadata
import com.example.emilockerclient.network.PeriodicLocationRequest
import com.example.emilockerclient.network.RetrofitClient
import com.example.emilockerclient.utils.ConnectivityHelper
import com.example.emilockerclient.utils.DeviceMetadataHelper
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

/**
 * LocationTrackingWorker - Periodically tracks device location and sends to backend
 *
 * Features:
 * - Runs every 1 hour
 * - Fetches current location (latitude, longitude, accuracy)
 * - Collects metadata (battery level, network type)
 * - Only sends when device is online
 * - Handles failures gracefully
 */
class LocationTrackingWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    companion object {
        private const val TAG = "LocationTrackingWorker"
    }

    override fun doWork(): Result {
        val context = applicationContext
        Log.i(TAG, "🌍 Location tracking worker started...")

        return try {
            // Step 1: Check if device is online
            if (!ConnectivityHelper.isNetworkAvailable(context)) {
                Log.w(TAG, "⚠️ Device is offline - skipping location tracking")
                return Result.retry() // Retry when network is back
            }

            Log.i(TAG, "✅ Device is online - proceeding with location tracking")

            // Step 2: Get device ID (serial number)
            val deviceId = getDeviceId(context)
            if (deviceId.isNullOrEmpty()) {
                Log.e(TAG, "❌ Failed to get device ID - aborting")
                return Result.failure()
            }

            Log.i(TAG, "📱 Device ID: $deviceId")

            // Step 3: Fetch current location
            fetchAndSendLocation(context, deviceId)

            // Return success - the actual API call is async
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in location tracking: ${e.message}", e)
            Result.retry()
        }
    }

    /**
     * Get device ID (serial number)
     */
    private fun getDeviceId(context: Context): String? {
        return try {
            val fetcher = DeviceIdentifierFetcher(
                context,
                android.content.ComponentName(
                    context,
                    com.example.emilockerclient.admin.EmiAdminReceiver::class.java
                )
            )
            fetcher.getSerialNumber()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get device ID: ${e.message}")
            null
        }
    }

    /**
     * Fetch current location and send to backend
     */
    private fun fetchAndSendLocation(context: Context, deviceId: String) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        Log.i(TAG, "📍 Location obtained: lat=${location.latitude}, lon=${location.longitude}, accuracy=${location.accuracy}m")

                        // Collect metadata
                        val batteryLevel = DeviceMetadataHelper.getBatteryLevel(context)
                        val networkType = DeviceMetadataHelper.getNetworkType(context)

                        Log.i(TAG, "🔋 Battery: $batteryLevel%, 📶 Network: $networkType")

                        // Build timestamp in ISO 8601 format (UTC)
                        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                            .apply { timeZone = TimeZone.getTimeZone("UTC") }
                            .format(Date())

                        // Create request payload
                        val request = PeriodicLocationRequest(
                            device_id = deviceId,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy.toDouble(),
                            recorded_at = timestamp,
                            metadata = LocationMetadata(
                                battery_level = batteryLevel,
                                network_type = networkType
                            )
                        )

                        // Send to backend
                        sendLocationToBackend(request)

                    } else {
                        Log.w(TAG, "⚠️ Location is null - requesting fresh location")
                        requestFreshLocation(context, deviceId)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to get location: ${e.message}")
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Location permission denied: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching location: ${e.message}")
        }
    }

    /**
     * Request fresh location if last known location is not available
     */
    private fun requestFreshLocation(context: Context, deviceId: String) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        try {
            val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                5000 // 5 seconds
            )
                .setMaxUpdates(1)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                        val loc = result.lastLocation
                        if (loc != null) {
                            Log.i(TAG, "📍 Fresh location obtained: lat=${loc.latitude}, lon=${loc.longitude}")

                            val batteryLevel = DeviceMetadataHelper.getBatteryLevel(context)
                            val networkType = DeviceMetadataHelper.getNetworkType(context)

                            val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                                .format(Date())

                            val request = PeriodicLocationRequest(
                                device_id = deviceId,
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                accuracy = loc.accuracy.toDouble(),
                                recorded_at = timestamp,
                                metadata = LocationMetadata(
                                    battery_level = batteryLevel,
                                    network_type = networkType
                                )
                            )

                            sendLocationToBackend(request)
                        }
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                },
                android.os.Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Location permission denied for fresh request: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error requesting fresh location: ${e.message}")
        }
    }

    /**
     * Send location data to backend API
     */
    private fun sendLocationToBackend(request: PeriodicLocationRequest) {
        Log.i(TAG, "📤 Sending location to backend...")
        Log.i(TAG, "   Device: ${request.device_id}")
        Log.i(TAG, "   Location: ${request.latitude}, ${request.longitude}")
        Log.i(TAG, "   Accuracy: ${request.accuracy}m")
        Log.i(TAG, "   Battery: ${request.metadata.battery_level}%")
        Log.i(TAG, "   Network: ${request.metadata.network_type}")
        Log.i(TAG, "   Time: ${request.recorded_at}")

        val call = RetrofitClient.api.sendPeriodicLocation(request)
        call.enqueue(object : Callback<com.example.emilockerclient.network.ApiResponse> {
            override fun onResponse(
                call: Call<com.example.emilockerclient.network.ApiResponse>,
                response: Response<com.example.emilockerclient.network.ApiResponse>
            ) {
                if (response.isSuccessful) {
                    Log.i(TAG, "✅ Location sent successfully: ${response.body()?.message}")
                } else {
                    Log.w(TAG, "⚠️ Backend responded with error: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<com.example.emilockerclient.network.ApiResponse>, t: Throwable) {
                Log.e(TAG, "❌ Network error sending location: ${t.message}")
            }
        })
    }
}

