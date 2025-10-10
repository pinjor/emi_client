package com.example.emilockerclient.commands

import android.content.Context
import android.util.Log
import com.example.emilockerclient.managers.DeviceControlManager
import com.example.emilockerclient.network.ServerCommand
import com.google.gson.Gson

object CommandHandler {
    private const val TAG = "CommandHandler"
    private val gson = Gson()

    fun handle(context: Context, cmd: ServerCommand) {
        Log.i(TAG, "⚡ Handling command: ${cmd.command} → Payload: ${gson.toJson(cmd.payload)}")

        val manager = DeviceControlManager(context)

        // Check device ownership before executing sensitive commands
        if (!manager.isDeviceOwner()) {
            Log.w(TAG, "🚫 Device is not owner → cannot execute '${cmd.command}' command")
            return
        }

        try {
            when (cmd.command.lowercase().trim()) {
                "lock" -> {
                    val message = cmd.payload?.get("message") ?: "Device locked by admin."
                    Log.i(TAG, "🔒 Executing LOCK command: message='$message'")
                    manager.showLockScreen(message)
                }

                "unlock" -> {
                    Log.i(TAG, "🔓 Executing UNLOCK command")
                    manager.clearLock()
                }

                "disable-camera" -> {
                    Log.i(TAG, "📷 Disabling camera")
                    manager.disableCamera()
                }

                "enable-camera" -> {
                    Log.i(TAG, "📷 Enabling camera")
                    manager.enableCamera()
                }

                "disable-bluetooth" -> {
                    Log.i(TAG, "🔵 Disabling Bluetooth")
                    manager.disableBluetooth()
                }

                "enable-bluetooth" -> {
                    Log.i(TAG, "🔵 Enabling Bluetooth")
                    manager.enableBluetooth()
                }

                else -> {
                    Log.w(TAG, "⚠️ Unknown command received: ${cmd.command}")
                }
            }

            Log.i(TAG, "✅ Command '${cmd.command}' handled successfully")

        } catch (t: Throwable) {
            Log.e(TAG, "💥 Error executing command '${cmd.command}': ${t.message}", t)
        }
    }
}
