// commands/CommandHandler.kt

package com.example.emilockerclient.commands

import android.content.Context
import android.util.Log
import com.example.emilockerclient.managers.DeviceControlManager
import com.example.emilockerclient.network.ServerCommand
import com.google.gson.Gson
import kotlin.math.log

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
            "set_wallpaper", "remove_wallpaper",
            "enable_call", "disable_call",
            "lock_usb", "unlock_usb"

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
                    Log.i(TAG, "Locking device as per command.")
                    val title = cmd.getString("title") ?: "Payment Required"
                    val message = cmd.getString("message") ?: "Device locked by admin."

                    manager.showLockScreen(title, message)
                }

                "unlock_device", "unlock" -> {
                    Log.i(TAG, "Unlocking device as per command.")
                    manager.clearLock()
                }

                "disable_camera" -> {
                    Log.i(TAG, "Disabling camera as per command.")
                    manager.disableCamera()
                }

                "enable_camera" -> {
                    Log.i(TAG, "Enabling camera as per command.")
                    manager.enableCamera()
                }

                "disable_bluetooth" -> {
                    Log.i(TAG, "Disabling Bluetooth as per command.")
                    manager.disableBluetooth()
                }

                "enable_bluetooth" -> {
                    Log.i(TAG, "Enabling Bluetooth as per command.")
                    manager.enableBluetooth()
                }

                "enable_call" ->{
                    Log.i(TAG, "Enabling outgoing call and SMS as per command.")
                    manager.enableOutgoingCallAndSMS()
                }

                "disable_call" ->{
                    Log.i(TAG, "Disabling outgoing call and SMS as per command.")
                    manager.disableOutgoingCallAndSMS()
                }

                "lock_usb" ->{
                    Log.i(TAG, "Locking USB data as per command.")
                    manager.applyUsbAdbRestrictions()
                }

                "unlock_usb" ->{
                    Log.i(TAG, "Unlocking USB data as per command.")
                    manager.clearUsbAdbRestrictions()
                }

                "hide_app" -> {
                    Log.i(TAG, "Received HIDE_APP command -> hiding current app.")
                    manager.toggleSelfVisibility(true)
                }

                "unhide_app" -> {
                    Log.i(TAG, "Received UNHIDE_APP command -> unhiding current app.")
                    manager.toggleSelfVisibility(false)
                }


                "remove_app" -> {
                    Log.i(TAG, "Executing remove_app command: release device owner")
                    manager.cleanupAndReleaseDeviceOwner()
                }

                "reboot_device" -> {
                    Log.i(TAG, "Rebooting device as per command.")
                    manager.rebootDevice()
                }

//                "reset_password" -> {
//                    Log.i(TAG, "Resetting device password as per command.")
//                    val pwd = cmd.getString("password")
//                    if (pwd != null) manager.resetDevicePassword(pwd)
//                    else {
//                        reason = "missing_password"
//                        status = "FAILED"
//                    }
//                }
//
//                "remove_password" -> {
//                    Log.i(TAG, "Removing device password as per command.")
//                    manager.clearDevicePassword()
//                }
//
//                "set_wallpaper" -> {
//                    Log.i(TAG, "Setting wallpaper as per command.")
//                    val url = cmd.getString("image_url")
//                    if (url != null) manager.setWallpaperFromUrl(url)
//                    else {
//                        reason = "missing_image_url"
//                        status = "FAILED"
//                    }
//                }
//
                "remove_wallpaper" -> {
                    Log.i(TAG, "Removing wallpaper as per command.")
                    manager.removeWallpaper()


                }

                "show_message", "reminder_screen" -> {
                    val message = cmd.getString("message") ?: cmd.getString("title") ?: ""
                    manager.showLockScreen("Please Pay the EMI Due",message)
                }

//                "reminder_audio" -> {
//                    val audioUrl = cmd.getString("audio_url")
//                    if (audioUrl != null) manager.playAudioReminder(audioUrl)
//                    else {
//                        reason = "missing_audio_url"
//                        status = "FAILED"
//                    }
//                }

                "request_location" -> {
                    Log.i(TAG, "Requesting device location as per command.")
                    // manager.requestLocation will fetch and (optionally) send back to server
                    manager.requestLocation { locResult ->
                        // optional: you could send location back as part of ack (not implemented by default)
                        Log.i(TAG, "LOCATION RESULT: $locResult")
                    }
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
