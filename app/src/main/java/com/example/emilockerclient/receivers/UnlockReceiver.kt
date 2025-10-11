package com.example.emilockerclient.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.emilockerclient.utils.PrefsHelper

class UnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("UnlockReceiver", "Received ACTION_UNLOCK → clearing lock state")
        if (context != null) {
            PrefsHelper.setLocked(context, false)
            PrefsHelper.setLockMessage(context, "")
        }
    }
}
