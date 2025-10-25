#!/usr/bin/env python3
"""
Script to calculate SHA-256 checksum for APK and generate QR provisioning JSON
Usage: python3 get_apk_checksum.py [path_to_apk]
"""

import hashlib
import json
import sys
import os

def calculate_sha256(file_path):
    """Calculate SHA-256 hash of a file"""
    sha256_hash = hashlib.sha256()

    try:
        with open(file_path, "rb") as f:
            # Read file in chunks to handle large APKs
            for byte_block in iter(lambda: f.read(4096), b""):
                sha256_hash.update(byte_block)
        return sha256_hash.hexdigest()
    except FileNotFoundError:
        print(f"❌ Error: File not found: {file_path}")
        sys.exit(1)
    except Exception as e:
        print(f"❌ Error reading file: {e}")
        sys.exit(1)

def format_checksum(checksum):
    """Format checksum for Android provisioning (uppercase with colons)"""
    checksum = checksum.upper()
    return ':'.join([checksum[i:i+2] for i in range(0, len(checksum), 2)])

def generate_qr_json(formatted_checksum, device_id="52", seller_id="191"):
    """Generate QR provisioning JSON"""
    qr_data = {
        "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME":
            "com.example.emilockerclient/.admin.EmiAdminReceiver",
        "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION":
            "https://api.imelocker.com/downloads/emi-locker-client.apk",
        "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM":
            formatted_checksum,
        "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": False,
        "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": True,
        "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
            "device_id": device_id,
            "seller_id": seller_id
        }
    }
    return qr_data

def main():
    # Default APK path
    default_apk = "app/build/outputs/apk/release/app-release.apk"

    if len(sys.argv) > 1:
        apk_path = sys.argv[1]
    else:
        apk_path = default_apk

    # Check if APK exists
    if not os.path.exists(apk_path):
        print(f"❌ APK not found at: {apk_path}")
        print(f"\n📝 Please build your release APK first:")
        print(f"   ./gradlew assembleRelease")
        print(f"\n💡 Or specify APK path:")
        print(f"   python3 get_apk_checksum.py /path/to/your.apk")
        sys.exit(1)

    print(f"📱 APK found: {apk_path}")
    print(f"📏 File size: {os.path.getsize(apk_path) / (1024*1024):.2f} MB")
    print(f"\n🔐 Calculating SHA-256 checksum...")

    # Calculate checksum
    checksum = calculate_sha256(apk_path)
    formatted = format_checksum(checksum)

    print(f"\n✅ Raw SHA-256:")
    print(f"   {checksum}")
    print(f"\n📋 Formatted for Android (use this in QR):")
    print(f"   {formatted}")

    # Generate QR JSON
    qr_json = generate_qr_json(formatted)

    # Save to file
    output_file = "qr_provisioning.json"
    with open(output_file, 'w') as f:
        json.dump(qr_json, f, indent=2)

    print(f"\n✅ QR JSON saved to: {output_file}")
    print(f"\n📄 QR JSON content:")
    print(json.dumps(qr_json, indent=2))

    print(f"\n🎯 Next steps:")
    print(f"1. Upload {apk_path} to:")
    print(f"   https://api.imelocker.com/downloads/emi-locker-client.apk")
    print(f"2. Use {output_file} in your Flutter Admin App to generate QR codes")
    print(f"3. Update device_id and seller_id for each unique device")
    print(f"\n💡 Pro tip: You can also copy just the formatted checksum above")

if __name__ == "__main__":
    main()

