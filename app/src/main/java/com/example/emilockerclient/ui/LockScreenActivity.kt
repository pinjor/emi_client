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
                    // Call ended, return to lock screen after 3 seconds
                    if (isInDialer) {
                        android.util.Log.i(TAG, "Call ended, returning to lock screen in 3s")
                        handler.postDelayed({
                            bringToFront()
                        }, 3000)
                    }
                }
                TelephonyManager.CALL_STATE_OFFHOOK, TelephonyManager.CALL_STATE_RINGING -> {
                    isInDialer = true
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
        // Display lock message from Intent or SharedPreferences
        val message = intent.getStringExtra("LOCK_MESSAGE") ?: PrefsHelper.getLockMessage(this)
        findViewById<TextView>(R.id.tvLockMessage).text = message

        // TODO: Get seller info from backend/preferences (hardcoded for now)
        findViewById<TextView>(R.id.tvSellerName).text = "EMI Mobile Store"
        findViewById<TextView>(R.id.tvSellerPhone).text = "+880 1712-345678"

        // Call Seller button
        findViewById<Button>(R.id.btnCallSeller).setOnClickListener {
            val phoneNumber = "+8801712345678" // TODO: Get from backend
            makeCall(phoneNumber)
        }

        // WhatsApp button
        findViewById<Button>(R.id.btnWhatsApp).setOnClickListener {
            val phoneNumber = "+8801712345678" // TODO: Get from backend
            openWhatsApp(phoneNumber)
        }

        // Emergency call button (999)
        findViewById<Button>(R.id.btnEmergency).setOnClickListener {
            makeCall("999")
        }
    }

    private fun makeCall(phoneNumber: String) {
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            dialIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            isInDialer = true
            startActivity(dialIntent)

            // Schedule to bring lock screen back after 2 seconds
            handler.postDelayed({
                bringToFront()
            }, 2000)

            android.util.Log.i(TAG, "Dialer opened for: $phoneNumber")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to open dialer: ${e.message}")
        }
    }

    private fun openWhatsApp(phoneNumber: String) {
        try {
            val cleanNumber = phoneNumber.replace("+", "").replace(" ", "")
            val whatsappIntent = Intent(Intent.ACTION_VIEW)
            whatsappIntent.data = Uri.parse("https://wa.me/$cleanNumber")
            whatsappIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            isInDialer = true
            startActivity(whatsappIntent)

            // Schedule to bring lock screen back
            handler.postDelayed({
                bringToFront()
            }, 2000)

            android.util.Log.i(TAG, "WhatsApp opened for: $phoneNumber")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to open WhatsApp: ${e.message}")
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
        android.util.Log.i(TAG, "onResume()")
        isInDialer = false
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.i(TAG, "onPause()")

        // If user navigated away and device is still locked, relaunch immediately
        if (PrefsHelper.isLocked(this) && !isInDialer) {
            handler.postDelayed({
                bringToFront()
            }, 500)
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

        try {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to unregister phone listener: ${e.message}")
        }

        handler.removeCallbacksAndMessages(null)
    }
}
