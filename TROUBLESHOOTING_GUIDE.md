# Troubleshooting "Can't set up device" Error

## ✅ What We've Verified

- ✅ APK is properly signed with SHA-256 certificate
- ✅ APK is accessible at https://www.imelocker.com/apk/emi-locker-client.apk
- ✅ QR code uses correct SHA-256 checksum
- ✅ Skip encryption is enabled (helps with OEM compatibility)
- ✅ No testOnly flag in manifest
- ✅ DeviceProvisioningActivity is properly configured

## 🔍 Most Common Causes

### 1. Device Doesn't Support Work Profiles (Most Likely)

**Many manufacturers block factory reset provisioning:**
- Samsung devices (especially with OneUI)
- Xiaomi/Redmi devices
- Some Huawei devices
- Oppo/Vivo devices

**Solution:** Test on a **Pixel device or stock Android** device if possible.

### 2. APK Download/Installation Fails

**Test:** Can you manually install the APK?

1. Download the APK: https://www.imelocker.com/apk/emi-locker-client.apk
2. Enable "Install from Unknown Sources" in device settings
3. Try to install the APK manually

**If it won't install:** The APK has compatibility issues that need to be fixed before provisioning can work.

**If it installs:** The device should support provisioning.

### 3. Device Already Has Device Owner

If the device has been tested before, it might already have a Device Owner set.

**Fix:** 
1. Enable ADB debugging
2. Run: `adb shell dpm list-owners`
3. If it shows your app, run: `adb shell dpm set-device-owner com.example.emilockerclient/.admin.EmiAdminReceiver`

### 4. Android Version Issues

**Android 11 and below:**
- Sometimes has provisioning bugs
- Try on Android 12+ if possible

**Android 13+:**
- More strict about provisioning
- Requires factory reset (which you're doing)

## 🧪 Diagnostic Steps

### Step 1: Check Device Model & Android Version

**What device are you testing on?** (e.g., Pixel 7, Samsung Galaxy S21, Xiaomi Note 10, etc.)
**Android version?** (Settings → About phone)

This helps identify compatibility issues.

### Step 2: Enable ADB and Check Logs

If possible, connect the device via USB during provisioning:

```bash
adb logcat | grep -i provisioning
```

This will show what's actually failing during provisioning.

### Step 3: Test APK Installation Manually

**Download and install the APK manually:**
```bash
# Download APK
curl -o test.apk https://www.imelocker.com/apk/emi-locker-client.apk

# Install on device
adb install -r test.apk
```

**If installation fails**, that's the issue. If it succeeds, provisioning should work.

### Step 4: Try Without Encryption

The QR already has `PROVISIONING_SKIP_ENCRYPTION` set to `true`. This is correct.

Some devices require **both**:
- `PROVISIONING_SKIP_ENCRYPTION: true`
- `PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED: true` ✅ (already set)

### Step 5: Test on Different Device

If possible:
- Test on a Pixel device
- Test on Android 12+ stock Android

Samsung and Xiaomi devices are known to block factory reset provisioning.

## 🔧 Alternative Approach: Test Programmatically

If factory reset provisioning doesn't work on your device:

### Option 1: Manual Device Owner Setup (After App Install)

1. Install the APK manually
2. Enable ADB
3. Set as Device Owner:
```bash
adb shell dpm set-device-owner com.example.emilockerclient/.admin.EmiAdminReceiver
```

### Option 2: Use NFC Provisioning Instead

NFC provisioning sometimes works on devices that block QR provisioning.

### Option 3: OEM-Specific Provisioning

Some manufacturers have special provisioning methods (Samsung Knox, etc.)

## 📋 Quick Checklist

Before testing, ensure:
- [ ] Factory reset completely (remove all accounts first)
- [ ] WiFi connected
- [ ] QR code generated from the correct JSON with SHA-256
- [ ] Device supports work profiles
- [ ] Android version is 10+ (preferably 12+)
- [ ] No existing Device Owner on the device

## 🆘 Still Failing?

Please provide:
1. **Device model** (e.g., Samsung Galaxy S21, Pixel 7, etc.)
2. **Android version** (e.g., Android 13)
3. **Manufacturer** (Samsung, Xiaomi, etc.)
4. **Can you manually install the APK?** (Yes/No)
5. **Any error messages in logcat?** (if you have ADB access)

With this information, I can provide device-specific fixes.

## 💡 Quick Test

**Test the simplest possible scenario:**

Create a test QR code with minimal data:

```json
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.example.emilockerclient/.admin.EmiAdminReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM": "0A:7D:81:7E:E5:CC:16:DA:41:ED:BB:A8:7A:91:72:64:1C:F0:28:1D:F9:23:CD:E8:11:C0:E1:07:6B:FD:29:D8",
  "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": true
}
```

If this fails, it's almost certainly a device compatibility issue.

