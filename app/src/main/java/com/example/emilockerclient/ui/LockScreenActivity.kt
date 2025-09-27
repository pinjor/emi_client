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
            finish() // close lock screen on unlock
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Modern API to show above lock screen and turn screen on
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
                // requestDismissKeyguard is not called because we intentionally want to block normal keyguard
            } else {
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }

            // Keep display and block user from sleeping the app
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        } catch (_: Exception) {
            // ignore
        }

        setContentView(R.layout.activity_lock_screen)

        // Read lock message
        val message = intent.getStringExtra("LOCK_MESSAGE") ?: PrefsHelper.getLockMessage(this)
        findViewById<TextView>(R.id.tvLockMessage).text = message

        // Emergency dial button
        findViewById<Button>(R.id.btnEmergency).setOnClickListener {
            val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:999"))
            startActivity(dial)
        }

        // Register unlock receiver (explicitly exported false in manifest; use local receiver)
        registerReceiver(unlockReceiver, IntentFilter(UNLOCK_ACTION), RECEIVER_NOT_EXPORTED)

        // Disable system back by consuming it
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // blocked
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(unlockReceiver)
        } catch (_: Exception) {
        }
    }
}
