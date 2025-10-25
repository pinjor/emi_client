package com.example.emilockerclient

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.emilockerclient.network.DeviceRegisterRequest
import com.example.emilockerclient.network.DeviceRegistrationResponse
import com.example.emilockerclient.network.RetrofitClient
import com.example.emilockerclient.utils.SetupPrefsHelper
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Activity for registering device with IMEI in Admin Mode
 */
class RegistrationActivity : AppCompatActivity() {

    private val TAG = "RegistrationActivity"

    private lateinit var tilImei: TextInputLayout
    private lateinit var etImei: TextInputEditText
    private lateinit var btnRegister: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatusMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        // Initialize views
        tilImei = findViewById(R.id.tilImei)
        etImei = findViewById(R.id.etImei)
        btnRegister = findViewById(R.id.btnRegister)
        progressBar = findViewById(R.id.progressBar)
        tvStatusMessage = findViewById(R.id.tvStatusMessage)

        btnRegister.setOnClickListener {
            val imei = etImei.text.toString().trim()

            if (validateImei(imei)) {
                registerDevice(imei)
            }
        }
    }

    private fun validateImei(imei: String): Boolean {
        return when {
            imei.isEmpty() -> {
                tilImei.error = "IMEI is required"
                false
            }
            imei.length != 15 -> {
                tilImei.error = "IMEI must be 15 digits"
                false
            }
            !imei.all { it.isDigit() } -> {
                tilImei.error = "IMEI must contain only numbers"
                false
            }
            else -> {
                tilImei.error = null
                true
            }
        }
    }

    private fun registerDevice(imei: String) {
        // Show loading
        showLoading(true)
        btnRegister.isEnabled = false
        tvStatusMessage.text = "Connecting to server..."
        tvStatusMessage.visibility = View.VISIBLE

        // Fetch FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e(TAG, "Failed to get FCM token", task.exception)
                showError("Failed to get FCM token. Please check your internet connection.")
                return@addOnCompleteListener
            }

            val fcmToken = task.result
            Log.i(TAG, "FCM Token obtained: $fcmToken")

            // Save token locally
            SetupPrefsHelper.setRegisteredFcmToken(this, fcmToken)

            // Send registration request to backend
            sendRegistrationRequest(imei, fcmToken)
        }
    }

    private fun sendRegistrationRequest(imei: String, fcmToken: String) {
        tvStatusMessage.text = "Registering device..."

        // For Admin Mode, we use IMEI as both serial_number and imei1
        // Backend will recognize this device by IMEI
        val request = DeviceRegisterRequest(
            serial_number = imei, // Using IMEI as identifier
            imei1 = imei,
            fcm_token = fcmToken
        )

        Log.i(TAG, "Sending registration request for IMEI: $imei")

        RetrofitClient.api.registerDevice(request)
            .enqueue(object : Callback<DeviceRegistrationResponse> {
                override fun onResponse(
                    call: Call<DeviceRegistrationResponse>,
                    response: Response<DeviceRegistrationResponse>
                ) {
                    showLoading(false)

                    if (response.isSuccessful && response.body()?.success == true) {
                        val data = response.body()?.data
                        val customerName = data?.device?.customer_name

                        Log.i(TAG, "✅ Registration successful! Customer: $customerName")

                        // Save registration data
                        SetupPrefsHelper.setRegisteredImei(this@RegistrationActivity, imei)
                        SetupPrefsHelper.setRegistrationCompleted(this@RegistrationActivity, true)

                        // Show success message
                        tvStatusMessage.text = "✅ Registration successful!"
                        tvStatusMessage.setTextColor(getColor(android.R.color.holo_green_dark))

                        Toast.makeText(
                            this@RegistrationActivity,
                            "Device registered successfully!",
                            Toast.LENGTH_LONG
                        ).show()

                        // Navigate to Permission Setup
                        val intent = Intent(this@RegistrationActivity, com.example.emilockerclient.PermissionSetupActivity::class.java)
                        startActivity(intent)
                        finish()

                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "❌ Registration failed: HTTP ${response.code()} → $errorBody")

                        val errorMessage = response.body()?.message ?: "Registration failed. Please try again."
                        showError(errorMessage)
                    }
                }

                override fun onFailure(call: Call<DeviceRegistrationResponse>, t: Throwable) {
                    showLoading(false)
                    Log.e(TAG, "❌ Registration request failed", t)
                    showError("Connection failed: ${t.message}")
                }
            })
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        tvStatusMessage.text = message
        tvStatusMessage.setTextColor(getColor(android.R.color.holo_red_dark))
        tvStatusMessage.visibility = View.VISIBLE
        btnRegister.isEnabled = true
    }
}

