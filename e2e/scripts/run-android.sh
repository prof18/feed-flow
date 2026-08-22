#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
APK_PATH="$REPO_ROOT/androidApp/build/outputs/apk/googlePlay/debug/androidApp-googlePlay-debug.apk"

cd "$REPO_ROOT"

maestro_started=false

restore_development_content() {
  local original_exit_code=$?
  if [ "$maestro_started" = true ] && command -v feedflow-restore-dev-feeds >/dev/null 2>&1; then
    echo "Restoring development feeds after Android Maestro run..."
    feedflow-restore-dev-feeds --platform android || \
      echo "Warning: development-feed restore failed; run feedflow-restore-dev-feeds --platform android manually." >&2
  fi
  return "$original_exit_code"
}
trap restore_development_content EXIT

./gradlew --quiet --console=plain :androidApp:assembleGooglePlayDebug

if command -v android >/dev/null 2>&1; then
  android run --apks="$APK_PATH"
else
  adb install -r "$APK_PATH"
  adb shell monkey -p com.prof18.feedflow.debug 1
fi

"$REPO_ROOT/e2e/scripts/push-android-fixtures.sh"

E2E_ANDROID_SUITES="${E2E_ANDROID_SUITES:-smoke regression}"

for suite in $E2E_ANDROID_SUITES; do
  while IFS= read -r flow_file; do
    maestro_started=true
    maestro --platform android test "$flow_file"
  done < <(find "$REPO_ROOT/e2e/maestro/android/$suite" -name '*.yaml' | sort)
done
