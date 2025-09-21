#!/bin/bash
PKG="com.example.emilockerclient"
ADMIN=".admin.EmiAdminReceiver"
APK="./app-debug.apk"   # adjust path if needed

case "$1" in
  install)
    # ❯ adb install -t -r app/build/outputs/apk/debug/app-debug.apk
    echo "📦 Installing debug APK..."
    ./gradlew AssembleDebug
    adb install -r -t "$APK"
    ;;
  owner)
    # ❯ adb shell dpm set-device-owner com.example.emilockerclient/.admin.EmiAdminReceiver
    echo "🔑 Setting device owner..."
    adb shell dpm set-device-owner $PKG/$ADMIN
    adb shell dumpsys device_policy | grep "Device Owner"
    ;;
  remove-owner)
    echo "🚫 Removing device owner..."
    adb shell dpm remove-active-admin $PKG/$ADMIN
    ;;
  uninstall)
    # ❯ adb uninstall com.example.emilockerclient
    echo "🗑️ Uninstalling app..."
    adb uninstall $PKG
    ;;
  cycle)
    # ❯ adb shell dpm remove-active-admin com.example.emilockerclient/.admin.EmiAdminReceiver
    echo "♻️ Full cycle: remove owner → uninstall → install → set owner"
    adb shell dpm remove-active-admin $PKG/$ADMIN || true
    adb uninstall $PKG || true
    adb install -r -t "$APK"
    adb shell dpm set-device-owner $PKG/$ADMIN
    ;;
  *)
    echo "Usage: $0 {install|owner|remove-owner|uninstall|cycle}"
    ;;
esac

