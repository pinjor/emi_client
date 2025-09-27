package com.example.emilockerclient.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class RegisterRequest(
    val deviceId: String,
    val model: String,
    val imei: String? = null,
    val simSerial: String? = null,
    val orderId: String? = null
)
data class RegisterResponse(val success: Boolean, val message: String, val deviceToken: String? = null)

data class HeartbeatRequest(
    val deviceId: String,
    val imei: String?,
    val simSerial: String?,
    val model: String,
    val isLocked: Boolean,
    val timestamp: Long
)
data class HeartbeatResponse(
    val success: Boolean,
    val message: String,
    val command: ServerCommand? = null // server may include immediate command to execute
)

data class ServerCommand(
    val type: String, // e.g. "LOCK_DEVICE", "UNLOCK_DEVICE", "SHOW_MESSAGE"
    val payload: Map<String, String>? = null
)

data class CommandAckRequest(val deviceId: String, val commandType: String, val status: String)

data class FcmTokenRegisterRequest(val deviceId: String, val fcmToken: String)

interface ApiService {
    @POST("device/register")
    fun registerDevice(@Body req: RegisterRequest): Call<RegisterResponse>

    @POST("device/heartbeat")
    fun sendHeartbeat(@Body req: HeartbeatRequest): Call<HeartbeatResponse>

    @POST("device/commandAck")
    fun ackCommand(@Body req: CommandAckRequest): Call<RegisterResponse>

    @POST("device/registerFcm")
    fun registerFcm(@Body req: FcmTokenRegisterRequest): Call<RegisterResponse>
}
