package com.example.emilockerclient

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.emilockerclient.admin.EmiAdminReceiver
import android.app.admin.DevicePolicyManager
import android.app.admin.FactoryResetProtectionPolicy

class ProvisioningTestActivity : AppCompatActivity() {

    private val TAG = "ProvisioningTestActivity"
    private lateinit var dpm: DevicePolicyManager
    private lateinit var compName: ComponentName

    // UI references
    private lateinit var tvStatus: TextView
    private lateinit var etAccounts: EditText
    private lateinit var btnApply: Button
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provisioning_test)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, EmiAdminReceiver::class.java)

        tvStatus = findViewById(R.id.tv_status)
        etAccounts = findViewById(R.id.et_accounts)
        btnApply = findViewById(R.id.btn_apply_frp)
        btnBack = findViewById(R.id.btn_back)

        updateStatus()

        // If caller passed an EMAILS extra, prefill and auto-apply (optional)
        val extraAccounts = intent?.getStringExtra("EXTRA_FRP_ACCOUNTS")?.trim()
        if (!extraAccounts.isNullOrEmpty()) {
            etAccounts.setText(extraAccounts)
            // Do NOT auto-apply without a short delay so UI shows; do it on button click below if you prefer auto.
        }

        btnApply.setOnClickListener {
            val raw = etAccounts.text.toString().trim()
            if (raw.isEmpty()) {
                Toast.makeText(this, "Enter one or more account identifiers (comma separated).", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val list = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val ok = applyFrpAccounts(list)
            if (ok) {
                updateStatus()
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun updateStatus() {
        val owner = dpm.isDeviceOwnerApp(packageName)
        val admin = dpm.isAdminActive(compName)
        tvStatus.text = "DeviceOwner: $owner\nAdminActive: $admin"
    }

    /**
     * Apply enterprise FRP with the provided list of account identifiers.
     * For dev testing, passing email strings usually works (e.g. "test@gmail.com").
     *
     * NOTE: device must be Device Owner and the accounts must already exist on the device (added in Settings)
     */
    private fun applyFrpAccounts(accounts: List<String>): Boolean {
        if (!dpm.isDeviceOwnerApp(packageName)) {
            Log.w(TAG, "Cannot apply FRP: app is not device owner.")
            Toast.makeText(this, "App is not device owner. Use adb dpm set-device-owner for testing.", Toast.LENGTH_LONG).show()
            return false
        }

        if (accounts.isEmpty()) {
            Toast.makeText(this, "No accounts provided.", Toast.LENGTH_SHORT).show()
            return false
        }

        return try {
            val policy = FactoryResetProtectionPolicy.Builder()
                .setFactoryResetProtectionEnabled(true)
                .setFactoryResetProtectionAccounts(accounts)
                .build()

            dpm.setFactoryResetProtectionPolicy(compName, policy)

            Log.i(TAG, "FRP applied with accounts: $accounts")
            Toast.makeText(this, "FRP applied for: ${accounts.joinToString(",")}", Toast.LENGTH_LONG).show()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply FRP: ${e.message}", e)
            Toast.makeText(this, "Failed to apply FRP: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }

    companion object {
        /**
         * Helper to start this activity and pre-fill accounts from code.
         * Example: ProvisioningTestActivity.startWithAccounts(context, "test@gmail.com")
         */
        fun startWithAccounts(context: Context, commaSeparatedAccounts: String) {
            val i = Intent(context, ProvisioningTestActivity::class.java)
            i.putExtra("EXTRA_FRP_ACCOUNTS", commaSeparatedAccounts)
            context.startActivity(i)
        }
    }
}
