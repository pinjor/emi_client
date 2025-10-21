package com.example.emilockerclient.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.emilockerclient.network.LocationData
import com.example.emilockerclient.network.LocationResponseRequest
import com.google.android.gms.location.*
import java.text.SimpleDateFormat
import java.util.*

class LocationService(private val context: Context) {

    private val TAG = "LocationService"

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(deviceSerial: String, callback: (LocationResponseRequest?) -> Unit) {
        val fused = LocationServices.getFusedLocationProviderClient(context)

        fused.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    callback(buildLocationRequest(deviceSerial, location.latitude, location.longitude, location.accuracy, location.time))
                } else {
                    requestFreshLocation(fused, deviceSerial, callback)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get last location: ${e.message}")
                callback(null)
            }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun requestFreshLocation(
        fused: FusedLocationProviderClient,
        serial: String,
        callback: (LocationResponseRequest?) -> Unit
    ) {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMaxUpdates(1)
            .build()

        fused.requestLocationUpdates(request, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation
                if (loc != null) {
                    callback(buildLocationRequest(serial, loc.latitude, loc.longitude, loc.accuracy, loc.time))
                } else {
                    callback(null)
                }
                fused.removeLocationUpdates(this)
            }
        }, Looper.getMainLooper())
    }

    private fun buildLocationRequest(
        serial: String,
        lat: Double,
        lon: Double,
        acc: Float,
        time: Long
    ): LocationResponseRequest {
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(time))

        return LocationResponseRequest(
            device_id = serial,
            data = LocationData(lat, lon, acc, timestamp)
        )
    }
}
