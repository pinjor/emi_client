package com.example.emilockerclient.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Button
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

    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_IDLE -> {
                    // Call ended - wait 10 seconds then deactivate dialer mode
                    android.util.Log.i(TAG, "📞 Call ended, deactivating dialer mode in 10 seconds...")
                    handler.postDelayed({
                        android.util.Log.i(TAG, "⏰ 10 seconds passed, setting dialerActive = false")
                        PrefsHelper.setDialerActive(this@LockScreenActivity, false)
                        // Lock screen will be relaunched by LockMonitorService automatically
                    }, 10000) // 10 seconds grace period after call ends
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    // Call is active (user picked up or made a call)
                    android.util.Log.i(TAG, "📞 Call is ACTIVE (OFFHOOK)")
                    PrefsHelper.setDialerActive(this@LockScreenActivity, true)
                }
                TelephonyManager.CALL_STATE_RINGING -> {
                    // Incoming call ringing
                    android.util.Log.i(TAG, "📞 Call is RINGING")
                    PrefsHelper.setDialerActive(this@LockScreenActivity, true)
                }
            }
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

        // Monitor phone state for dialer bypass prevention
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        try {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to register phone state listener: ${e.message}")
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

        // TODO: Get seller info from backend/preferences (hardcoded for now)
        findViewById<TextView>(R.id.tvSellerName).text = "EMI Mobile Store"
        findViewById<TextView>(R.id.tvSellerPhone).text = "+8801600457087"

        // Call Seller button
        findViewById<Button>(R.id.btnCallSeller).setOnClickListener {
            val phoneNumber = "+8801600457087" // TODO: Get from backend
            openDialer(phoneNumber)
        }

        // Emergency call button (999)
        findViewById<Button>(R.id.btnEmergency).setOnClickListener {
            openDialer("999")
        }
    }

    private fun openDialer(phoneNumber: String) {
        try {
            android.util.Log.i(TAG, "Opening dialer for: $phoneNumber")

            // Set flag BEFORE opening dialer - CRITICAL!
            PrefsHelper.setDialerActive(this, true)

            // Schedule a fallback check in case user dismisses dialer without calling
            // This handles the edge case where user opens dialer but doesn't make a call
            handler.postDelayed({
                // Check if user is back in lock screen but no call was made
                if (PrefsHelper.isDialerActive(this)) {
                    android.util.Log.w(TAG, "⚠️ Dialer was opened but no call detected - resetting dialer state")
                    PrefsHelper.setDialerActive(this, false)
                }
            }, 30000) // 30 seconds timeout - if no call in 30s, assume user dismissed dialer

            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(dialIntent)

            android.util.Log.i(TAG, "✅ Dialer opened successfully, 30s timeout started")

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to open dialer: ${e.message}")
            PrefsHelper.setDialerActive(this, false)
        }
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.i(TAG, "onResume() - dialerActive=${PrefsHelper.isDialerActive(this)}")

        // CRITICAL FIX: If user returns to lock screen without making a call, reset dialer state
        // This handles the edge case where user opens dialer → dismisses it → tries other apps
        if (PrefsHelper.isDialerActive(this)) {
            android.util.Log.i(TAG, "⚠️ User returned to lock screen while dialerActive=true")
            android.util.Log.i(TAG, "   Checking if a call was actually made...")

            // Give a grace period (3 seconds) for PhoneStateListener to update
            handler.postDelayed({
                // If still dialerActive after 3 seconds, means no call was made
                if (PrefsHelper.isDialerActive(this@LockScreenActivity)) {
                    android.util.Log.w(TAG, "🚫 No call detected - user likely dismissed dialer")
                    android.util.Log.i(TAG, "   Resetting dialerActive to false")
                    PrefsHelper.setDialerActive(this@LockScreenActivity, false)
                } else {
                    android.util.Log.i(TAG, "✅ Call was made, dialerActive properly managed by PhoneStateListener")
                }
            }, 3000) // 3 second grace period
        }
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.i(TAG, "onPause() - locked=${PrefsHelper.isLocked(this)}, dialerActive=${PrefsHelper.isDialerActive(this)}")

        // CRITICAL FIX: Don't relaunch if dialer is active!
        if (PrefsHelper.isDialerActive(this)) {
            android.util.Log.i(TAG, "✅ Dialer is active - NOT scheduling relaunch")
            return
        }

        // Only relaunch if device is locked AND dialer is NOT active
        if (PrefsHelper.isLocked(this)) {
            android.util.Log.i(TAG, "Device locked and dialer NOT active - scheduling relaunch in 2s")
            handler.postDelayed({
                // Double-check dialer is still not active before relaunching
                if (!PrefsHelper.isDialerActive(this@LockScreenActivity)) {
                    android.util.Log.i(TAG, "Executing delayed relaunch")
                    bringToFront()
                } else {
                    android.util.Log.i(TAG, "Cancelled relaunch - dialer became active")
                }
            }, 2000) // 2 seconds delay for normal navigation blocking
        }
    }

    override fun onStop() {
        super.onStop()
        android.util.Log.i(TAG, "onStop()")

        // Don't do anything if user is in dialer
        if (PrefsHelper.isDialerActive(this)) {
            android.util.Log.i(TAG, "User in dialer - allowing activity to stop")
        }
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

        try {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to unregister phone listener: ${e.message}")
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
