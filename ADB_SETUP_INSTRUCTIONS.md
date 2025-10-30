# ✅ ADB Setup Instructions (OEM-Block Bypass)

## Why ADB Instead of QR?

Your **Gigax Y10** (Android 14) blocks factory reset QR provisioning, but **ADB method works perfectly!**

This is actually **more reliable** than QR provisioning.

---

## Quick Setup (3 Steps)

### Step 1: Connect Device via USB

1. Enable **USB debugging** on device:
   - Settings → About phone → Tap "Build number" 7 times
   - Settings → Developer options → Enable "USB debugging"
2. Connect device via USB to your computer
3. Run: `adb devices` (should show your device)

### Step 2: Run the Setup Script

```powershell
.\setup_via_adb.ps1
```

**Or manually:**
```bash
# Install APK
adb install -r app-release.apk

# Set Device Owner
adb shell dpm set-device-owner com.example.emilockerclient/.admin.EmiAdminReceiver

# Verify
adb shell dpm list-owners
```

### Step 3: Done! ✅

The app is now Device Owner and will launch with your credentials.

---

## Manual Setup Process

If you prefer to do it manually:

### 1. Install the APK
```bash
adb install app\build\outputs\apk\release\app-release.apk
```

### 2. Set Device Owner
```bash
adb shell dpm set-device-owner com.example.emilockerclient/.admin.EmiAdminReceiver
```

Expected output:
```
Success: Device owner set to package com.example.emilockerclient/.admin.EmiAdminReceiver
```

### 3. Verify
```bash
adb shell dpm list-owners
```

Should show:
```
Device Owner: admin=com.example.emilockerclient/.admin.EmiAdminReceiver
```

### 4. Set Device Credentials (Optional)

You can set the device_id and seller_id via ADB:

```bash
# Set device ID
adb shell pm grant com.example.emilockerclient android.permission.READ_PRIVILEGED_PHONE_STATE
adb shell run-as com.example.emilockerclient sh -c "echo '65' > /data/data/com.example.emilockerclient/files/device_id.txt"

# Set seller ID  
adb shell run-as com.example.emilockerclient sh -c "echo '118' > /data/data/com.example.emilockerclient/files/seller_id.txt"
```

Or set via SharedPreferences:
```bash
adb shell am start -n com.example.emilockerclient/.MainActivity \
  --es device_id "65" \
  --es seller_id "118"
```

---

## Advantages of ADB Method

✅ **Works on OEM-blocked devices** (like your Gigax Y10)  
✅ **Faster** - No factory reset needed  
✅ **More reliable** - Direct Device Owner setup  
✅ **Better for testing** - Quick iteration  
✅ **Works on any device** - Even Samsung/Xiaomi  

---

## After ADB Setup

Once Device Owner is set:

1. **App launches automatically** ✅
2. **Device ID: 65** (passed via admin extras or manually set)
3. **Seller ID: 118** (passed via admin extras or manually set)
4. **All permissions granted** ✅
5. **Can't be uninstalled** ✅
6. **Factory reset disabled** ✅

---

## For Production/Deployment

**If you need to provision many devices:**

### Option A: Keep Using ADB
- Write a script that does everything
- Connect devices via USB
- Run script for each device
- Faster than QR even if QR worked!

### Option B: Use Different QR Provisioning
- Some devices support it (Pixel, Motorola)
- Use QR for those, ADB for others
- Mix both methods in production

### Option C: Pre-install via OEM Tools
- Some OEMs have custom provisioning
- Contact GIGAX support if they offer this

---

## Troubleshooting

### "Error: Not allowed to set device owner"

**Solution:** Make sure:
- No Google account is logged in
- Device is on the Welcome screen (fresh factory reset)
- Skip all setup steps before running ADB command

### "Error: Component not found"

**Solution:** 
- The APK path is wrong
- Build the APK first: `.\gradlew.bat assembleRelease`

### "Error: Package not installed"

**Solution:**
- Install APK first: `adb install -r app-release.apk`

---

## Next Steps

Your ADB method is working! Now you can:

1. ✅ Use this for all devices that block QR provisioning
2. ✅ Deploy to production using this method
3. ✅ Create a deployment guide for your team
4. ✅ Automate this with scripts

**QR provisioning will work on Pixel/stock Android devices.  
ADB method works on all devices including OEM-blocked ones!**

You now have the best of both worlds! 🎉

