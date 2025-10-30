# QR Provisioning - Final Checklist ✅

## Current Status

### ✅ APK Status
- **File**: `app\build\outputs\apk\release\app-release.apk`
- **Size**: 16.7 MB
- **Signed**: ✅ Yes
- **Certificate**: EMI Locker, OU=Development
- **SHA-256**: `0A:7D:81:7E:E5:CC:16:DA:41:ED:BB:A8:7A:91:72:64:1C:F0:28:1D:F9:23:CD:E8:11:C0:E1:07:6B:FD:29:D8`
- **Server**: https://www.imelocker.com/apk/emi-locker-client.apk

### ✅ QR JSON Status
- **File**: `qr_provisioning.json`
- **Certificate Checksum**: ✅ SHA-256 format (correct)
- **Skip Encryption**: ✅ Enabled (helps with OEM compatibility)
- **Device ID**: 65
- **Seller ID**: 118

---

## ⚠️ CRITICAL: Use SHA-256, NOT SHA-1!

The QR code **MUST** use SHA-256 checksum, not SHA-1:
- ❌ **WRONG**: `E0:8C:E7:E8:40:F0:DB:A9:8A:07:C2:C3:BB:CB:C1:B0:8E:6B:C7:39` (SHA-1)
- ✅ **CORRECT**: `0A:7D:81:7E:E5:CC:16:DA:41:ED:BB:A8:7A:91:72:64:1C:F0:28:1D:F9:23:CD:E8:11:C0:E1:07:6B:FD:29:D8` (SHA-256)

---

## Steps to Test

### 1. Upload New APK to Server

Upload the **latest** APK from:
```
W:\D_folder\emi client\emi_client\app\build\outputs\apk\release\app-release.apk
```

To server location:
```
https://www.imelocker.com/apk/emi-locker-client.apk
```

**Replace the existing file on the server!**

### 2. Generate QR Code

**Copy this exact JSON** (from `qr_provisioning.json`):
```json
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.example.emilockerclient/.admin.EmiAdminReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://www.imelocker.com/apk/emi-locker-client.apk",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM": "0A:7D:81:7E:E5:CC:16:DA:41:ED:BB:A8:7A:91:72:64:1C:F0:28:1D:F9:23:CD:E8:11:C0:E1:07:6B:FD:29:D8",
  "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": true,
  "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
  "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
    "device_id": "65",
    "seller_id": "118"
  }
}
```

**Generate QR code from this JSON** (use any online QR generator or your Flutter app).

### 3. Test on Device

1. **Factory Reset** the device completely
2. **Connect to WiFi**
3. At Welcome screen, **tap 6 times** to open QR scanner
4. **Scan the QR code**
5. Wait 2-5 minutes for provisioning

### Expected Results

✅ APK downloads from server  
✅ Certificate verified  
✅ App installs  
✅ Device Owner enabled  
✅ App launches with Device ID 65, Seller ID 118  

---

## Troubleshooting

### If "Can't set up device" persists:

1. **Check device compatibility**
   - Try a Pixel or stock Android device
   - Some Samsung/Xiaomi devices block work profiles

2. **Verify APK is accessible**
   ```powershell
   Invoke-WebRequest -Uri "https://www.imelocker.com/apk/emi-locker-client.apk" -Method Head
   ```
   Should return: `Content-Length: 16691865` and `Content-Type: application/vnd.android.package-archive`

3. **Check the QR code**
   - Ensure it uses **SHA-256** checksum (with colons)
   - Don't use SHA-1!
   - Verify the JSON structure is correct

4. **Try on different Android version**
   - Some Android 11 devices have provisioning bugs
   - Android 12+ is most reliable

5. **Manual APK install test**
   - Try installing the APK manually first
   - If it won't install, provisioning will fail too

---

## Key Differences in This Configuration

✅ **Skip Encryption**: Enabled (`true`) - Helps with OEM compatibility issues  
✅ **Leave All System Apps**: Enabled - Prevents conflicts with system apps  
✅ **SHA-256**: Using correct format for Android 12+ provisioning  
✅ **Device Owner Component**: Properly configured in manifest  

---

## File Locations

- **Local APK**: `W:\D_folder\emi client\emi_client\app\build\outputs\apk\release\app-release.apk`
- **Server APK**: https://www.imelocker.com/apk/emi-locker-client.apk
- **QR JSON**: `W:\D_folder\emi client\emi_client\qr_provisioning.json`
- **Keystore**: `W:\D_folder\emi client\emi_client\keystores\emi-release.jks`

---

## Next Steps

1. ✅ APK rebuilt with latest settings
2. ⏳ **Upload APK to server** (replace existing)
3. ✅ Generate QR from updated JSON
4. ⏳ Test provisioning
5. ✅ Done!

Let me know if you need help uploading the APK or if provisioning still fails!

