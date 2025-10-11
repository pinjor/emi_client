// commands/CommandHandler.kt

package com.example.emilockerclient.commands

import android.content.Context
import android.util.Log
import com.example.emilockerclient.managers.DeviceControlManager
import com.example.emilockerclient.network.ServerCommand
import com.google.gson.Gson

object CommandHandler {
    private const val TAG = "CommandHandler"
    private val gson = Gson()

    /**
     * Handle a server command. This function is safe to call from FCM service thread.
     * It attempts to run the command and sends an async ack to server.
     */
    fun handle(context: Context, cmd: ServerCommand) {
        val manager = DeviceControlManager(context)
        val normalized = cmd.normalized()
        Log.i(TAG, "Handling command='$normalized' params=${gson.toJson(cmd.params)}")

        // Many commands change device state - require device owner
        val deviceOwnerRequired = setOf(
            "lock_device", "unlock_device",
            "disable_camera", "enable_camera",
            "disable_bluetooth", "enable_bluetooth",
            "hide_app", "unhide_app",
            "remove_app", "reboot_device",
            "wipe_device", "reset_password",
            "remove_password", "apply_restrictions",
            "clear_restrictions",
            "set_wallpaper", "remove_wallpaper"
        )

        if (normalized in deviceOwnerRequired && !manager.isDeviceOwner()) {
            Log.w(TAG, "Cannot execute '$normalized' — app is not device owner.")
            // sendAck(context, cmd, "FAILED", "NotDeviceOwner") // will be uncommented when server supports it InshaAllah
            return
        }

        // Result tracking
        var status = "SUCCESS"
        var reason: String? = null
        try {
            when (normalized) {
                "lock_device", "lock" -> {
                    val message = cmd.getString("message") ?: "Device locked by admin."
                    manager.showLockScreen(message)
                }
                "unlock_device", "unlock" -> {
                    manager.clearLock()
                }
                "disable_camera" -> manager.disableCamera()
                "enable_camera" -> manager.enableCamera()
                "disable_bluetooth" -> manager.disableBluetooth()
                "enable_bluetooth" -> manager.enableBluetooth()
                "hide_app" -> {
                    val pkg = cmd.getString("package_name")
                    val state = cmd.getBoolean("state", true)
                    if (pkg != null) manager.hideApp(pkg, state)
                    else {
                        reason = "missing_package_name"
                        status = "FAILED"
                    }
                }
                "unhide_app" -> {
                    val pkg = cmd.getString("package_name")
                    if (pkg != null) manager.hideApp(pkg, false)
                    else {
                        reason = "missing_package_name"
                        status = "FAILED"
                    }
                }
                "remove_app" -> {
                    val pkg = cmd.getString("package_name")
                    if (pkg != null) manager.removeApp(pkg)
                    else {
                        reason = "missing_package_name"
                        status = "FAILED"
                    }
                }
                "reboot_device" -> manager.rebootDevice()
                "reset_password" -> {
                    val pwd = cmd.getString("password")
                    if (pwd != null) manager.resetDevicePassword(pwd)
                    else {
                        reason = "missing_password"
                        status = "FAILED"
                    }
                }
                "remove_password" -> manager.clearDevicePassword()
                "set_wallpaper" -> {
                    val url = cmd.getString("image_url")
                    if (url != null) manager.setWallpaperFromUrl(url)
                    else {
                        reason = "missing_image_url"
                        status = "FAILED"
                    }
                }
                "remove_wallpaper" -> manager.removeWallpaper()
                "show_message", "reminder_screen" -> {
                    val message = cmd.getString("message") ?: cmd.getString("title") ?: ""
                    manager.showLockScreen(message)
                }
                "reminder_audio" -> {
                    val audioUrl = cmd.getString("audio_url")
                    if (audioUrl != null) manager.playAudioReminder(audioUrl)
                    else {
                        reason = "missing_audio_url"
                        status = "FAILED"
                    }
                }
                "request_location" -> {
                    // manager.requestLocation will fetch and (optionally) send back to server
                    manager.requestLocation { locResult ->
                        // optional: you could send location back as part of ack (not implemented by default)
                        Log.i(TAG, "LOCATION RESULT: $locResult")
                    }
                }
                "wipe_device" -> {
                    // Very destructive - ensure you want this
                    manager.wipeDevice()
                }
                else -> {
                    Log.w(TAG, "Unknown command: $normalized")
                    status = "FAILED"
                    reason = "unknown_command"
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error executing command $normalized: ${t.message}", t)
            status = "FAILED"
            reason = t.message
        }

        // ack to server (best-effort)
        // sendAck(context, cmd, status, reason) // will be uncommented when server supports it InshaAllah
    }

    // private fun sendAck(context: Context, cmd: ServerCommand, status: String, reason: String? = null) {
    //     try {
    //         val serial = com.example.emilockerclient.managers.DeviceIdentifierFetcher(context, com.example.emilockerclient.admin.EmiAdminReceiver().componentNameDummy()).let {
    //             // avoid throwing — attempt to fetch serial if possible — may throw SecurityException
    //             try { it.getSerialNumber() } catch (e: Exception) { null }
    //         }
    //         val ack = CommandAckRequest(
    //             serial_number = serial ?: "UNKNOWN",
    //             command = cmd.command,
    //             status = status + (reason?.let{"|$it"} ?: "")
    //         )
    //         Log.i(TAG, "Sending command ACK to server: ${gson.toJson(ack)}")
    //         RetrofitClient.api.ackCommand(ack).enqueue(object : Callback<com.example.emilockerclient.network.ApiResponse> {
    //             override fun onResponse(call: Call<com.example.emilockerclient.network.ApiResponse>, response: Response<com.example.emilockerclient.network.ApiResponse>) {
    //                 if (response.isSuccessful) Log.i(TAG, "ACK accepted by server: ${response.body()?.message}")
    //                 else Log.w(TAG, "ACK HTTP ${response.code()} → ${response.errorBody()?.string()}")
    //             }
    //             override fun onFailure(call: Call<com.example.emilockerclient.network.ApiResponse>, t: Throwable) {
    //                 Log.w(TAG, "ACK failed: ${t.message}")
    //             }
    //         })
    //     } catch (e: Exception) {
    //         Log.w(TAG, "Failed to send ack: ${e.message}")
    //     }
    // }
}
