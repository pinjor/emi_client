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

    private var isInDialer = false
    private var dialerOpenTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val DIALER_GRACE_PERIOD = 10000000L // the time is in milliseconds (10 seconds)

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
                    // Call ended, return to lock screen after 10 seconds
                    android.util.Log.i(TAG, "Call ended, returning to lock screen in 10s")
                    handler.postDelayed({
                        isInDialer = false
                        dialerOpenTime = 0L
                        bringToFront()
                    }, 10000) // 10 seconds after call ends
                }
                TelephonyManager.CALL_STATE_OFFHOOK, TelephonyManager.CALL_STATE_RINGING -> {
                    // Call is active
                    android.util.Log.i(TAG, "Call is active, extending dialer time")
                    isInDialer = true
                    // Extend the grace period while call is active
                    dialerOpenTime = System.currentTimeMillis()
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
        findViewById<TextView>(R.id.tvSellerPhone).text = "+880 1712-345678"

        // Call Seller button
        findViewById<Button>(R.id.btnCallSeller).setOnClickListener {
            val phoneNumber = "+8801712345678" // TODO: Get from backend
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
            isInDialer = true
            dialerOpenTime = System.currentTimeMillis()

            // Cancel any pending relaunch attempts
            handler.removeCallbacksAndMessages(null)

            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(dialIntent)

            android.util.Log.i(TAG, "Dialer opened successfully, grace period starts now")

            // Schedule grace period check (2 minutes)
            handler.postDelayed({
                checkDialerGracePeriod()
            }, DIALER_GRACE_PERIOD)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to open dialer: ${e.message}")
            isInDialer = false
            dialerOpenTime = 0L
        }
    }

    private fun checkDialerGracePeriod() {
        // If grace period expired and no active call, bring lock screen back
        val timeSinceOpen = System.currentTimeMillis() - dialerOpenTime
        if (isInDialer && timeSinceOpen >= DIALER_GRACE_PERIOD) {
            android.util.Log.i(TAG, "Dialer grace period expired (2 min), returning to lock screen")
            isInDialer = false
            dialerOpenTime = 0L
            bringToFront()
        }
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

    override fun onResume() {
        super.onResume()
        android.util.Log.i(TAG, "onResume() - isInDialer=$isInDialer")

        // Only reset dialer flag if enough time has passed and user came back naturally
        if (isInDialer) {
            val timeSinceOpen = System.currentTimeMillis() - dialerOpenTime
            if (timeSinceOpen > 5000) { // 5 seconds - user came back naturally
                android.util.Log.i(TAG, "User returned from dialer naturally after ${timeSinceOpen}ms")
                isInDialer = false
                dialerOpenTime = 0L
            } else {
                android.util.Log.i(TAG, "Ignoring onResume - just opened dialer (${timeSinceOpen}ms ago)")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.i(TAG, "onPause() - isInDialer=$isInDialer, locked=${PrefsHelper.isLocked(this)}")

        // CRITICAL: Completely skip relaunch if user is in dialer
        if (isInDialer) {
            android.util.Log.i(TAG, "User is in dialer - NOT relaunching lock screen")
            return
        }

        // Only relaunch if device is locked AND user is NOT in dialer
        if (PrefsHelper.isLocked(this)) {
            android.util.Log.i(TAG, "Device locked and user NOT in dialer - scheduling relaunch in 2s")
            handler.postDelayed({
                if (!isInDialer) {
                    android.util.Log.i(TAG, "Executing delayed relaunch")
                    bringToFront()
                } else {
                    android.util.Log.i(TAG, "Cancelled relaunch - user now in dialer")
                }
            }, 2000) // 2 seconds delay for normal navigation blocking
        }
    }

    override fun onStop() {
        super.onStop()
        android.util.Log.i(TAG, "onStop() - isInDialer=$isInDialer")

        // Don't do anything if user is in dialer
        if (isInDialer) {
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
}
