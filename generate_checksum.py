#!/usr/bin/env python3
import base64
import subprocess
import sys
import re

def get_sha256_fingerprint(apk_path):
    """Extract SHA-256 fingerprint from APK signing certificate."""
    try:
        result = subprocess.run(
            ["keytool", "-printcert", "-jarfile", apk_path],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            check=True
        )
        # More robust regex — handles tabs, spaces, and case variations
        match = re.search(r"SHA[-\s]?256:\s*([A-Fa-f0-9:]+)", result.stdout)
        if not match:
            print("❌ Could not find SHA-256 fingerprint in keytool output.")
            print("🔍 keytool output:\n", result.stdout)
            sys.exit(1)
        return match.group(1).replace(":", "").lower()
    except subprocess.CalledProcessError as e:
        print("❌ Error running keytool. Make sure it's in PATH.")
        print(e.stderr)
        sys.exit(1)

def fingerprint_to_base64url(hex_str):
    raw_bytes = bytes.fromhex(hex_str)
    encoded = base64.urlsafe_b64encode(raw_bytes).decode().rstrip("=")
    return encoded

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 app.py app-release.apk")
        sys.exit(1)

    apk_path = sys.argv[1]
    print(f"📦 Reading APK: {apk_path}\n")

    fingerprint = get_sha256_fingerprint(apk_path)
    print(f"🔐 SHA-256 Fingerprint (hex): {fingerprint}\n")

    checksum = fingerprint_to_base64url(fingerprint)
    print("✅ Final Provisioning Checksum (use this in your QR JSON):")
    print(f"   {checksum}\n")

if __name__ == "__main__":
    main()

