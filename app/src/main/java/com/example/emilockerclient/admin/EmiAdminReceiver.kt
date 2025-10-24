package com.example.emilockerclient.admin

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.widget.Toast

class EmiAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val compName = ComponentName(context, EmiAdminReceiver::class.java)

        if (dpm.isDeviceOwnerApp(context.packageName)) {
            // 🚫 Core protections - only block factory reset
            // Device Owner apps are automatically protected from uninstall, no need for DISALLOW_UNINSTALL_APPS
            dpm.addUserRestriction(compName, UserManager.DISALLOW_FACTORY_RESET)

            // ❌ DO NOT lock Google account here.
            // Let admin add their Google account first, then enforce FRP via DeviceControlManager.
            // Do NOT automatically block account management here unless you are 100% sure
            // the admin Google account(s) are already added and synced. It's safer to call
            // setAccountManagementDisabled from provisioning flow after account added.
        }
        if(dpm.isDeviceOwnerApp(context.packageName)){
            Toast.makeText(context, "ImeLocker is now Device Owner!", Toast.LENGTH_SHORT).show()
        }else{
            Toast.makeText(context, "ImeLocker is now Admin!", Toast.LENGTH_SHORT).show()

        }
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "..admin disabled..", Toast.LENGTH_SHORT).show()
    }
}
