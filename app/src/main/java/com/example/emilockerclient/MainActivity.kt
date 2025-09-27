package com.example.emilockerclient

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.emilockerclient.admin.EmiAdminReceiver
import com.example.emilockerclient.managers.DeviceControlManager
import com.example.emilockerclient.workers.HeartbeatWorker
import com.example.emilockerclient.workers.OfflineCheckWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var compName: ComponentName
    private lateinit var deviceManager: DeviceControlManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, EmiAdminReceiver::class.java)
        deviceManager = DeviceControlManager(this)

        // 🔹 Periodic heartbeat every 15 min
        val heartbeatRequest =
            PeriodicWorkRequestBuilder<HeartbeatWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "heartbeatWork",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            heartbeatRequest
        )

        // 🔹 Offline check every 1 hour
        val offlineCheckRequest =
            PeriodicWorkRequestBuilder<OfflineCheckWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "offlineCheckWork",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            offlineCheckRequest
        )



        // 🔹 One immediate heartbeat on app start
        val oneTimeHeartbeat = OneTimeWorkRequestBuilder<HeartbeatWorker>().build()
        WorkManager.getInstance(this).enqueue(oneTimeHeartbeat)

        // Button: Enable Admin
        findViewById<Button>(R.id.btnEnableAdmin).setOnClickListener {
            if (deviceManager.isAdminActive()) {
                Toast.makeText(this, "Already admin!", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required for EMI lock enforcement")
                }
                startActivity(intent)
            }
        }

        // Button: Lock (via DPM only)
        findViewById<Button>(R.id.btnLock).setOnClickListener {
            if (deviceManager.isAdminActive()) {
                deviceManager.lockDevice()
            } else {
                Toast.makeText(this, "Admin not active", Toast.LENGTH_SHORT).show()
            }
        }

        // Button: Apply Restrictions (now functional)
        findViewById<Button>(R.id.btnRestrict).setOnClickListener {
            if (deviceManager.isDeviceOwner()) {
                deviceManager.applyRestrictions()
                Toast.makeText(this, "Restrictions applied", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Restrictions require Device Owner mode", Toast.LENGTH_SHORT).show()
            }
        }

        // Button: Clear Restrictions (now functional)
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            if (deviceManager.isDeviceOwner()) {
                deviceManager.clearRestrictions()
                Toast.makeText(this, "Restrictions cleared", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Restrictions require Device Owner mode", Toast.LENGTH_SHORT).show()
            }
        }

        // Button: Lock Google Account (FRP ON)
        findViewById<Button>(R.id.btnFrpLock).setOnClickListener {
            if (deviceManager.isDeviceOwner()) {
                deviceManager.enforceFrpProtection(true)
                Toast.makeText(this, "FRP Lock applied", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Device Owner required", Toast.LENGTH_SHORT).show()
            }
        }

// Button: Unlock Google Account (FRP OFF)
        findViewById<Button>(R.id.btnFrpUnlock).setOnClickListener {
            if (deviceManager.isDeviceOwner()) {
                deviceManager.enforceFrpProtection(false)
                Toast.makeText(this, "FRP Lock removed (account changes allowed)", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Device Owner required", Toast.LENGTH_SHORT).show()
            }
        }




        // 🔹 NEW: Show LockScreen (test Phase 4)
        findViewById<Button>(R.id.btnShowLockScreen).setOnClickListener {
            deviceManager.showLockScreen("⚠️ EMI overdue! Please contact seller.")
        }

        // 🔹 NEW: Clear LockScreen (test unlock)
        findViewById<Button>(R.id.btnClearLockScreen).setOnClickListener {
            deviceManager.clearLock()
        }
    }
}
