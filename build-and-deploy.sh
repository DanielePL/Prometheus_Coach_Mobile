#!/bin/bash

# Build, install and copy APK to Desktop
# Usage: ./build-and-deploy.sh

set -e

echo "🔨 Building debug APK..."
./gradlew assembleDebug

echo "📱 Installing on device..."
adb -s adb-R5CX40KZ5ET-rK9wsh._adb-tls-connect._tcp install -r app/build/outputs/apk/debug/app-debug.apk

echo "📁 Copying APK to Desktop..."
cp app/build/outputs/apk/debug/app-debug.apk ~/Desktop/PrometheusCoach-debug.apk

echo "✅ Done! APK available at ~/Desktop/PrometheusCoach-debug.apk"
