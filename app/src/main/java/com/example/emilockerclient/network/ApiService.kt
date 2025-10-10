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

// Standard response structure (you can adjust if backend returns different fields)

// Generic command structure for FCM and server responses
data class ServerCommand(
    val command: String,  // e.g. "lock", "unlock", "disable-camera"
    val payload: Map<String, String>? = null
)

// Optional command acknowledgment (if server expects one)
data class CommandAckRequest(
    val serial_number: String,
    val command: String,
    val status: String // e.g. "SUCCESS" / "FAILED"
)


// Retrofit API interface

interface ApiService {

    // Register the device with serial, imei, and FCM token
    @POST("devices/register")
    fun registerDevice(@Body req: DeviceRegisterRequest): Call<DeviceRegistrationResponse>

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

