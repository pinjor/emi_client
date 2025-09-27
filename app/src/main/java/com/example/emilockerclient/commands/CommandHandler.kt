package com.example.emilockerclient.commands

import android.content.Context
import android.util.Log
import com.example.emilockerclient.managers.DeviceControlManager
import com.example.emilockerclient.network.ServerCommand

object CommandHandler {
    private const val TAG = "CommandHandler"

    /**
     * Handle a server command. Keep this small and idempotent.
     * The ServerCommand model is expected to have:
     *  - type: String
     *  - payload: Map<String, String>?
     *
     * If you need ack back to server, do it in the worker that calls this.
     */
    fun handle(context: Context, cmd: ServerCommand) {
        Log.i(TAG, "Handling command: ${cmd.type}")
        val manager = DeviceControlManager(context)

        try {
            when (cmd.type.uppercase().trim()) {
                "LOCK_DEVICE" -> {
                    val message = cmd.payload?.get("message") ?: "Device locked by admin."
                    manager.showLockScreen(message)
                }
                "UNLOCK_DEVICE" -> {
                    manager.clearLock()
                }
                "SHOW_MESSAGE" -> {
                    val message = cmd.payload?.get("message") ?: ""
                    // Reuse lock screen for showing message (keeps user blocked until admin clears)
                    manager.showLockScreen(message)
                }
                "APPLY_RESTRICTIONS" -> {
                    if (manager.isDeviceOwner()) manager.applyRestrictions()
                    else Log.w(TAG, "APPLY_RESTRICTIONS skipped: not device owner")
                }
                "CLEAR_RESTRICTIONS" -> {
                    if (manager.isDeviceOwner()) manager.clearRestrictions()
                    else Log.w(TAG, "CLEAR_RESTRICTIONS skipped: not device owner")
                }
                "ENFORCE_FRP" -> {
                    // payload: {"enabled":"true"}
                    val enabled = cmd.payload?.get("enabled")?.toBoolean() ?: true
                    manager.enforceFrpProtection(enabled)
                }
                else -> {
                    Log.w(TAG, "Unknown command: ${cmd.type}")
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error handling command ${cmd.type}: ${t.message}", t)
        }
    }
}
