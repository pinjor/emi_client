# Android 14 - QR Provisioning Fix for Gigax Y10

## Issue: Android 14 + OEM Blocking

The "Can't set up device" error on Android 14 is **often caused by OEM restrictions**, even though Android 14 officially supports provisioning.

## 🔍 Gigax Y10 Device Information

**Possible Identities:**
- Could be a **Vivo/Motorola/Oppo** variant
- Check: Settings → About phone → Model number

## 🛠️ Android 14 Specific Issues

Android 14 introduced some changes that can affect provisioning:

### Issue 1: Stricter Package Validation

Android 14 validates APKs more strictly during provisioning.

**Check:** Does the APK install manually? If not, that's the blocker.

### Issue 2: OEM Restrictions

Many OEMs on Android 14 block factory reset provisioning to force use of their own Device Policy apps.

## ✅ Solutions to Try

### Solution 1: Test Manual APK Installation

1. Download APK: https://www.imelocker.com/apk/emi-locker-client.apk
2. Install manually: Settings → Security → Allow installation from unknown sources
3. Install the APK

**If APK installs:** Device supports provisioning, there's another issue  
**If APK fails:** Device is blocking the app itself

### Solution 2: Try Different OEM Settings

Some devices require enabling "Allow factory reset provisioning" in settings:

1. Go to: Settings → Security → Work profile
2. Enable "Allow factory reset QR provisioning" (if option exists)
3. Try provisioning again

### Solution 3: Enable USB Debugging and Get Detailed Logs

If possible, connect device via USB and capture logs:

```bash
# Clear logcat
adb logcat -c

# Watch provisioning logs
adb logcat | grep -E "DeviceProvisioning|Provisioning|EmiAdmin"
```

This will show exactly where provisioning is failing.

### Solution 4: Try ADB-Based Device Owner Setup

Instead of QR provisioning, set Device Owner directly via ADB:

1. **Install APK manually** on the device
2. **Enable ADB debugging**: Settings → Developer options → USB debugging
3. **Connect via USB** to computer
4. **Run these commands:**

```bash
# Check current device owner
adb shell dpm list-owners

# Set your app as device owner
adb shell dpm set-device-owner com.example.emilockerclient/.admin.EmiAdminReceiver

# Verify
adb shell dpm list-owners
```

This should show: `Device Owner: admin=com.example.emilockerclient/.admin.EmiAdminReceiver`

### Solution 5: Check for "MI Account" or "OEM Account" Requirements

Some Android 14 OEM devices require you to:
1. Not add any Google account during initial setup
2. Skip all setup steps before scanning QR
3. Some require removing "Find my device" and OEM accounts first

### Solution 6: Android 14 + Work Profile Restrictions

If Android 14 is detecting the device as a "work device":
1. During factory reset setup, **skip** all Google account prompts
2. **Skip** all "Finish setup" prompts
3. Go directly to QR scanner
4. Scan QR code **before** signing into any accounts

## 🧪 Quick Test

Try this minimal QR code to test if provisioning works at all:

```json
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.example.emilockerclient/.admin.EmiAdminReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM": "0A:7D:81:7E:E5:CC:16:DA:41:ED:BB:A8:7A:91:72:64:1C:F0:28:1D:F9:23:CD:E8:11:C0:E1:07:6B:FD:29:D8",
  "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": true,
  "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true
}
```

(Remove the admin extras bundle to test basic provisioning first)

## 📱 Device-Specific Requirements for Android 14

### Manufacturers Known to Block on Android 14:
- **Samsung** (OneUI 6) - Often blocks third-party provisioning
- **Xiaomi** (MIUI 14/15) - Usually blocks
- **Oppo/Realme** - Often blocks
- **Vivo** - Can be blocked
- **Motorola** - Usually works
- **Pixel** - Always works
- **Nothing/OnePlus** - Usually works

## 🔧 Alternative: Use ADB Instead of QR

If QR provisioning is blocked by the OEM, you can still set Device Owner via ADB:

### Steps:

1. **Factory Reset Device**

2. **Skip all setup** (no Google account, no OEM account)

3. **Enable Developer Options:**
   - Settings → About phone
   - Tap "Build number" 7 times

4. **Enable USB Debugging:**
   - Settings → Developer options
   - Enable "USB debugging"

5. **Connect to PC via USB**

6. **Install APK:**
```bash
adb install app-release.apk
```

7. **Set Device Owner:**
```bash
# This will set the app as Device Owner without QR code
adb shell dpm set-device-owner com.example.emilockerclient/.admin.EmiAdminReceiver

# Verify it worked
adb shell dpm list-owners
```

This bypasses the QR provisioning flow entirely!

## ❓ Questions to Help Debug

1. **Can you manually install the APK?** (Yes/No)
2. **What happens when you scan the QR code?** (Error immediately? After download? During install?)
3. **Is the device unlocked/rooted?** (Some OEM blocks require device to be unlocked)
4. **Are you adding any Google/OEM accounts during setup?** (Should skip all of them)
5. **Do you have access to ADB?** (Can help get detailed error messages)

## 🎯 Recommended Next Steps

**Most likely working solution:**

1. Install APK manually via USB
2. Use ADB to set Device Owner:
```bash
adb install -r app-release.apk
adb shell dpm set-device-owner com.example.emilockerclient/.admin.EmiAdminReceiver
```

This bypasses OEM restrictions!

Let me know what happens when you try manual installation and ADB setup.

