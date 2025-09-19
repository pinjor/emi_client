package com.example.emilockerclient.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

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
    val message: String
)

interface ApiService {
    @POST("heartbeat") // Replace with actual endpoint later
    fun sendHeartbeat(@Body request: HeartbeatRequest): Call<HeartbeatResponse>
}
