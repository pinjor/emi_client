# Build and Deploy Guide - EMI Locker QR Provisioning

## 🎯 Fixed Issues

✅ **Removed DeviceProvisioningActivity** - Not needed for DPC apps
✅ **Fixed download URL** - Changed to `https://www.imelocker.com/apk/emi-locker-client.apk`
✅ **Removed android:testOnly** - Won't block provisioning
✅ **Updated EmiAdminReceiver** - Properly handles admin extras bundle
✅ **Fixed MainActivity** - Uses correct SharedPreferences keys

## 📋 Step-by-Step Build Process

### Step 1: Build Signed APK

#### Option A: Using Android Studio (Recommended)

1. **Open Android Studio**
2. **Build → Generate Signed Bundle / APK**
3. **Select APK → Next**
4. **Choose your keystore:**
   - If you have existing keystore:
     - Key store path: `~/.android/keystores/emi-release.jks`
     - Key store password: `emilockerclient2024`
     - Key alias: `emi_release`
     - Key password: `emilockerclient2024`
   - If creating new keystore:
     - Create new keystore with above details
     - **IMPORTANT: Backup this keystore file!**
5. **Select release build variant**
6. **Finish**
7. **APK will be at:** `app/build/outputs/apk/release/app-release.apk`

#### Option B: Using Command Line

```bash
# If keystore doesn't exist, create it
./create_release_keystore.sh

# Build release APK
./gradlew clean assembleRelease
```

### Step 2: Generate Checksum and QR JSON

```bash
# Run the checksum script
python3 get_apk_checksum.py

# Output will show:
# ✅ Certificate SHA-256 (formatted for Android)
# ✅ QR JSON saved to: qr_provisioning.json
```

**Example output:**
```
📱 APK found: app/build/outputs/apk/release/app-release.apk
📏 File size: 15.89 MB

🔐 Extracting signing certificate SHA-256...

✅ Certificate SHA-256 (formatted for Android):
   6C:A7:1C:F9:7B:57:2D:6D:01:65:66:3E:0F:F2:8F:96:47:8C:48:32:B2:5C:C4:A7:F3:FE:0C:59:C9:F7:48:7E

✅ QR JSON saved to: qr_provisioning.json
```

### Step 3: Upload APK to Server

**CRITICAL: Use the EXACT URL specified in QR JSON**

```bash
# Upload to server
# URL: https://www.imelocker.com/apk/emi-locker-client.apk
# File: app/build/outputs/apk/release/app-release.apk

# Verify upload (must return HTTP 200)
curl -I https://www.imelocker.com/apk/emi-locker-client.apk

# Expected response:
# HTTP/2 200
# content-type: application/vnd.android.package-archive
```

### Step 4: Generate QR Code

**Use the `qr_provisioning.json` file generated in Step 2**

Example JSON:
```json
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.example.emilockerclient/.admin.EmiAdminReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://www.imelocker.com/apk/emi-locker-client.apk",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM": "6C:A7:1C:F9:7B:57:2D:6D:01:65:66:3E:0F:F2:8F:96:47:8C:48:32:B2:5C:C4:A7:F3:FE:0C:59:C9:F7:48:7E",
  "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": false,
  "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
  "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
    "device_id": "52",
    "seller_id": "191"
  }
}
```

**In Flutter Admin App:**
```dart
// 1. Copy qr_provisioning.json content
// 2. Update device_id and seller_id per device
// 3. Convert JSON to string
String qrData = jsonEncode(qrJson);

// 4. Generate QR code
QrImageView(
  data: qrData,
  version: QrVersions.auto,
  size: 300.0,
)
```

### Step 5: Provision Device

1. **Factory Reset Device**
   - Settings → System → Reset → Factory data reset
   
2. **Start Setup**
   - Connect to WiFi (or QR can include WiFi details)
   - At Welcome screen, tap screen 6 times to activate QR scanner
   
3. **Scan QR Code**
   - Point camera at QR code
   - Wait for "Getting ready for work setup" screen
   
4. **Wait for Provisioning**
   - Android downloads APK
   - Android verifies checksum
   - Android installs app
   - Android sets app as Device Owner
   - App launches automatically
   
5. **Verify Success**
   - MainActivity should show provisioning success
   - Check logs: `adb logcat | grep EmiAdminReceiver`
   - Expected logs:
     ```
     🎉 onProfileProvisioningComplete() called - Provisioning successful!
     ✅ Confirmed: App is Device Owner
     📦 Admin extras bundle received
     📝 Saving provisioning data
     🔐 Auto-granting permissions...
     ✅ Permissions granted
     🚀 Launching MainActivity...
     ```

## 🔧 Troubleshooting

### Error: "Can't setup device"

**Possible Causes & Solutions:**

1. **Checksum Mismatch**
   ```bash
   # Rebuild APK
   ./gradlew clean assembleRelease
   
   # Regenerate checksum
   python3 get_apk_checksum.py
   
   # Update QR JSON with new checksum
   # Regenerate QR code
   # Try provisioning again
   ```

2. **APK Not Accessible**
   ```bash
   # Test download
   curl -I https://www.imelocker.com/apk/emi-locker-client.apk
   
   # Should return HTTP 200
   # If 404, re-upload APK
   ```

3. **APK Not Properly Signed**
   ```bash
   # Verify APK signature
   keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk
   
   # Should show certificate details
   # If "Not a signed jar file", rebuild with signing config
   ```

4. **Component Name Mismatch**
   - Check QR JSON has: `"com.example.emilockerclient/.admin.EmiAdminReceiver"`
   - Must match exactly (case-sensitive)

5. **Network Issues**
   - Ensure device has internet connectivity during provisioning
   - Try different WiFi network
   - Check firewall/proxy settings

### Error: "App not installed"

**Solution:**
```bash
# Rebuild APK with proper signing
./gradlew clean assembleRelease

# Re-upload to server
# Regenerate checksum
# Update QR code
```

### Device Owner Not Set

**Check:**
```bash
# Connect device via ADB
adb shell dpm list-owners

# Should show:
# Device Owner:
# admin=com.example.emilockerclient/.admin.EmiAdminReceiver
```

**If not shown:**
- Provisioning failed
- Check logcat for errors
- Verify QR JSON format
- Try provisioning again

## 🔄 Updating the App

### For Code Updates (Same Keystore)

```bash
# 1. Make code changes
# 2. Build with SAME keystore
./gradlew clean assembleRelease

# 3. Checksum stays the SAME (no QR update needed!)
python3 get_apk_checksum.py

# 4. Upload new APK to server (replace old one)
# 5. Existing QR codes continue to work! ✅
```

### For Keystore Change (New Signing Key)

```bash
# 1. Create new keystore
./create_release_keystore.sh

# 2. Build with NEW keystore
./gradlew clean assembleRelease

# 3. Get NEW checksum
python3 get_apk_checksum.py

# 4. Update QR JSON with new checksum
# 5. Generate NEW QR codes
# 6. Upload new APK to server
# 7. Re-provision ALL devices with NEW QR code ⚠️
```

## 📱 Testing Checklist

Before provisioning:
- [ ] APK built and signed
- [ ] Checksum generated
- [ ] APK uploaded to server
- [ ] APK accessible via curl
- [ ] QR JSON updated
- [ ] QR code generated
- [ ] Device factory reset
- [ ] Device connected to WiFi

After provisioning:
- [ ] App installed and running
- [ ] Device Owner status confirmed
- [ ] Permissions granted
- [ ] Firebase initialized
- [ ] Lock commands work
- [ ] Location tracking works
- [ ] FCM messages received

## 🎯 Final Notes

### What Changed in This Fix:

1. **Removed DeviceProvisioningActivity**
   - Not needed for DPC apps
   - Android handles provisioning directly
   
2. **Fixed Download URL**
   - Changed from `api.imelocker.com/downloads/...`
   - To `www.imelocker.com/apk/...`
   
3. **Updated EmiAdminReceiver**
   - Properly extracts admin extras bundle
   - Saves device_id and seller_id to SharedPreferences
   
4. **Fixed MainActivity**
   - Uses correct SharedPreferences keys
   - No dependency on DeviceProvisioningActivity

### Critical Requirements:

✅ **Proper Signing** - Must use jarsigner or Android Studio signing
✅ **Correct Checksum** - Must match uploaded APK's certificate
✅ **Accessible URL** - Must return HTTP 200 with correct content-type
✅ **No testOnly Flag** - Removed from manifest
✅ **Internet Connectivity** - Device needs internet during provisioning

### Your App is Ready! 🚀

The app is now properly configured for QR code provisioning:
- All unnecessary components removed
- Correct URLs configured
- Proper manifest setup
- Ready for Device Owner provisioning

Just follow the build steps above and you should no longer see the "Can't setup device" error!

