package com.example.emilockerclient.services

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility Service for monitoring app usage and preventing uninstallation
 * This is used in Admin Mode (non-device owner) to provide additional control
 */
class EmiAccessibilityService : AccessibilityService() {

    private val TAG = "EmiAccessibilityService"

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "✅ Accessibility Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Monitor accessibility events if needed
        // Can be used to detect app uninstall attempts, settings changes, etc.
    }

    override fun onInterrupt() {
        Log.w(TAG, "⚠️ Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "❌ Accessibility Service destroyed")
    }
}

