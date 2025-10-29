# 🔧 Troubleshooting: Reset Loop After QR Provisioning

## Problem
After factory reset and scanning QR code, device shows "Can't set up device" error with a Reset button in a continuous loop.

## Root Causes

### 1. **Signature Checksum Mismatch** (Most Common)
The APK signature checksum in your QR code doesn't match the actual APK signature.

**Fix:**
```bash
# 1. Build a signed release APK
cd /path/to/emi_locker_client_app
./gradlew clean assembleRelease

# 2. Generate the correct signature checksum
python3 generate_checksum.py app/build/outputs/apk/release/app-release.apk

# Output will be something like:
# ✅ Final Provisioning Checksum (use this in your QR JSON):
#    E0:29:08:BD:F6:FA:CA:D9:C5:E0:D9:9D:8B:4F:61:5C:7D:39:95:C4:DE:24:9C:84:8E:EA:B1:21:2D:48:B7:FE

# 3. Update your QR code JSON with the new checksum
# 4. Upload APK to your server
# 5. Generate new QR codes
```

### 2. **APK Not Accessible or Downloading Fails**
The device can't download the APK from the URL in your QR code.

**Fix:**
- Verify the APK URL is accessible: `https://www.imelocker.com/apk/emi-locker-client.apk`
- Check server logs for download attempts
- Ensure APK file exists at that location
- Test download with curl or browser

### 3. **Missing DeviceProvisioningActivity in Manifest**
(Already fixed in recent updates)

### 4. **MainActivity Crash After Provisioning**
Something in MainActivity causes a crash immediately after provisioning completes.

**To Debug:**
```bash
# Connect device via ADB and watch logs during provisioning
adb logcat -s EmiAdminReceiver MainActivity DeviceProvisioning
```

## Step-by-Step Fix

### Step 1: Build and Upload Correct APK

```bash
cd W:\D_folder\EMIII\emi_locker_client_app

# Build release APK
gradlew.bat clean assembleRelease

# Find the APK
# Look in: app\build\outputs\apk\release\app-release.apk
```

### Step 2: Get the Correct Signature Checksum

```bash
# Use the Python script to generate checksum
python generate_checksum.py app\build\outputs\apk\release\app-release.apk

# This will output a colon-separated hex string
# Example: E0:29:08:BD:F6:FA:CA:D9:C5:E0:D9:9D:8B:4F:61:5C:7D:39:95:C4:DE:24:9C:84:8E:EA:B1:21:2D:48:B7:FE
```

### Step 3: Update QR Code

Update your QR code JSON with the correct checksum:

```json
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.example.emilockerclient/.admin.EmiAdminReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://www.imelocker.com/apk/emi-locker-client.apk",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM": "REPLACE_WITH_YOUR_CHECKSUM_HERE",
  "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": false,
  "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
  "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
    "device_id": "52",
    "seller_id": "191"
  }
}
```

### Step 4: Upload APK to Server

```bash
# Upload the APK to your server
scp app/build/outputs/apk/release/app-release.apk user@your-server:/path/to/www/imelocker.com/apk/emi-locker-client.apk

# OR use FTP or other method
# The URL should be accessible: https://www.imelocker.com/apk/emi-locker-client.apk
```

### Step 5: Test Provisioning

1. Factory reset the test device
2. On "Hi there" screen, tap 6-7 times
3. QR scanner appears
4. Scan your updated QR code
5. Device should download and install the APK
6. Provisioning should complete successfully ✅

## Debugging Commands

### Check if Device Owner is Set
```bash
adb shell dpm list-owners
# Should show: com.example.emilockerclient (Device Owner)
```

### Watch Real-Time Logs During Provisioning
```bash
adb logcat -s EmiAdminReceiver DeviceProvisioning MainActivity
```

### Check SharedPreferences After Provisioning
```bash
adb shell run-as com.example.emilockerclient cat /data/data/com.example.emilockerclient/shared_prefs/emi_prefs.xml
```

## Expected Log Flow

When provisioning succeeds, you should see:

```
✅ Provisioning activity started
📦 Admin extras bundle received with 2 items
   Key: device_id = 52
   Key: seller_id = 191
✅ Provisioning activity completed, returning RESULT_OK
🎉 onProfileProvisioningComplete() called - Provisioning successful!
✅ Confirmed: App is Device Owner
📝 Saving provisioning data
🔐 Auto-granting permissions...
✅ Permissions granted
🔒 Adding device restrictions...
✅ Factory reset disabled
🔥 Initializing Firebase...
🚀 Launching MainActivity...
🎉 Post-provisioning setup completed successfully!
```

## Common Error Patterns

### Error: "Package signature mismatch"
**Cause:** Checksum doesn't match APK signature
**Fix:** Regenerate checksum and update QR code

### Error: "Failed to download package"
**Cause:** APK URL is not accessible
**Fix:** Check server, verify file exists, test URL with curl

### Error: "Can't set up device" → Reset loop
**Cause:** DeviceProvisioningActivity not declared in manifest (fixed)
**Fix:** Already addressed in recent code updates

### Error: Activity crash after provisioning
**Cause:** MainActivity or other component fails to initialize
**Fix:** Check logcat for stack traces, handle exceptions gracefully

## Testing Without Factory Reset

```bash
# Install APK
adb install -r app/build/outputs/apk/release/app-release.apk

# Set as Device Owner manually
adb shell dpm set-device-owner com.example.emilockerclient/.admin.EmiAdminReceiver

# Verify
adb shell dpm list-owners
```

## Notes

- The signature checksum MUST match the APK signature exactly
- If you rebuild the APK, you MUST regenerate the checksum and update all QR codes
- The APK must be signed (not an unsigned debug build)
- Ensure the APK download URL is publicly accessible
- Device must have internet connectivity during provisioning
