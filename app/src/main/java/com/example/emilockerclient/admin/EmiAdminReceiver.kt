package com.example.emilockerclient.admin

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log
import android.widget.Toast
import com.example.emilockerclient.MainActivity
import com.example.emilockerclient.managers.PermissionManager
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

class EmiAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "EmiAdminReceiver"
    }

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

    /**
     * Called when QR code provisioning completes successfully.
     * This is the callback after device becomes Device Owner via QR code.
     *
     * We use this to:
     * 1. Auto-grant all required permissions
     * 2. Initialize Firebase for FCM
     * 3. Launch MainActivity to complete setup
     */
    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)

        Log.i(TAG, "🎉 onProfileProvisioningComplete() called - Provisioning successful!")

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val compName = ComponentName(context, EmiAdminReceiver::class.java)

        // Verify we are Device Owner
        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            Log.e(TAG, "❌ App is not Device Owner after provisioning!")
            Toast.makeText(context, "Provisioning failed: Not Device Owner", Toast.LENGTH_LONG).show()
            return
        }

        Log.i(TAG, "✅ Confirmed: App is Device Owner")

        try {
            // 1. Auto-grant all required permissions
            Log.i(TAG, "🔐 Auto-granting permissions...")
            val permissionManager = PermissionManager(context, compName)
            permissionManager.ensurePermissions()
            Log.i(TAG, "✅ Permissions granted")

            // 2. Add core device restrictions
            Log.i(TAG, "🔒 Adding device restrictions...")
            dpm.addUserRestriction(compName, UserManager.DISALLOW_FACTORY_RESET)
            Log.i(TAG, "✅ Factory reset disabled")

            // 3. Initialize Firebase
            Log.i(TAG, "🔥 Initializing Firebase...")
            FirebaseApp.initializeApp(context)

            // Get FCM token asynchronously (for logging purposes)
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "✅ FCM Token: ${task.result}")
                } else {
                    Log.w(TAG, "⚠️ FCM token fetch failed: ${task.exception?.message}")
                }
            }

            // 4. Launch MainActivity to complete setup
            Log.i(TAG, "🚀 Launching MainActivity...")
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("provisioned_via_qr", true)
            }
            context.startActivity(launchIntent)

            Toast.makeText(
                context,
                "✅ EMI Locker provisioned successfully!",
                Toast.LENGTH_LONG
            ).show()

            Log.i(TAG, "🎉 Post-provisioning setup completed successfully!")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during post-provisioning setup: ${e.message}", e)
            Toast.makeText(
                context,
                "Provisioning error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
