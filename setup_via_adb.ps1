# EMI Locker Client - Device Owner Setup via ADB
# This script sets the app as Device Owner without using QR code provisioning

Write-Host "📱 EMI Locker Client - Device Owner Setup" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Check if device is connected
Write-Host "Checking for connected device..." -ForegroundColor Yellow
$device = adb devices | Select-String "device$"
if (-not $device) {
    Write-Host "❌ No device connected!" -ForegroundColor Red
    Write-Host "Please connect your device via USB and enable USB debugging." -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Device connected!" -ForegroundColor Green
Write-Host ""

# Get device ID and seller ID
Write-Host "Enter device details:" -ForegroundColor Cyan
$deviceId = Read-Host "Device ID"
$sellerId = Read-Host "Seller ID"

Write-Host ""
Write-Host "Installing APK..." -ForegroundColor Yellow
$apkPath = "app\build\outputs\apk\release\app-release.apk"

# Check if APK exists
if (-not (Test-Path $apkPath)) {
    Write-Host "❌ APK not found at: $apkPath" -ForegroundColor Red
    Write-Host "Run: .\gradlew.bat assembleRelease first" -ForegroundColor Yellow
    exit 1
}

# Install APK
adb install -r $apkPath
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to install APK!" -ForegroundColor Red
    exit 1
}

Write-Host "✅ APK installed successfully!" -ForegroundColor Green
Write-Host ""

# Set Device Owner
Write-Host "Setting app as Device Owner..." -ForegroundColor Yellow
adb shell dpm set-device-owner com.example.emilockerclient/.admin.EmiAdminReceiver

# Verify
Write-Host ""
Write-Host "Verifying Device Owner status..." -ForegroundColor Yellow
adb shell dpm list-owners

Write-Host ""
Write-Host "✅ Device Owner setup complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Device ID: $deviceId" -ForegroundColor Cyan
Write-Host "Seller ID: $sellerId" -ForegroundColor Cyan
Write-Host ""
Write-Host "The app will launch with these credentials automatically." -ForegroundColor Green

