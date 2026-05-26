#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
APK_PATH="$REPO_ROOT/androidApp/build/outputs/apk/googlePlay/debug/androidApp-googlePlay-debug.apk"
APP_ID="com.prof18.feedflow.debug"

cd "$REPO_ROOT"

cleanup() {
  adb shell wm size reset >/dev/null 2>&1 || true
  adb shell wm density reset >/dev/null 2>&1 || true
}
trap cleanup EXIT

adb shell wm size 1600x2560
adb shell wm density 320

./gradlew --quiet --console=plain :androidApp:assembleGooglePlayDebug

if command -v android >/dev/null 2>&1; then
  android run --apks="$APK_PATH"
else
  adb install -r "$APK_PATH"
  adb shell monkey -p "$APP_ID" 1
fi

maestro --platform android test "$REPO_ROOT/e2e/maestro/android/manual-supported/210-android-tablet-rotation.yaml"
