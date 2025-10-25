# 📱 QR Code Provisioning - Complete Guide

## 🚀 Quick Start (3 Steps)

### Step 1: Build Your Signed APK

```bash
cd /home/lazy/AndroidStudioProjects/EmiLockerClient
./gradlew assembleRelease
```

**⚠️ Important:** Your APK must be signed! See "APK Signing Setup" below if you get unsigned APK.

---

### Step 2: Get SHA-256 Checksum

**Option A: Use Python Script (Recommended)**
```bash
python3 get_apk_checksum.py
```

**Option B: Use Bash Script**
```bash
chmod +x get_apk_checksum.sh
./get_apk_checksum.sh
```

**Option C: Manual Command**
```bash
sha256sum app/build/outputs/apk/release/app-release.apk
```

The script will:
- ✅ Calculate SHA-256 checksum
- ✅ Format it correctly for Android provisioning
- ✅ Generate `qr_provisioning.json` file
- ✅ Show you the formatted checksum to use

**Output Example:**
```
📋 Formatted for Android (use this in QR):
   A1:B2:C3:D4:E5:F6:78:90:12:34:56:78:90:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56
```

---

### Step 3: Upload APK and Use JSON

1. **Upload APK** to your server:
   ```
   https://api.imelocker.com/downloads/emi-locker-client.apk
   ```

2. **Copy the generated `qr_provisioning.json`** content

3. **In your Flutter Admin App**, use this JSON to generate QR codes

4. **Update device_id and seller_id** for each unique device/customer

---

## 🔑 APK Signing Setup (One-Time)

If your APK is unsigned, you need to set up signing:

### Method 1: Create Keystore

```bash
keytool -genkey -v -keystore ~/emi-locker-keystore.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias emi-locker-key
```

**Remember the password you set!**

### Method 2: Configure Gradle Auto-Signing

Add to `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../emi-locker-keystore.jks")
            storePassword = "YOUR_PASSWORD_HERE"
            keyAlias = "emi-locker-key"
            keyPassword = "YOUR_PASSWORD_HERE"
        }
    }
    
    buildTypes {
        release {
            // ...existing code...
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

Then rebuild:
```bash
./gradlew clean assembleRelease
```

---

## 📋 QR JSON Format

```json
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.example.emilockerclient/.admin.EmiAdminReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://api.imelocker.com/downloads/emi-locker-client.apk",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM": "A1:B2:C3:...",
  "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": false,
  "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
  "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
    "device_id": "52",
    "seller_id": "191"
  }
}
```

**What to change for each device:**
- `device_id`: Unique ID from your backend (e.g., "52", "53", "54"...)
- `seller_id`: ID of the seller who sold this device (e.g., "191")
- `PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM`: Only change if you rebuild APK

---

## 🧪 Testing Provisioning

### Option A: Real Provisioning (Factory Reset)

1. Factory reset device
2. On welcome screen, tap 6 times on "Welcome" text
3. QR scanner appears
4. Scan QR code
5. Device provisions automatically
6. App becomes Device Owner ✅

### Option B: Testing Without Factory Reset

```bash
# Install APK
adb install -r app/build/outputs/apk/release/app-release.apk

# Set as Device Owner manually
adb shell dpm set-device-owner com.example.emilockerclient/.admin.EmiAdminReceiver

# Verify
adb shell dpm list-owners
```

---

## 🔄 Updating APK

When you update your app:

1. Build new APK: `./gradlew assembleRelease`
2. Calculate new checksum: `python3 get_apk_checksum.py`
3. Update QR JSON with new checksum
4. Upload new APK to server
5. Generate new QR codes

**⚠️ Checksum must match APK exactly or provisioning fails!**

---

## 📱 Flutter Admin App Integration

```dart
import 'package:qr_flutter/qr_flutter.dart';
import 'dart:convert';

String generateProvisioningQR(int deviceId, int sellerId, String checksum) {
  final provisioningData = {
    "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": 
        "com.example.emilockerclient/.admin.EmiAdminReceiver",
    "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": 
        "https://api.imelocker.com/downloads/emi-locker-client.apk",
    "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM": checksum,
    "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": false,
    "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
    "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
      "device_id": deviceId.toString(),
      "seller_id": sellerId.toString(),
    }
  };
  
  return jsonEncode(provisioningData);
}

// Widget
QrImageView(
  data: generateProvisioningQR(52, 191, "A1:B2:C3:..."),
  version: QrVersions.auto,
  size: 300.0,
)
```

---

## ❓ Troubleshooting

**Problem:** APK not found after build
- **Solution:** Check `app/build/outputs/apk/release/` directory
- Run: `ls -lh app/build/outputs/apk/release/`

**Problem:** "APK is not signed"
- **Solution:** Follow "APK Signing Setup" section above

**Problem:** Provisioning fails with "Package signature mismatch"
- **Solution:** Checksum doesn't match APK. Recalculate and update QR

**Problem:** Device doesn't show QR scanner
- **Solution:** Tap 6 times on welcome screen (may vary by Android version)

**Problem:** Can't access serial number after provisioning
- **Solution:** Make sure app is Device Owner: `adb shell dpm list-owners`

---

## 📞 Need Help?

Check logs during provisioning:
```bash
adb logcat | grep -E "DeviceProvisioning|EmiAdminReceiver|PermissionManager"
```

