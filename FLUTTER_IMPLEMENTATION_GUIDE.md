# 🦋 Flutter Admin App - QR Provisioning Implementation

## ✅ Step 1: Add QR Package to pubspec.yaml

Add this to your Flutter project's `pubspec.yaml`:

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

## ✅ Step 2: Create QR Provisioning Helper

Create file: `lib/helpers/qr_provisioning_helper.dart`

```dart
import 'dart:convert';

class QRProvisioningHelper {
  // 🔐 APK SHA-256 Checksum (from your get_apk_checksum.py output)
  static const String APK_CHECKSUM = 
      "6C:A7:1C:F9:7B:57:2D:6D:01:65:66:3E:0F:F2:8F:96:47:8C:48:32:B2:5C:C4:A7:F3:FE:0C:59:C9:F7:48:7E";
  
  static const String APK_DOWNLOAD_URL = 
      "https://api.imelocker.com/downloads/emi-locker-client.apk";
  
  static const String ADMIN_COMPONENT_NAME = 
      "com.example.emilockerclient/.admin.EmiAdminReceiver";
  
  /// Generate QR code provisioning JSON string
  /// 
  /// [sellerId] - ID of the seller provisioning the device
  /// 
  /// Returns JSON string ready to be encoded in QR code
  static String generateProvisioningQR(int sellerId) {
    final provisioningData = {
      "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": 
          ADMIN_COMPONENT_NAME,
      "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": 
          APK_DOWNLOAD_URL,
      "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM": 
          APK_CHECKSUM,
      "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": false,
      "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
      "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
        "seller_id": sellerId.toString(),
      }
    };
    
    return jsonEncode(provisioningData);
  }
  
  /// Generate universal QR (no seller info)
  /// Use if you want one QR for all sellers
  static String generateUniversalQR() {
    final provisioningData = {
      "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": 
          ADMIN_COMPONENT_NAME,
      "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": 
          APK_DOWNLOAD_URL,
      "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM": 
          APK_CHECKSUM,
      "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": false,
      "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
    };
    
    return jsonEncode(provisioningData);
  }
}
```

---

## ✅ Step 3: Create QR Display Widget

Create file: `lib/widgets/device_provisioning_qr.dart`

```dart
import 'package:flutter/material.dart';
import 'package:qr_flutter/qr_flutter.dart';
import '../helpers/qr_provisioning_helper.dart';

class DeviceProvisioningQR extends StatelessWidget {
  final int sellerId;
  final bool showInstructions;
  
  const DeviceProvisioningQR({
    Key? key,
    required this.sellerId,
    this.showInstructions = true,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final qrData = QRProvisioningHelper.generateProvisioningQR(sellerId);
    
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              'Device Provisioning QR Code',
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                fontWeight: FontWeight.bold,
              ),
            ),
            SizedBox(height: 8),
            Container(
              padding: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              decoration: BoxDecoration(
                color: Colors.blue.shade50,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(
                'Seller ID: $sellerId',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: Colors.blue.shade900,
                ),
              ),
            ),
            SizedBox(height: 16),
            
            // QR Code
            Container(
              padding: EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: Colors.grey.shade300, width: 2),
              ),
              child: QrImageView(
                data: qrData,
                version: QrVersions.auto,
                size: 280.0,
                backgroundColor: Colors.white,
                errorCorrectionLevel: QrErrorCorrectLevel.M,
              ),
            ),
            
            if (showInstructions) ...[
              SizedBox(height: 16),
              Container(
                padding: EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.green.shade50,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.green.shade200),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Icon(Icons.info_outline, color: Colors.green.shade700, size: 20),
                        SizedBox(width: 8),
                        Text(
                          'How to Use:',
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            color: Colors.green.shade900,
                          ),
                        ),
                      ],
                    ),
                    SizedBox(height: 8),
                    _buildInstruction('1', 'Factory reset the device'),
                    _buildInstruction('2', 'On welcome screen, tap 6 times'),
                    _buildInstruction('3', 'Scan this QR code'),
                    _buildInstruction('4', 'Wait 2-3 minutes'),
                    _buildInstruction('5', 'Device will show "Provisioned!"'),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
  
  Widget _buildInstruction(String number, String text) {
    return Padding(
      padding: EdgeInsets.only(bottom: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 20,
            height: 20,
            decoration: BoxDecoration(
              color: Colors.green.shade700,
              shape: BoxShape.circle,
            ),
            child: Center(
              child: Text(
                number,
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 11,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ),
          SizedBox(width: 8),
          Expanded(
            child: Text(
              text,
              style: TextStyle(fontSize: 13),
            ),
          ),
        ],
      ),
    );
  }
}
```

---

## ✅ Step 4: Create Full QR Screen (Optional but Recommended)

Create file: `lib/screens/provisioning_qr_screen.dart`

```dart
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../widgets/device_provisioning_qr.dart';
import '../helpers/qr_provisioning_helper.dart';

class ProvisioningQRScreen extends StatelessWidget {
  final int sellerId;
  final String sellerName;
  
  const ProvisioningQRScreen({
    Key? key,
    required this.sellerId,
    required this.sellerName,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Device Provisioning'),
        actions: [
          IconButton(
            icon: Icon(Icons.copy),
            tooltip: 'Copy QR Data',
            onPressed: () => _copyQRData(context),
          ),
          IconButton(
            icon: Icon(Icons.help_outline),
            tooltip: 'Help',
            onPressed: () => _showDetailedInstructions(context),
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Seller info card
            Card(
              color: Colors.blue.shade50,
              child: Padding(
                padding: EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        CircleAvatar(
                          backgroundColor: Colors.blue,
                          child: Icon(Icons.person, color: Colors.white),
                        ),
                        SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                sellerName,
                                style: TextStyle(
                                  fontSize: 18,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              Text(
                                'Seller ID: $sellerId',
                                style: TextStyle(
                                  fontSize: 14,
                                  color: Colors.grey.shade700,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    SizedBox(height: 12),
                    Container(
                      padding: EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Row(
                        children: [
                          Icon(Icons.qr_code, size: 20, color: Colors.blue),
                          SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              'Use this QR code to provision multiple devices',
                              style: TextStyle(fontSize: 13),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
            
            SizedBox(height: 16),
            
            // QR Code
            Center(
              child: DeviceProvisioningQR(
                sellerId: sellerId,
                showInstructions: true,
              ),
            ),
            
            SizedBox(height: 24),
            
            // Action buttons
            ElevatedButton.icon(
              onPressed: () {
                _showDetailedInstructions(context);
              },
              icon: Icon(Icons.help),
              label: Text('View Detailed Instructions'),
              style: ElevatedButton.styleFrom(
                padding: EdgeInsets.all(16),
              ),
            ),
            
            SizedBox(height: 12),
            
            OutlinedButton.icon(
              onPressed: () {
                _copyQRData(context);
              },
              icon: Icon(Icons.copy),
              label: Text('Copy QR Data (Advanced)'),
              style: OutlinedButton.styleFrom(
                padding: EdgeInsets.all(16),
              ),
            ),
          ],
        ),
      ),
    );
  }
  
  void _copyQRData(BuildContext context) {
    final qrData = QRProvisioningHelper.generateProvisioningQR(sellerId);
    Clipboard.setData(ClipboardData(text: qrData));
    
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('QR data copied to clipboard!'),
        backgroundColor: Colors.green,
      ),
    );
  }
  
  void _showDetailedInstructions(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Provisioning Instructions'),
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                'Complete Step-by-Step Guide:',
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
              ),
              SizedBox(height: 12),
              _buildDetailedStep('1', 'Prepare Device', 
                'Factory reset the device:\n'
                'Settings → System → Reset → Factory data reset'),
              _buildDetailedStep('2', 'Start Setup', 
                'Power on and go through initial setup screens\n'
                'Connect to WiFi if needed'),
              _buildDetailedStep('3', 'Open QR Scanner', 
                'On the welcome screen, tap 6 times quickly\n'
                'QR scanner should appear'),
              _buildDetailedStep('4', 'Scan QR Code', 
                'Point camera at the QR code above\n'
                'Device will start downloading EMI Locker'),
              _buildDetailedStep('5', 'Wait for Provisioning', 
                'Download: ~1 minute\n'
                'Installation: ~30 seconds\n'
                'Setup: ~1 minute\n'
                'Total: 2-3 minutes'),
              _buildDetailedStep('6', 'Verify Success', 
                'Device will show:\n'
                '"✅ EMI Locker provisioned successfully!"\n'
                'Serial number and seller ID will be displayed'),
              _buildDetailedStep('7', 'Hand Over', 
                'Device is now ready for customer\n'
                'EMI Locker is active and monitoring'),
              SizedBox(height: 16),
              Container(
                padding: EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.orange.shade50,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Row(
                  children: [
                    Icon(Icons.warning_amber, color: Colors.orange),
                    SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        'Note: Device must be factory reset. '
                        'Provisioning won\'t work on already-setup devices.',
                        style: TextStyle(fontSize: 12),
                      ),
                    ),
                  ],
                ),
              ),
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
  
  Widget _buildDetailedStep(String number, String title, String description) {
    return Padding(
      padding: EdgeInsets.only(bottom: 16),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 28,
            height: 28,
            decoration: BoxDecoration(
              color: Colors.blue,
              shape: BoxShape.circle,
            ),
            child: Center(
              child: Text(
                number,
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ),
          SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 14,
                  ),
                ),
                SizedBox(height: 4),
                Text(
                  description,
                  style: TextStyle(fontSize: 12, color: Colors.grey.shade700),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
```

---

## ✅ Step 5: Integration Example

In your existing seller management or device assignment screen, add:

```dart
// Example: In your seller details or device management screen

ElevatedButton.icon(
  onPressed: () {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => ProvisioningQRScreen(
          sellerId: currentSeller.id,  // e.g., 191
          sellerName: currentSeller.name,  // e.g., "Ahmad"
        ),
      ),
    );
  },
  icon: Icon(Icons.qr_code_2),
  label: Text('Show Provisioning QR'),
)
```

Or for a simple inline QR:

```dart
// Simple inline usage
DeviceProvisioningQR(
  sellerId: 191,
  showInstructions: false,
)
```

---

## 🎯 Quick Start Checklist:

- [ ] Add `qr_flutter: ^4.1.0` to pubspec.yaml
- [ ] Run `flutter pub get`
- [ ] Create `lib/helpers/qr_provisioning_helper.dart`
- [ ] Create `lib/widgets/device_provisioning_qr.dart`
- [ ] Create `lib/screens/provisioning_qr_screen.dart` (optional)
- [ ] Add button to show QR in your UI
- [ ] Test generating QR code
- [ ] Test scanning on factory-reset device

---

## 🔄 Updating APK Later:

When you update Android app:

1. Build new APK: `./gradlew assembleRelease`
2. Get new checksum: `python3 get_apk_checksum.py`
3. Update `APK_CHECKSUM` in `qr_provisioning_helper.dart`
4. Upload new APK to server
5. Generate new QR codes

---

## 📱 Testing Flow:

1. Open Flutter Admin App
2. Navigate to seller/device section
3. Click "Show Provisioning QR"
4. Factory reset a test device
5. Tap 6 times on welcome screen
6. Scan displayed QR code
7. Watch device provision automatically!

---

**That's it! Your Flutter Admin App is ready for QR provisioning!** 🚀

