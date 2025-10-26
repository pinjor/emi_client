package com.example.emilockerclient.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.emilockerclient.R
import com.example.emilockerclient.utils.PrefsHelper

class LockScreenActivity : AppCompatActivity() {

    private val UNLOCK_ACTION = "com.example.emilockerclient.ACTION_UNLOCK"
    private val TAG = "LockScreenActivity"

    private val handler = Handler(Looper.getMainLooper())

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            android.util.Log.i(TAG, "Unlock broadcast received, finishing activity")
            finish()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make this activity show above lock screen and turn screen on
        setupWindowFlags()

        setContentView(R.layout.activity_lock_screen)

        // Setup UI elements
        setupUI()

        // Register unlock receiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(unlockReceiver, IntentFilter(UNLOCK_ACTION), RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(unlockReceiver, IntentFilter(UNLOCK_ACTION))
        }


        // Disable back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                android.util.Log.i(TAG, "Back button pressed - blocked")
                // Do nothing - back is disabled
            }
        })
    }

    private fun setupWindowFlags() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }

            // Keep screen on while this activity is displayed
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // Prevent screenshots (optional security)
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to set window flags: ${e.message}")
        }
    }

    private fun setupUI() {
        // Display lock title from Intent or SharedPreferences
        val title = intent.getStringExtra("LOCK_TITLE") ?: PrefsHelper.getLockTitle(this)
        findViewById<TextView>(R.id.tvLockTitle).text = title

        // Display lock message from Intent or SharedPreferences
        val message = intent.getStringExtra("LOCK_MESSAGE") ?: PrefsHelper.getLockMessage(this)
        findViewById<TextView>(R.id.tvLockMessage).text = message

        // Display seller information (read-only, no buttons)
        // TODO: Get seller info from backend/preferences (hardcoded for now)
        findViewById<TextView>(R.id.tvSellerName).text = "EMI Mobile Store"
        findViewById<TextView>(R.id.tvSellerPhone).text = "+8801600457087"
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.i(TAG, "onResume()")
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.i(TAG, "onPause() - locked=${PrefsHelper.isLocked(this)}")

        // Relaunch if device is still locked
        if (PrefsHelper.isLocked(this)) {
            android.util.Log.i(TAG, "Device locked - scheduling relaunch in 2s")
            handler.postDelayed({
                android.util.Log.i(TAG, "Executing delayed relaunch")
                bringToFront()
            }, 2000) // 2 seconds delay
        }
    }

    override fun onStop() {
        super.onStop()
        android.util.Log.i(TAG, "onStop()")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        android.util.Log.i(TAG, "onNewIntent()")

        // Update message if provided
        intent.getStringExtra("LOCK_MESSAGE")?.let { message ->
            findViewById<TextView>(R.id.tvLockMessage).text = message
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Block Home button (doesn't work on modern Android, but try anyway)
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            android.util.Log.i(TAG, "Home button pressed - blocked")
            return true
        }

        // Block Recent Apps button
        if (keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            android.util.Log.i(TAG, "Recent apps button pressed - blocked")
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.i(TAG, "onDestroy()")

        try {
            unregisterReceiver(unlockReceiver)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to unregister receiver: ${e.message}")
        }


        handler.removeCallbacksAndMessages(null)
    }

    private fun bringToFront() {
        // Check if still locked
        if (PrefsHelper.isLocked(this)) {
            val intent = Intent(this, LockScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("LOCK_MESSAGE", PrefsHelper.getLockMessage(this@LockScreenActivity))
            }
            startActivity(intent)
            android.util.Log.i(TAG, "Lock screen brought back to front")
        }
    }
}
