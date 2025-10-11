package com.example.emilockerclient.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.emilockerclient.R
import com.example.emilockerclient.utils.PrefsHelper

class LockScreenActivity : AppCompatActivity() {

    private val UNLOCK_ACTION = "com.example.emilockerclient.ACTION_UNLOCK"

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish() // Close the lock screen when unlock action is received
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show activity above lock screen and turn screen on
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
                // Intentionally not dismissing keyguard to block normal unlock
            } else {
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }

            // Keep screen on while this activity is displayed
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (_: Exception) {
            // Ignore exceptions from window flag manipulations
        }

        setContentView(R.layout.activity_lock_screen)

        // Display lock message from Intent extra or saved preferences
        val message = intent.getStringExtra("LOCK_MESSAGE") ?: PrefsHelper.getLockMessage(this)
        findViewById<TextView>(R.id.tvLockMessage).text = message

        // Emergency call button dials "999"
        findViewById<Button>(R.id.btnEmergency).setOnClickListener {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:999"))
            startActivity(dialIntent)
        }

        // Register BroadcastReceiver for unlock events; local, non-exported receiver
        registerReceiver(unlockReceiver, IntentFilter(UNLOCK_ACTION), RECEIVER_NOT_EXPORTED)

        // Disable system back button while lock screen is shown
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Back press is disabled here
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(unlockReceiver)
        } catch (_: Exception) {
            // Safe to ignore if already unregistered or failed
        }
    }
}
