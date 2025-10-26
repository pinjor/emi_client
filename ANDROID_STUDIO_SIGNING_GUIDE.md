# 📱 Generate Signed APK from Android Studio - Complete Guide

## ✅ What I Fixed

Your `app/build.gradle.kts` had a complex signing configuration that tried to load keystore info from `gradle.properties`. This caused issues with Android Studio's GUI-based signing workflow.

**Fixed:** Simplified the signing configuration to work seamlessly with Android Studio's "Generate Signed Bundle / APK" feature.

---

## 🚀 Step-by-Step: Generate Signed APK in Android Studio

### Step 1: Create Keystore (If You Don't Have One)

You have two options:

#### Option A: Create via Android Studio (Recommended)

1. In Android Studio, go to: **Build → Generate Signed Bundle / APK...**
2. Select **APK** → Click **Next**
3. Click **Create new...** (next to "Key store path")
4. Fill in the form:
   ```
   Key store path: /home/lazy/keystores/emi-release.jks
   Password: [Choose a strong password]
   Confirm: [Same password]
   
   Alias: emi_release
   Password: [Can be same as store password]
   Confirm: [Same password]
   
   Validity (years): 25
   
   Certificate:
   First and Last Name: EMI Locker
   Organizational Unit: Development
   Organization: EMI Locker Inc
   City or Locality: [Your city]
   State or Province: [Your state]
   Country Code (XX): US
   ```
5. Click **OK**
6. **⚠️ IMPORTANT: Backup this keystore file immediately!**

#### Option B: Create via Command Line

```bash
mkdir -p ~/keystores

keytool -genkeypair -v \
  -keystore ~/keystores/emi-release.jks \
  -alias emi_release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass YourStrongPassword \
  -keypass YourStrongPassword \
  -dname "CN=EMI Locker, OU=Development, O=EMI Locker Inc, L=City, ST=State, C=US"
```

**⚠️ Save your keystore credentials:**
```
Keystore Path: /home/lazy/keystores/emi-release.jks
Store Password: YourStrongPassword
Key Alias: emi_release
Key Password: YourStrongPassword
```

### Step 2: Generate Signed APK

1. **Build → Generate Signed Bundle / APK...**

2. Select **APK** → Click **Next**

3. **Key store path:** Browse to your keystore (e.g., `/home/lazy/keystores/emi-release.jks`)

4. **Key store password:** Enter your store password

5. **Key alias:** Select `emi_release` from dropdown

6. **Key password:** Enter your key password

7. Check **Remember passwords** (optional, for convenience)

8. Click **Next**

9. **Destination folder:** `app/release` (default is fine)

10. **Build variants:** Select **release**

11. **Signature Versions:**
    - ✅ Check **V1 (Jar Signature)** ← Important for QR provisioning!
    - ✅ Check **V2 (Full APK Signature)**
    - ✅ Check **V3 (Full APK Signature with key rotation)**
    - ✅ Check **V4 (APK Signature Scheme v4)**

12. Click **Finish**

### Step 3: Wait for Build

Android Studio will:
- Build your app
- Sign it with your keystore
- Show a notification when done: **"APK(s) generated successfully"**

### Step 4: Locate Your APK

The signed APK will be at:
```
app/release/app-release.apk
```

Or click **locate** in the notification popup.

---

## 🔐 Extract Certificate SHA-256 for QR Provisioning

After generating the signed APK, you need the certificate checksum for your QR provisioning JSON.

### Method 1: Using keytool (Simple)

```bash
keytool -list -v \
  -keystore ~/keystores/emi-release.jks \
  -alias emi_release \
  -storepass YourStrongPassword | grep SHA256
```

**Look for this line:**
```
SHA256: AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:...
```

Copy that value (with colons) - this is your certificate checksum!

### Method 2: Using Python Script (Automated)

```bash
# Use the script I created for you
python3 get_apk_checksum.py app/release/app-release.apk
```

This will:
- Extract the certificate from the APK
- Generate `qr_provisioning.json` automatically
- Display the checksum in the correct format

### Method 3: Verify APK Signature

```bash
# Verify the APK is properly signed
keytool -printcert -jarfile app/release/app-release.apk
```

---

## 📋 Create QR Provisioning JSON

### For Factory Reset QR (Device Owner Mode)

Create `qr_provisioning.json`:

```json
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.example.emilockerclient/.admin.EmiAdminReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://api.imelocker.com/downloads/emi-locker-client.apk",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM": "AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:...",
  "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": false,
  "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
  "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
    "device_id": "52",
    "seller_id": "191"
  }
}
```

Replace the `PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM` value with your actual SHA-256.

### For Running Device QR (Admin Mode Only)

For devices that can't be factory reset, your Flutter admin app should generate:

```json
{
  "apk_url": "https://api.imelocker.com/downloads/emi-locker-client.apk",
  "device_id": "52",
  "seller_id": "191"
}
```

---

## 🚢 Deployment Checklist

### Before First Release

- [ ] ✅ Build configuration fixed (done!)
- [ ] Create keystore via Android Studio or command line
- [ ] **Backup keystore** to at least 2 secure locations
- [ ] Generate signed APK via Android Studio
- [ ] Verify APK is signed: `keytool -printcert -jarfile app/release/app-release.apk`
- [ ] Extract certificate SHA-256
- [ ] Create `qr_provisioning.json` with correct checksum
- [ ] Upload APK to server: `https://api.imelocker.com/downloads/emi-locker-client.apk`
- [ ] Test download URL: `curl -I https://api.imelocker.com/downloads/emi-locker-client.apk`
- [ ] Generate QR code in Flutter admin app
- [ ] Test QR provisioning on factory-reset device

### For Updates

1. **Increment version in `app/build.gradle.kts`:**
   ```kotlin
   versionCode = 2  // Increment this
   versionName = "1.1"  // Update this
   ```

2. **Use the SAME keystore** for signing
   - Build → Generate Signed Bundle / APK
   - Use same keystore as before
   - Same passwords, same alias

3. **Upload new APK** to same URL (replace old file)

4. **Keep same QR JSON** - certificate checksum doesn't change!

---

## ⚠️ Important Notes

### Keystore Security

1. **Backup immediately** after creation:
   ```bash
   cp ~/keystores/emi-release.jks ~/Backups/emi-release-$(date +%Y%m%d).jks
   ```

2. **Store credentials securely:**
   - Use password manager
   - Document in secure location
   - Share with team via secure channel

3. **Never commit to git:**
   - Already in `.gitignore`
   - Don't share publicly

### If You Lose Your Keystore

⚠️ **You CANNOT update your app!**
- Users must uninstall and reinstall
- Device owner provisioning will break
- You'll need a new keystore = new app signature

### Certificate Checksum Format

Android QR provisioning accepts **TWO formats:**

1. **Colon-separated (preferred):**
   ```
   AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:...
   ```

2. **URL-safe Base64 (alternative):**
   ```
   qrvM-XtXLW0BZWfjD_KPlkeEyDKyXMSn8_4MWcn3SH4
   ```

The `keytool` output gives you format #1 (which is what you need).

---

## 🔧 Troubleshooting

### "Error: Keystore was tampered with, or password was incorrect"

**Solution:** Check your password carefully. Passwords are case-sensitive.

### "APK not signed" or "V1 signature missing"

**Solution:** 
1. Make sure you checked **V1 (Jar Signature)** when generating the APK
2. The updated `build.gradle.kts` already has `enableV1Signing = true`

### "Cannot find keystore"

**Solution:** 
- Use absolute path: `/home/lazy/keystores/emi-release.jks`
- Not relative path: `~/keystores/emi-release.jks` might not work in Android Studio

### Build fails in Android Studio

**Solution:**
1. **File → Invalidate Caches / Restart**
2. **Build → Clean Project**
3. **Build → Rebuild Project**
4. Try generating signed APK again

---

## 📝 Quick Reference Commands

```bash
# Create keystore (command line)
keytool -genkeypair -v -keystore ~/keystores/emi-release.jks -alias emi_release -keyalg RSA -keysize 2048 -validity 10000

# Get SHA-256 from keystore
keytool -list -v -keystore ~/keystores/emi-release.jks -alias emi_release | grep SHA256

# Verify APK signature
keytool -printcert -jarfile app/release/app-release.apk

# Extract checksum from APK (automated)
python3 get_apk_checksum.py app/release/app-release.apk

# Backup keystore
cp ~/keystores/emi-release.jks ~/Backups/emi-release-backup.jks
```

---

## ✅ Summary

**What Changed:**
- ✅ Fixed `app/build.gradle.kts` signing configuration
- ✅ Enabled V1/V2/V3/V4 signing for maximum compatibility
- ✅ Removed gradle.properties dependency (cleaner for Android Studio)

**What You Need to Do:**
1. Create keystore (via Android Studio GUI or command line)
2. Generate signed APK via Android Studio
3. Extract certificate SHA-256
4. Create QR provisioning JSON
5. Upload APK and test!

**Your app is now ready for production signing via Android Studio!** 🚀

