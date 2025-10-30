#!/bin/bash
# ============================================
# GIGAX Y10 - ADB Provisioning Script
# ============================================
# This script provisions GIGAX Y10 devices that block QR code provisioning
# Works on Linux/Mac with ADB installed
#
# Prerequisites:
# 1. ADB installed and in PATH
# 2. Device factory reset
# 3. USB Debugging enabled on device
# 4. Device connected via USB
#
# Usage: chmod +x setup_gigax_y10.sh && ./setup_gigax_y10.sh
# ============================================

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
APK_PATH="app/build/outputs/apk/release/app-release.apk"
PACKAGE_NAME="com.example.emilockerclient"
ADMIN_RECEIVER="com.example.emilockerclient/.admin.EmiAdminReceiver"

echo -e "${CYAN}============================================${NC}"
echo -e "${CYAN}  GIGAX Y10 - ADB Provisioning Script${NC}"
echo -e "${CYAN}  EMI Locker Client Setup${NC}"
echo -e "${CYAN}============================================${NC}"
echo ""

# Check if ADB is installed
echo -e "${YELLOW}[1/7] Checking ADB installation...${NC}"
if ! command -v adb &> /dev/null; then
    echo -e "${RED}❌ ERROR: ADB not found!${NC}"
    echo -e "${RED}Please install Android SDK Platform Tools:${NC}"
    echo -e "${RED}https://developer.android.com/studio/releases/platform-tools${NC}"
    exit 1
fi
echo -e "${GREEN}✅ ADB found: $(which adb)${NC}"
echo ""

# Check if APK exists
echo -e "${YELLOW}[2/7] Checking APK file...${NC}"
if [ ! -f "$APK_PATH" ]; then
    echo -e "${RED}❌ ERROR: APK not found at: $APK_PATH${NC}"
    echo -e "${RED}Please build the APK first:${NC}"
    echo -e "${RED}  ./gradlew assembleRelease${NC}"
    exit 1
fi
APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
echo -e "${GREEN}✅ APK found: $APK_PATH ($APK_SIZE)${NC}"
echo ""

# Check device connection
echo -e "${YELLOW}[3/7] Checking device connection...${NC}"
DEVICE_COUNT=$(adb devices | grep -w "device" | wc -l)
if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo -e "${RED}❌ ERROR: No device connected!${NC}"
    echo ""
    echo -e "${YELLOW}Please ensure:${NC}"
    echo -e "${YELLOW}  1. Device is connected via USB${NC}"
    echo -e "${YELLOW}  2. USB Debugging is enabled${NC}"
    echo -e "${YELLOW}  3. You've authorized the computer on the device${NC}"
    echo ""
    echo -e "${CYAN}To enable USB Debugging:${NC}"
    echo -e "${CYAN}  Settings → About phone → Tap 'Build number' 7 times${NC}"
    echo -e "${CYAN}  Settings → Developer options → Enable 'USB debugging'${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Device connected${NC}"
echo ""

# Get device info
echo -e "${YELLOW}[4/7] Getting device information...${NC}"
DEVICE_MODEL=$(adb shell getprop ro.product.model | tr -d '\r')
ANDROID_VERSION=$(adb shell getprop ro.build.version.release | tr -d '\r')
SERIAL_NUMBER=$(adb shell getprop ro.serialno | tr -d '\r')
echo -e "${CYAN}   Model: $DEVICE_MODEL${NC}"
echo -e "${CYAN}   Android: $ANDROID_VERSION${NC}"
echo -e "${CYAN}   Serial: $SERIAL_NUMBER${NC}"
echo ""

# Check if already Device Owner
echo -e "${YELLOW}[5/7] Checking Device Owner status...${NC}"
CURRENT_OWNER=$(adb shell dpm list-owners 2>&1)
if echo "$CURRENT_OWNER" | grep -q "$PACKAGE_NAME"; then
    echo -e "${YELLOW}⚠️  WARNING: App is already Device Owner!${NC}"
    echo -e "${YELLOW}   Current owner: $CURRENT_OWNER${NC}"
    echo ""
    read -p "Continue anyway? (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${YELLOW}Aborted by user.${NC}"
        exit 0
    fi
else
    echo -e "${GREEN}✅ No Device Owner set${NC}"
fi
echo ""

# Install APK
echo -e "${YELLOW}[6/7] Installing APK...${NC}"
echo -e "${CYAN}   This may take 30-60 seconds...${NC}"
INSTALL_RESULT=$(adb install -r "$APK_PATH" 2>&1)
if echo "$INSTALL_RESULT" | grep -q "Success"; then
    echo -e "${GREEN}✅ APK installed successfully${NC}"
else
    echo -e "${RED}❌ ERROR: APK installation failed!${NC}"
    echo -e "${RED}   Error: $INSTALL_RESULT${NC}"
    echo ""
    echo -e "${YELLOW}Common causes:${NC}"
    echo -e "${YELLOW}  1. App is already installed - try uninstalling first${NC}"
    echo -e "${YELLOW}  2. Insufficient storage space${NC}"
    echo -e "${YELLOW}  3. APK signature mismatch${NC}"
    exit 1
fi
echo ""

# Set Device Owner
echo -e "${YELLOW}[7/7] Setting Device Owner...${NC}"
echo -e "${CYAN}   This is the critical step...${NC}"
SET_OWNER_RESULT=$(adb shell dpm set-device-owner "$ADMIN_RECEIVER" 2>&1)

if echo "$SET_OWNER_RESULT" | grep -q "Success"; then
    echo -e "${GREEN}✅ Device Owner set successfully!${NC}"
    echo ""
    echo -e "${GREEN}============================================${NC}"
    echo -e "${GREEN}  ✅ PROVISIONING COMPLETED SUCCESSFULLY!${NC}"
    echo -e "${GREEN}============================================${NC}"
    echo ""
    
    # Verify
    echo -e "${YELLOW}Verifying Device Owner status...${NC}"
    VERIFY_OWNER=$(adb shell dpm list-owners)
    echo -e "${CYAN}   $VERIFY_OWNER${NC}"
    echo ""
    
    echo -e "${YELLOW}Next steps:${NC}"
    echo -e "${CYAN}  1. Open the EMI Locker app on the device${NC}"
    echo -e "${CYAN}  2. Complete the registration process${NC}"
    echo -e "${CYAN}  3. Grant all required permissions${NC}"
    echo -e "${CYAN}  4. Device is now ready for use!${NC}"
    echo ""
    
elif echo "$SET_OWNER_RESULT" | grep -q "already several users"; then
    echo -e "${RED}❌ ERROR: Multiple users detected!${NC}"
    echo -e "${RED}   Device must have only one user (owner) for Device Owner setup.${NC}"
    echo ""
    echo -e "${YELLOW}Solution:${NC}"
    echo -e "${YELLOW}  1. Factory reset the device${NC}"
    echo -e "${YELLOW}  2. Skip all setup steps (no Google account)${NC}"
    echo -e "${YELLOW}  3. Enable USB Debugging${NC}"
    echo -e "${YELLOW}  4. Run this script again${NC}"
    exit 1
    
elif echo "$SET_OWNER_RESULT" | grep -q "already an owner"; then
    echo -e "${YELLOW}⚠️  WARNING: Device already has an owner!${NC}"
    echo -e "${YELLOW}   Current owner: $SET_OWNER_RESULT${NC}"
    echo ""
    echo -e "${YELLOW}To fix:${NC}"
    echo -e "${YELLOW}  1. Remove current Device Owner:${NC}"
    echo -e "${YELLOW}     adb shell dpm remove-active-admin $ADMIN_RECEIVER${NC}"
    echo -e "${YELLOW}  2. Or factory reset the device${NC}"
    exit 1
    
elif echo "$SET_OWNER_RESULT" | grep -q "Not allowed"; then
    echo -e "${RED}❌ ERROR: Device Owner setup not allowed!${NC}"
    echo -e "${RED}   Error: $SET_OWNER_RESULT${NC}"
    echo ""
    echo -e "${YELLOW}Common causes:${NC}"
    echo -e "${YELLOW}  1. Google account is already added${NC}"
    echo -e "${YELLOW}  2. Device has multiple users${NC}"
    echo -e "${YELLOW}  3. Work profile already exists${NC}"
    echo ""
    echo -e "${YELLOW}Solution:${NC}"
    echo -e "${YELLOW}  1. Factory reset the device${NC}"
    echo -e "${YELLOW}  2. Skip ALL setup steps${NC}"
    echo -e "${YELLOW}  3. Do NOT add any Google account${NC}"
    echo -e "${YELLOW}  4. Enable USB Debugging immediately${NC}"
    echo -e "${YELLOW}  5. Run this script again${NC}"
    exit 1
    
else
    echo -e "${RED}❌ ERROR: Failed to set Device Owner!${NC}"
    echo -e "${RED}   Error: $SET_OWNER_RESULT${NC}"
    echo ""
    echo -e "${YELLOW}Please check the error message above and try again.${NC}"
    exit 1
fi

echo ""
echo -e "${CYAN}============================================${NC}"
echo -e "${CYAN}  Provisioning script completed!${NC}"
echo -e "${CYAN}============================================${NC}"
echo ""
echo -e "${GREEN}Device Serial: $SERIAL_NUMBER${NC}"
echo -e "${GREEN}Status: Device Owner ✅${NC}"
echo ""
