#!/bin/bash

# Script to build APK and get SHA-256 checksum for QR provisioning
# Usage: ./get_apk_checksum.sh

set -e

echo "🔨 Building release APK..."
./gradlew assembleRelease

APK_PATH="app/build/outputs/apk/release/app-release.apk"

if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK not found at $APK_PATH"
    echo "Make sure your APK is signed. Check app/build.gradle.kts for signing config."
    exit 1
fi

echo ""
echo "✅ APK built successfully!"
echo "📍 Location: $APK_PATH"
echo ""
echo "🔐 Calculating SHA-256 checksum..."

# Get SHA-256 hash
HASH=$(sha256sum "$APK_PATH" | awk '{print $1}')

echo "Raw SHA-256: $HASH"
echo ""

# Format for Android provisioning (uppercase with colons)
FORMATTED=$(echo "$HASH" | tr '[:lower:]' '[:upper:]' | sed 's/../&:/g;s/:$//')

echo "📋 Formatted for QR JSON:"
echo "$FORMATTED"
echo ""

# Create QR JSON template
cat > qr_provisioning.json << EOF
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.example.emilockerclient/.admin.EmiAdminReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://api.imelocker.com/downloads/emi-locker-client.apk",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM": "$FORMATTED",
  "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": false,
  "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
  "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
    "device_id": "52",
    "seller_id": "191"
  }
}
EOF

echo "✅ QR JSON created: qr_provisioning.json"
echo ""
echo "📱 Next steps:"
echo "1. Upload $APK_PATH to https://api.imelocker.com/downloads/emi-locker-client.apk"
echo "2. Use qr_provisioning.json to generate QR code in your Admin App"
echo "3. Update device_id and seller_id in the JSON for each device"
echo ""

