package com.example.emilockerclient

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.emilockerclient.utils.SetupPrefsHelper

/**
 * Welcome screen for Admin Mode setup
 * Shown only when device is not Device Owner and setup hasn't been completed
 */
class SetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        // Mark that we're in Admin Mode
        SetupPrefsHelper.setDeviceMode(this, SetupPrefsHelper.MODE_ADMIN)

        val btnContinue = findViewById<Button>(R.id.btnContinue)
        btnContinue.setOnClickListener {
            // Navigate to Registration Activity
            val intent = Intent(this, com.example.emilockerclient.RegistrationActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

