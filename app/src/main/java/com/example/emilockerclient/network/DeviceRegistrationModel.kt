package com.example.emilockerclient.network


// The innermost device details
data class DeviceDetails(
    val serial_number: String,
    val imei_1: String, // Note the underscore to match the server response
    val imei_2: String?,
    val fcm_token: String,
    val registered: Boolean
)

// The device status block
data class DeviceStatus(
    val is_locked: Boolean,
    val is_camera_disabled: Boolean,
    val is_bluetooth_disabled: Boolean,
    val is_app_hidden: Boolean,
    val has_password: Boolean,
    val last_command_sent_at: String?
)

// The product block
data class ProductInfo(
    val type: String,
    val model: String,
    val price: String
)

// The nested 'device' object within 'data'
data class RegisteredDevice(
    val customer_id: Int,
    val customer_name: String,
    val nid_no: String,
    val mobile: String,
    val device: DeviceDetails,
    val device_status: DeviceStatus,
    val product: ProductInfo,
    val status: String,
    val can_receive_commands: Boolean
)

// The final structure of the 'data' field
data class RegistrationData(
    val device: RegisteredDevice
)

// 💡 NEW ROOT RESPONSE MODEL: Use the specific RegistrationData type
// Replace the old ApiResponse if this is the ONLY response type for this endpoint
data class DeviceRegistrationResponse(
    val success: Boolean,
    val message: String? = null,
    val data: RegistrationData? = null // Now contains the fully structured data
)

// Keep the old, generic ApiResponse for other endpoints if necessary
data class ApiResponse(
    val success: Boolean,
    val message: String? = null,
    val data: Any? = null
)