# 🚀 Complete Implementation Guide - QR Provisioning Setup

## 📅 Date: October 25, 2025

This guide covers EVERYTHING you need to do in both Android DPC app and Flutter Admin app to get QR provisioning working.

---

# 📱 PART 1: ANDROID DPC APP (CURRENT PROJECT)

## ✅ Step 1: Build the Signed APK

Open terminal and run:

```bash
cd /home/lazy/AndroidStudioProjects/EmiLockerClient

# Clean previous builds
./gradlew clean

# Build signed release APK
./gradlew assembleRelease
```

**Expected output location:**
```
app/build/outputs/apk/release/app-release.apk
```

**Time:** ~2-5 minutes depending on your system

---

## ✅ Step 2: Calculate SHA-256 Checksum

Run the Python script I created:

```bash
python3 get_apk_checksum.py
```

**What this does:**
- ✅ Finds your APK automatically
- ✅ Calculates SHA-256 hash
- ✅ Formats it with colons (A1:B2:C3:...)
- ✅ Creates `qr_provisioning.json` file
- ✅ Shows you the formatted checksum

**Example output:**
```
📱 APK found: app/build/outputs/apk/release/app-release.apk
📏 File size: 8.45 MB

🔐 Calculating SHA-256 checksum...

✅ Raw SHA-256:
   a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456

📋 Formatted for Android (use this in QR):
   A1:B2:C3:D4:E5:F6:78:90:12:34:56:78:90:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56

✅ QR JSON saved to: qr_provisioning.json
```

**SAVE THIS CHECKSUM!** You'll need it in Flutter app.

---

## ✅ Step 3: Upload APK to Your Server

Upload the APK to your Laravel server:

**Upload location:**
```
https://api.imelocker.com/downloads/emi-locker-client.apk
```

**How to upload:**

### Option A: Using SCP (if you have SSH access)
```bash
scp app/build/outputs/apk/release/app-release.apk \
  user@api.imelocker.com:/path/to/public/downloads/emi-locker-client.apk
```

### Option B: Using Laravel Admin Panel
1. Login to your Laravel admin panel
2. Go to File Manager or Downloads section
3. Upload the APK file
4. Make sure it's accessible at the URL above

### Option C: Using FTP/cPanel
1. Login to your hosting control panel
2. Navigate to `/public_html/downloads/` or `/public/downloads/`
3. Upload `app-release.apk`
4. Rename to `emi-locker-client.apk`

**Verify upload:**
```bash
curl -I https://api.imelocker.com/downloads/emi-locker-client.apk
```

Should return: `HTTP/1.1 200 OK`

---

## ✅ Step 4: Note Down Your QR JSON Template

Open the generated `qr_provisioning.json` file:

```bash
cat qr_provisioning.json
```

**Copy this entire JSON** - you'll need it for Flutter app!

It looks like:
```json
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.example.emilockerclient/.admin.EmiAdminReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://api.imelocker.com/downloads/emi-locker-client.apk",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM": "A1:B2:C3:D4:E5:F6:...",
  "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": false,
  "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
  "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
    "device_id": "52",
    "seller_id": "191"
  }
}
```

**✅ ANDROID APP IS NOW COMPLETE!**

---

# 🦋 PART 2: FLUTTER ADMIN APP

Now let's set up the Flutter app to generate QR codes.

## ✅ Step 1: Add QR Code Package

Open your Flutter Admin App project and add the QR package:

**pubspec.yaml:**
```yaml
dependencies:
  flutter:
    sdk: flutter
  qr_flutter: ^4.1.0  # Add this line
  # ...your other dependencies
```

Then run:
```bash
flutter pub get
```

---

## ✅ Step 2: Create QR Provisioning Helper Class

Create a new file: `lib/helpers/qr_provisioning_helper.dart`

```dart
import 'dart:convert';

class QRProvisioningHelper {
  // 🔐 This is the SHA-256 checksum from your Android APK
  // ⚠️ REPLACE THIS with the actual checksum from get_apk_checksum.py output
  static const String APK_CHECKSUM = "A1:B2:C3:D4:E5:F6:78:90:12:34:56:78:90:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56";
  
  static const String APK_DOWNLOAD_URL = "https://api.imelocker.com/downloads/emi-locker-client.apk";
  static const String ADMIN_COMPONENT_NAME = "com.example.emilockerclient/.admin.EmiAdminReceiver";
  
  /// Generate QR code provisioning JSON string
  static String generateProvisioningQR(int deviceId, int sellerId) {
    final provisioningData = {
      "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": ADMIN_COMPONENT_NAME,
      "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": APK_DOWNLOAD_URL,
      "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM": APK_CHECKSUM,
      "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": false,
      "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
      "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
        "device_id": deviceId.toString(),
        "seller_id": sellerId.toString(),
      }
    };
    
    return jsonEncode(provisioningData);
  }
}
```

**⚠️ IMPORTANT:** Replace `APK_CHECKSUM` with the actual checksum from Step 2 of Android app!

---

## ✅ Step 3: Create QR Code Display Widget

Create a new file: `lib/widgets/device_provisioning_qr.dart`

```dart
import 'package:flutter/material.dart';
import 'package:qr_flutter/qr_flutter.dart';
import '../helpers/qr_provisioning_helper.dart';

class DeviceProvisioningQR extends StatelessWidget {
  final int deviceId;
  final int sellerId;
  
  const DeviceProvisioningQR({
    Key? key,
    required this.deviceId,
    required this.sellerId,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final qrData = QRProvisioningHelper.generateProvisioningQR(deviceId, sellerId);
    
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              'Device Provisioning QR Code',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            SizedBox(height: 8),
            Text(
              'Device ID: $deviceId | Seller ID: $sellerId',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
            SizedBox(height: 16),
            // QR Code
            QrImageView(
              data: qrData,
              version: QrVersions.auto,
              size: 300.0,
              backgroundColor: Colors.white,
            ),
            SizedBox(height: 16),
            Text(
              'Scan this QR code on factory-reset device',
              style: Theme.of(context).textTheme.bodySmall,
              textAlign: TextAlign.center,
            ),
            SizedBox(height: 8),
            // Instructions
            Container(
              padding: EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.blue.shade50,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Instructions:',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                  SizedBox(height: 4),
                  Text('1. Factory reset the device'),
                  Text('2. On welcome screen, tap 6 times'),
                  Text('3. QR scanner appears'),
                  Text('4. Scan this code'),
                  Text('5. Wait for provisioning to complete'),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
```

---

## ✅ Step 4: Create QR Code Display Screen

Create a new file: `lib/screens/device_qr_screen.dart`

```dart
import 'package:flutter/material.dart';
import '../widgets/device_provisioning_qr.dart';

class DeviceQRScreen extends StatelessWidget {
  final int deviceId;
  final int sellerId;
  final String customerName;
  
  const DeviceQRScreen({
    Key? key,
    required this.deviceId,
    required this.sellerId,
    required this.customerName,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Device Provisioning'),
        actions: [
          IconButton(
            icon: Icon(Icons.info_outline),
            onPressed: () {
              _showInstructions(context);
            },
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Customer info
            Card(
              color: Colors.green.shade50,
              child: Padding(
                padding: EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Customer: $customerName',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    SizedBox(height: 8),
                    Text('Device ID: $deviceId'),
                    Text('Seller ID: $sellerId'),
                  ],
                ),
              ),
            ),
            SizedBox(height: 16),
            
            // QR Code
            Center(
              child: DeviceProvisioningQR(
                deviceId: deviceId,
                sellerId: sellerId,
              ),
            ),
            
            SizedBox(height: 16),
            
            // Action buttons
            ElevatedButton.icon(
              onPressed: () {
                // TODO: Mark device as provisioned in backend
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('Device provisioned successfully!')),
                );
                Navigator.pop(context);
              },
              icon: Icon(Icons.check),
              label: Text('Mark as Provisioned'),
              style: ElevatedButton.styleFrom(
                padding: EdgeInsets.all(16),
              ),
            ),
          ],
        ),
      ),
    );
  }
  
  void _showInstructions(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Provisioning Instructions'),
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              _buildStep('1', 'Factory reset the device'),
              _buildStep('2', 'Power on and go through initial setup'),
              _buildStep('3', 'On welcome screen, tap 6 times quickly'),
              _buildStep('4', 'QR scanner should appear'),
              _buildStep('5', 'Scan the QR code shown above'),
              _buildStep('6', 'Device will download and install EMI Locker'),
              _buildStep('7', 'Wait 2-3 minutes for provisioning'),
              _buildStep('8', 'Device will show "EMI Locker provisioned!"'),
              _buildStep('9', 'Hand device to customer'),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text('Got it!'),
          ),
        ],
      ),
    );
  }
  
  Widget _buildStep(String number, String text) {
    return Padding(
      padding: EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 24,
            height: 24,
            decoration: BoxDecoration(
              color: Colors.blue,
              shape: BoxShape.circle,
            ),
            child: Center(
              child: Text(
                number,
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ),
          SizedBox(width: 8),
          Expanded(child: Text(text)),
        ],
      ),
    );
  }
}
```

---

## ✅ Step 5: Integrate into Your Existing Flow

In your existing customer/device creation screen, add a button to show QR code:

**Example integration:**

```dart
// In your device assignment or customer details screen

ElevatedButton.icon(
  onPressed: () {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => DeviceQRScreen(
          deviceId: 52,  // Replace with actual device ID from your backend
          sellerId: 191, // Replace with actual seller ID
          customerName: 'John Doe', // Replace with customer name
        ),
      ),
    );
  },
  icon: Icon(Icons.qr_code),
  label: Text('Generate Provisioning QR'),
)
```

---

## ✅ Step 6: Update the APK Checksum

After running `get_apk_checksum.py` in Android project:

1. Copy the formatted checksum (the one with colons)
2. Open `lib/helpers/qr_provisioning_helper.dart` in Flutter
3. Replace the `APK_CHECKSUM` value with your actual checksum
4. Save the file

**Example:**
```dart
static const String APK_CHECKSUM = "3F:A1:B2:C3:D4:E5:F6:78:90:..."; // Your actual checksum
```

---

## ✅ FLUTTER APP IS NOW COMPLETE!

---

# 🧪 TESTING THE COMPLETE FLOW

## When you get a test device:

### 1. **Factory Reset Device**
- Settings → System → Reset → Factory data reset

### 2. **Start Setup Wizard**
- Power on device
- Go through initial screens

### 3. **Trigger QR Scanner**
- On welcome screen, **tap 6 times** quickly on the welcome text
- QR scanner should appear

### 4. **Scan QR Code**
- Open your Flutter Admin App
- Navigate to device assignment
- Click "Generate Provisioning QR"
- Scan the displayed QR code

### 5. **Watch Provisioning**
You should see:
- ✅ "Downloading EMI Locker..."
- ✅ "Installing..."
- ✅ "Setting up device owner..."
- ✅ "EMI Locker provisioned successfully!"

### 6. **Verify Success**
Check with ADB:
```bash
adb shell dpm list-owners
# Should show: com.example.emilockerclient

adb logcat | grep -E "DeviceProvisioning|EmiAdminReceiver"
# Should show provisioning logs
```

---

# 📋 QUICK REFERENCE CHECKLIST

## Android DPC App:
- [ ] Build signed APK (`./gradlew assembleRelease`)
- [ ] Calculate checksum (`python3 get_apk_checksum.py`)
- [ ] Upload APK to server
- [ ] Save the checksum value

## Flutter Admin App:
- [ ] Add `qr_flutter` package
- [ ] Create `qr_provisioning_helper.dart`
- [ ] Create `device_provisioning_qr.dart` widget
- [ ] Create `device_qr_screen.dart` screen
- [ ] Update checksum in helper file
- [ ] Integrate QR button in your UI

## Testing:
- [ ] Factory reset test device
- [ ] Scan QR code
- [ ] Verify Device Owner status
- [ ] Test remote commands via FCM

---

# 🎯 WHAT HAPPENS AFTER PROVISIONING?

Once device is provisioned:

1. ✅ App becomes Device Owner
2. ✅ All permissions auto-granted
3. ✅ Firebase initialized, FCM token generated
4. ✅ Device registers with backend (device_id: 52, seller_id: 191)
5. ✅ Location tracking starts (every 1 hour)
6. ✅ Offline monitoring starts
7. ✅ Seller can now send commands via Admin App:
   - Lock/unlock device
   - Disable camera, Bluetooth, settings
   - Show custom messages
   - Request location
   - Remote wipe

---

# 🔄 UPDATING THE APP LATER

When you make changes to Android app:

1. Make your code changes
2. Build new APK: `./gradlew assembleRelease`
3. Calculate NEW checksum: `python3 get_apk_checksum.py`
4. Upload new APK to server (overwrite old one)
5. Update checksum in Flutter app's `qr_provisioning_helper.dart`
6. Generate new QR codes for future devices

**⚠️ Old QR codes won't work with new APK!**

---

# 💡 PRODUCTION TIPS

1. **Use Production Keystore:**
   - Create proper keystore: `keytool -genkey -v -keystore emi-release.jks ...`
   - Update `app/build.gradle.kts` with keystore path
   - Keep keystore file SECURE!

2. **Remove testOnly flag:**
   - Open `AndroidManifest.xml`
   - Remove `android:testOnly="true"` line

3. **Backend Integration:**
   - Implement device registration API call in MainActivity
   - Send device_id, seller_id, FCM token, IMEI to backend
   - Store in database for command routing

4. **QR Code Storage:**
   - Option A: Generate QR on-demand (current approach)
   - Option B: Pre-generate and store QR images in backend
   - Option C: Print QR codes for quick provisioning

---

# ❓ TROUBLESHOOTING

**Problem:** APK build fails
- **Solution:** Run `./gradlew clean` then try again

**Problem:** QR scanner doesn't appear
- **Solution:** Try tapping in different areas, or tap 6 times faster

**Problem:** "Package signature mismatch"
- **Solution:** Checksum doesn't match APK. Recalculate and update

**Problem:** Device doesn't download APK
- **Solution:** Check APK URL is accessible: `curl -I https://api.imelocker.com/downloads/emi-locker-client.apk`

**Problem:** App not Device Owner after provisioning
- **Solution:** Check logs: `adb logcat | grep EmiAdminReceiver`

---

**🎉 YOU'RE ALL SET! Both apps are ready for QR provisioning!**

Good luck vai! 🚀

