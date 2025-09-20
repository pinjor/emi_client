#!/bin/bash
PKG="com.example.emilockerclient"
ADMIN=".admin.EmiAdminReceiver"
APK="./app-debug.apk"   # adjust path if needed

case "$1" in
  install)
    echo "📦 Installing debug APK..."
    adb install -r -t "$APK"
    ;;
  owner)
    echo "🔑 Setting device owner..."
    adb shell dpm set-device-owner $PKG/$ADMIN
    adb shell dumpsys device_policy | grep "Device Owner"
    ;;
  remove-owner)
    echo "🚫 Removing device owner..."
    adb shell dpm remove-active-admin $PKG/$ADMIN
    ;;
  uninstall)
    echo "🗑️ Uninstalling app..."
    adb uninstall $PKG
    ;;
  cycle)
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

