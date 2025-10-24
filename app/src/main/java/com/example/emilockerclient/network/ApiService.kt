package com.example.emilockerclient.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// -------------------------------
// ✅ Request/Response Models
// -------------------------------

// Device registration payload
data class DeviceRegisterRequest(
    val serial_number: String,
    val imei1: String,
    val fcm_token: String
)


/**
 * Generic command model parsed from FCM data payload.
 *
 * Example incoming data:
 * { "command":"DISABLE_CAMERA", "state":"true" }
 * { "command":"HIDE_APP", "package_name":"com.facebook.katana", "state":"true" }
 * { "command":"SHOW_MESSAGE", "message":"..." }
 *
 * We keep parameters in a Map for flexibility and add helpers below.
 */
data class ServerCommand(
    val command: String,
    val params: Map<String, String>? = null
) {
    // Helper: normalized command (lowercase, trimmed)
    fun normalized(): String = command.trim().lowercase()

    // Common param readers
    fun getString(key: String, default: String? = null): String? =
        params?.get(key) ?: default

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        params?.get(key)?.let { it.equals("true", ignoreCase = true) || it == "1" } ?: default
}


// Optional command acknowledgment (if server expects one)
data class CommandAckRequest(
    val serial_number: String,
    val command: String,
    val status: String // e.g. "SUCCESS" / "FAILED"
)



// location response payload
// -------------------------------
// 📍 Location Response Model
// -------------------------------
data class LocationResponseRequest(
    val device_id: String,
    val command: String = "REQUEST_LOCATION",
    val data: LocationData
)

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: String
)

// -------------------------------
// 📍 Periodic Location Tracking Model
// -------------------------------
data class PeriodicLocationRequest(
    val device_id: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double,
    val recorded_at: String,
    val metadata: LocationMetadata
)

data class LocationMetadata(
    val battery_level: Int,
    val network_type: String
)


// Retrofit API interface

interface ApiService {

    // Register the device with serial, imei, and FCM token
    @POST("devices/register")
    fun registerDevice(@Body req: DeviceRegisterRequest): Call<DeviceRegistrationResponse>

    // Periodic location tracking endpoint
    @POST("devices/location")
    fun sendPeriodicLocation(@Body req: PeriodicLocationRequest): Call<ApiResponse>

    @POST("devices/command-response")
    fun sendLocationResponse(@Body req: LocationResponseRequest): Call<ApiResponse>

    // Command endpoints (invoked dynamically when receiving FCM commands)
    @POST("devices/command/lock")
    fun lockDevice(): Call<ApiResponse>

    @POST("devices/command/unlock")
    fun unlockDevice(): Call<ApiResponse>

    @POST("devices/command/disable-camera")
    fun disableCamera(): Call<ApiResponse>

    @POST("devices/command/enable-camera")
    fun enableCamera(): Call<ApiResponse>

    @POST("devices/command/disable-bluetooth")
    fun disableBluetooth(): Call<ApiResponse>

    // Optional: acknowledge command execution
    @POST("devices/command/ack")
    fun ackCommand(@Body req: CommandAckRequest): Call<ApiResponse>
}
