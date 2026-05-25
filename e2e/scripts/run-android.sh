#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
APK_PATH="$REPO_ROOT/androidApp/build/outputs/apk/googlePlay/debug/androidApp-googlePlay-debug.apk"

cd "$REPO_ROOT"

./gradlew --quiet --console=plain :androidApp:assembleGooglePlayDebug

if command -v android >/dev/null 2>&1; then
  android run --apks="$APK_PATH"
else
  adb install -r "$APK_PATH"
  adb shell monkey -p com.prof18.feedflow.debug 1
fi

maestro --platform android test "$REPO_ROOT/e2e/maestro/android/p0"
