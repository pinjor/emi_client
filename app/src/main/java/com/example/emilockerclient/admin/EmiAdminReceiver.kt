package com.example.emilockerclient.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class EmiAdminReceiver : DeviceAdminReceiver() {
    // Called when the admin is enabled
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "EMI Admin Enabled", Toast.LENGTH_SHORT).show()
    }

    // Called when the admin is disabled
    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "EMI Admin Disabled", Toast.LENGTH_SHORT).show()
    }
}
