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
     * 1. Extract and save custom data from QR code (device_id, seller_id)
     * 2. Auto-grant all required permissions
     * 3. Initialize Firebase for FCM
     * 4. Launch MainActivity to complete setup
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
            // 1. Extract and save custom data from QR code's admin extras bundle
            val adminExtras = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(
                    DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE,
                    android.os.PersistableBundle::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE)
            }

            val prefs = context.getSharedPreferences("emi_prefs", Context.MODE_PRIVATE)
            
            if (adminExtras != null) {
                Log.i(TAG, "📦 Admin extras bundle received")

                val deviceId = adminExtras.getString("device_id", "")
                val sellerId = adminExtras.getString("seller_id", "")
                val serverUrl = adminExtras.getString("server_url", "")

                Log.i(TAG, "📝 Saving provisioning data:")
                Log.i(TAG, "   Device ID: $deviceId")
                Log.i(TAG, "   Seller ID: $sellerId")
                Log.i(TAG, "   Server URL: $serverUrl")

                prefs.edit().apply {
                    putString("provisioned_device_id", deviceId)
                    putString("provisioned_seller_id", sellerId)
                    putString("provisioned_server_url", serverUrl)
                    putBoolean("is_provisioned", true)
                    apply()
                }
            } else {
                Log.w(TAG, "⚠️ No admin extras bundle found in intent")
                // Still mark as provisioned since we're now Device Owner
                // This handles edge cases where bundle might not be accessible
                prefs.edit().apply {
                    putBoolean("is_provisioned", true)
                    apply()
                }
                Log.i(TAG, "✅ Marked as provisioned without admin extras")
            }

            // 2. Auto-grant all required permissions
            Log.i(TAG, "🔐 Auto-granting permissions...")
            val permissionManager = PermissionManager(context, compName)
            permissionManager.ensurePermissions()
            Log.i(TAG, "✅ Permissions granted")

            // 3. Add core device restrictions
            Log.i(TAG, "🔒 Adding device restrictions...")
            dpm.addUserRestriction(compName, UserManager.DISALLOW_FACTORY_RESET)
            Log.i(TAG, "✅ Factory reset disabled")

            // 4. Initialize Firebase
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

            // 5. Launch MainActivity to complete setup
            Log.i(TAG, "🚀 Launching MainActivity...")
            try {
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
                Log.e(TAG, "❌ Failed to launch MainActivity: ${e.message}", e)
                // Still mark as provisioned even if MainActivity fails
                Log.i(TAG, "✅ Provisioning succeeded but activity launch failed")
            }

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
