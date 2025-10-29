# 🔧 How to Fix the Reset Loop Issue

## Problem
Your provisioning fails and device shows "Can't set up device" error in a loop because:

1. ❌ **Signature Checksum Mismatch** - The checksum in your QR code doesn't match your APK signature
2. ❌ **APK not accessible** - Device can't download the APK from your server

## Solution (Do This Now)

### Step 1: Build Your APK
```bash
cd W:\D_folder\EMIII\emi_locker_client_app
gradlew.bat clean assembleRelease
```

### Step 2: Get Correct Checksum
```bash
python generate_checksum.py app\build\outputs\apk\release\app-release.apk
```

**Copy the output** - it looks like:
```
E0:29:08:BD:F6:FA:CA:D9:C5:E0:D9:9D:8B:4F:61:5C:7D:39:95:C4:DE:24:9C:84:8E:EA:B1:21:2D:48:B7:FE
```

### Step 3: Update Your Admin App
In your Flutter Admin App, update the QR code generation to use the NEW checksum:

```dart
final provisioningData = {
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": 
      "com.example.emilockerclient/.admin.EmiAdminReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": 
      "https://www.imelocker.com/apk/emi-locker-client.apk",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM": 
      "PUT_YOUR_NEW_CHECKSUM_HERE",  // ⬅️ Replace this!
  "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": false,
  "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
  "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
    "device_id": deviceId.toString(),
    "seller_id": sellerId.toString(),
  }
};
```

### Step 4: Upload APK to Server
```bash
# Upload the built APK to your server
# Make sure it's accessible at: https://www.imelocker.com/apk/emi-locker-client.apk
```

### Step 5: Test
1. Factory reset your device
2. Tap 6-7 times on "Hi there" to open QR scanner
3. Scan your updated QR code
4. Should work now! ✅

## Code Changes Made (Already Applied)

✅ Added `DeviceProvisioningActivity` to `AndroidManifest.xml`  
✅ Added re-provisioning check to prevent conflicts  
✅ Added error handling in `EmiAdminReceiver.onProfileProvisioningComplete()`  
✅ Added detailed logging for debugging  

## Why This Happens

**Android Enterprise Provisioning** is very strict about security:
- The signature checksum must match EXACTLY
- Even one character difference = provisioning failure
- If provisioning fails, Android shows "Can't set up device" with reset option
- That's why you're seeing the reset loop

## Quick Test (Without Factory Reset)

If you want to test immediately without resetting:

```bash
# Install the APK
adb install app/build/outputs/apk/release/app-release.apk

# Set as Device Owner
adb shell dpm set-device-owner com.example.emilockerclient/.admin.EmiAdminReceiver

# Verify it worked
adb shell dpm list-owners
```

If you see `com.example.emilockerclient (Device Owner)`, it worked! ✅

## Summary

The main issue is **signature checksum mismatch**. Your admin app is generating QR codes with an old checksum that doesn't match your current APK signature.

**Fix:** 
1. Build APK → `gradlew.bat assembleRelease`
2. Get checksum → `python generate_checksum.py app\build\outputs\apk\release\app-release.apk`  
3. Update admin app QR generation with new checksum
4. Upload APK to server
5. Generate new QR codes and test
