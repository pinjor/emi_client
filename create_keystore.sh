#!/bin/bash

# Script to create production keystore for EMI Locker app
# Run this once to create your production signing key

echo "🔐 Creating Production Keystore for EMI Locker"
echo ""

KEYSTORE_FILE="emi-locker-release.jks"
KEY_ALIAS="emi-locker-key"
VALIDITY_DAYS=10000  # ~27 years

# Check if keystore already exists
if [ -f "$KEYSTORE_FILE" ]; then
    echo "⚠️  Keystore already exists: $KEYSTORE_FILE"
    read -p "Do you want to overwrite it? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ Cancelled. Using existing keystore."
        exit 0
    fi
    rm "$KEYSTORE_FILE"
fi

echo "Creating new keystore..."
echo ""
echo "You'll be asked for:"
echo "  1. Keystore password (choose strong password!)"
echo "  2. Your name / organization details"
echo "  3. Key password (can be same as keystore password)"
echo ""
echo "⚠️  IMPORTANT: Save the passwords securely - you can't recover them!"
echo ""

keytool -genkey -v \
    -keystore "$KEYSTORE_FILE" \
    -keyalg RSA \
    -keysize 2048 \
    -validity $VALIDITY_DAYS \
    -alias "$KEY_ALIAS"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Keystore created successfully: $KEYSTORE_FILE"
    echo ""
    echo "📝 Next steps:"
    echo "1. Save this file securely (backup it!)"
    echo "2. Remember your passwords"
    echo "3. Update app/build.gradle.kts with keystore details"
    echo ""
    echo "🔒 Keystore details:"
    echo "   File: $KEYSTORE_FILE"
    echo "   Alias: $KEY_ALIAS"
    echo "   Location: $(pwd)/$KEYSTORE_FILE"
else
    echo ""
    echo "❌ Failed to create keystore"
    exit 1
fi

