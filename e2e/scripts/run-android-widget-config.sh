#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
APK_PATH="$REPO_ROOT/androidApp/build/outputs/apk/googlePlay/debug/androidApp-googlePlay-debug.apk"
APP_ID="com.prof18.feedflow.debug"
CONFIG_ACTIVITY="com.prof18.feedflow.android.widget.WidgetConfigurationActivity"
WIDGET_ID="42"

cd "$REPO_ROOT"

./gradlew --quiet --console=plain :androidApp:assembleGooglePlayDebug

if command -v android >/dev/null 2>&1; then
  android run --apks="$APK_PATH"
else
  adb install -r "$APK_PATH"
  adb shell monkey -p "$APP_ID" 1
fi

maestro --platform android test "$REPO_ROOT/e2e/maestro/android/manual-supported/201-android-widget-seed.yaml"

adb shell am start -W -n "$APP_ID/$CONFIG_ACTIVITY" --ei appWidgetId "$WIDGET_ID" >/dev/null

maestro --platform android test "$REPO_ROOT/e2e/maestro/android/manual-supported/201-android-widget-configuration.yaml"
