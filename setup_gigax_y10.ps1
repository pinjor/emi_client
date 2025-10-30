# ============================================
# GIGAX Y10 - ADB Provisioning Script
# ============================================
# This script provisions GIGAX Y10 devices that block QR code provisioning
# Works on Windows with ADB installed
#
# Prerequisites:
# 1. ADB installed and in PATH
# 2. Device factory reset
# 3. USB Debugging enabled on device
# 4. Device connected via USB
#
# Usage: .\setup_gigax_y10.ps1
# ============================================

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  GIGAX Y10 - ADB Provisioning Script" -ForegroundColor Cyan
Write-Host "  EMI Locker Client Setup" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$APK_PATH = "app\build\outputs\apk\release\app-release.apk"
$PACKAGE_NAME = "com.example.emilockerclient"
$ADMIN_RECEIVER = "com.example.emilockerclient/.admin.EmiAdminReceiver"

# Check if ADB is installed
Write-Host "[1/7] Checking ADB installation..." -ForegroundColor Yellow
$adbCheck = Get-Command adb -ErrorAction SilentlyContinue
if (-not $adbCheck) {
    Write-Host "❌ ERROR: ADB not found!" -ForegroundColor Red
    Write-Host "Please install Android SDK Platform Tools:" -ForegroundColor Red
    Write-Host "https://developer.android.com/studio/releases/platform-tools" -ForegroundColor Red
    exit 1
}
Write-Host "✅ ADB found: $($adbCheck.Source)" -ForegroundColor Green
Write-Host ""

# Check if APK exists
Write-Host "[2/7] Checking APK file..." -ForegroundColor Yellow
if (-not (Test-Path $APK_PATH)) {
    Write-Host "❌ ERROR: APK not found at: $APK_PATH" -ForegroundColor Red
    Write-Host "Please build the APK first:" -ForegroundColor Red
    Write-Host "  .\gradlew.bat assembleRelease" -ForegroundColor Red
    exit 1
}
$apkSize = (Get-Item $APK_PATH).Length / 1MB
Write-Host "✅ APK found: $APK_PATH ($([math]::Round($apkSize, 2)) MB)" -ForegroundColor Green
Write-Host ""

# Check device connection
Write-Host "[3/7] Checking device connection..." -ForegroundColor Yellow
$devices = adb devices | Select-String "device$"
if ($devices.Count -eq 0) {
    Write-Host "❌ ERROR: No device connected!" -ForegroundColor Red
    Write-Host "" -ForegroundColor Red
    Write-Host "Please ensure:" -ForegroundColor Yellow
    Write-Host "  1. Device is connected via USB" -ForegroundColor Yellow
    Write-Host "  2. USB Debugging is enabled" -ForegroundColor Yellow
    Write-Host "  3. You've authorized the computer on the device" -ForegroundColor Yellow
    Write-Host "" -ForegroundColor Yellow
    Write-Host "To enable USB Debugging:" -ForegroundColor Cyan
    Write-Host "  Settings → About phone → Tap 'Build number' 7 times" -ForegroundColor Cyan
    Write-Host "  Settings → Developer options → Enable 'USB debugging'" -ForegroundColor Cyan
    exit 1
}
Write-Host "✅ Device connected" -ForegroundColor Green
Write-Host ""

# Get device info
Write-Host "[4/7] Getting device information..." -ForegroundColor Yellow
$deviceModel = adb shell getprop ro.product.model
$androidVersion = adb shell getprop ro.build.version.release
$serialNumber = adb shell getprop ro.serialno
Write-Host "   Model: $deviceModel" -ForegroundColor Cyan
Write-Host "   Android: $androidVersion" -ForegroundColor Cyan
Write-Host "   Serial: $serialNumber" -ForegroundColor Cyan
Write-Host ""

# Check if already Device Owner
Write-Host "[5/7] Checking Device Owner status..." -ForegroundColor Yellow
$currentOwner = adb shell dpm list-owners 2>&1
if ($currentOwner -match $PACKAGE_NAME) {
    Write-Host "⚠️  WARNING: App is already Device Owner!" -ForegroundColor Yellow
    Write-Host "   Current owner: $currentOwner" -ForegroundColor Yellow
    Write-Host ""
    $continue = Read-Host "Continue anyway? (y/n)"
    if ($continue -ne "y") {
        Write-Host "Aborted by user." -ForegroundColor Yellow
        exit 0
    }
} else {
    Write-Host "✅ No Device Owner set" -ForegroundColor Green
}
Write-Host ""

# Install APK
Write-Host "[6/7] Installing APK..." -ForegroundColor Yellow
Write-Host "   This may take 30-60 seconds..." -ForegroundColor Cyan
$installResult = adb install -r $APK_PATH 2>&1
if ($installResult -match "Success") {
    Write-Host "✅ APK installed successfully" -ForegroundColor Green
} else {
    Write-Host "❌ ERROR: APK installation failed!" -ForegroundColor Red
    Write-Host "   Error: $installResult" -ForegroundColor Red
    Write-Host "" -ForegroundColor Red
    Write-Host "Common causes:" -ForegroundColor Yellow
    Write-Host "  1. App is already installed - try uninstalling first" -ForegroundColor Yellow
    Write-Host "  2. Insufficient storage space" -ForegroundColor Yellow
    Write-Host "  3. APK signature mismatch" -ForegroundColor Yellow
    exit 1
}
Write-Host ""

# Set Device Owner
Write-Host "[7/7] Setting Device Owner..." -ForegroundColor Yellow
Write-Host "   This is the critical step..." -ForegroundColor Cyan
$setOwnerResult = adb shell dpm set-device-owner $ADMIN_RECEIVER 2>&1

if ($setOwnerResult -match "Success") {
    Write-Host "✅ Device Owner set successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "============================================" -ForegroundColor Green
    Write-Host "  ✅ PROVISIONING COMPLETED SUCCESSFULLY!" -ForegroundColor Green
    Write-Host "============================================" -ForegroundColor Green
    Write-Host ""
    
    # Verify
    Write-Host "Verifying Device Owner status..." -ForegroundColor Yellow
    $verifyOwner = adb shell dpm list-owners
    Write-Host "   $verifyOwner" -ForegroundColor Cyan
    Write-Host ""
    
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "  1. Open the EMI Locker app on the device" -ForegroundColor Cyan
    Write-Host "  2. Complete the registration process" -ForegroundColor Cyan
    Write-Host "  3. Grant all required permissions" -ForegroundColor Cyan
    Write-Host "  4. Device is now ready for use!" -ForegroundColor Cyan
    Write-Host ""
    
} elseif ($setOwnerResult -match "already several users") {
    Write-Host "❌ ERROR: Multiple users detected!" -ForegroundColor Red
    Write-Host "   Device must have only one user (owner) for Device Owner setup." -ForegroundColor Red
    Write-Host "" -ForegroundColor Red
    Write-Host "Solution:" -ForegroundColor Yellow
    Write-Host "  1. Factory reset the device" -ForegroundColor Yellow
    Write-Host "  2. Skip all setup steps (no Google account)" -ForegroundColor Yellow
    Write-Host "  3. Enable USB Debugging" -ForegroundColor Yellow
    Write-Host "  4. Run this script again" -ForegroundColor Yellow
    exit 1
    
} elseif ($setOwnerResult -match "already an owner") {
    Write-Host "⚠️  WARNING: Device already has an owner!" -ForegroundColor Yellow
    Write-Host "   Current owner: $setOwnerResult" -ForegroundColor Yellow
    Write-Host "" -ForegroundColor Yellow
    Write-Host "To fix:" -ForegroundColor Yellow
    Write-Host "  1. Remove current Device Owner:" -ForegroundColor Yellow
    Write-Host "     adb shell dpm remove-active-admin $ADMIN_RECEIVER" -ForegroundColor Yellow
    Write-Host "  2. Or factory reset the device" -ForegroundColor Yellow
    exit 1
    
} elseif ($setOwnerResult -match "Not allowed") {
    Write-Host "❌ ERROR: Device Owner setup not allowed!" -ForegroundColor Red
    Write-Host "   Error: $setOwnerResult" -ForegroundColor Red
    Write-Host "" -ForegroundColor Red
    Write-Host "Common causes:" -ForegroundColor Yellow
    Write-Host "  1. Google account is already added" -ForegroundColor Yellow
    Write-Host "  2. Device has multiple users" -ForegroundColor Yellow
    Write-Host "  3. Work profile already exists" -ForegroundColor Yellow
    Write-Host "" -ForegroundColor Yellow
    Write-Host "Solution:" -ForegroundColor Yellow
    Write-Host "  1. Factory reset the device" -ForegroundColor Yellow
    Write-Host "  2. Skip ALL setup steps" -ForegroundColor Yellow
    Write-Host "  3. Do NOT add any Google account" -ForegroundColor Yellow
    Write-Host "  4. Enable USB Debugging immediately" -ForegroundColor Yellow
    Write-Host "  5. Run this script again" -ForegroundColor Yellow
    exit 1
    
} else {
    Write-Host "❌ ERROR: Failed to set Device Owner!" -ForegroundColor Red
    Write-Host "   Error: $setOwnerResult" -ForegroundColor Red
    Write-Host "" -ForegroundColor Red
    Write-Host "Please check the error message above and try again." -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Provisioning script completed!" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Device Serial: $serialNumber" -ForegroundColor Green
Write-Host "Status: Device Owner ✅" -ForegroundColor Green
Write-Host ""
