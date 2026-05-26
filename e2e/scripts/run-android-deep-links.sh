#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
APK_PATH="$REPO_ROOT/androidApp/build/outputs/apk/googlePlay/debug/androidApp-googlePlay-debug.apk"
APP_ID="com.prof18.feedflow.debug"
MAIN_ACTIVITY="com.prof18.feedflow.android.MainActivity"
SEED_FLOW="$REPO_ROOT/e2e/maestro/android/regression/126-deep-link-routing-seed.yaml"

cd "$REPO_ROOT"

./gradlew --quiet --console=plain :androidApp:assembleGooglePlayDebug

if command -v android >/dev/null 2>&1; then
  android run --apks="$APK_PATH"
else
  adb install -r "$APK_PATH"
  adb shell monkey -p "$APP_ID" 1
fi

run_deep_link_case() {
  local link="$1"
  local flow="$2"

  maestro --platform android test "$SEED_FLOW"
  adb shell am start -W \
    -a android.intent.action.VIEW \
    -d "$link" \
    -n "$APP_ID/$MAIN_ACTIVITY" \
    >/dev/null
  maestro --platform android test "$flow"
}

run_deep_link_case \
  "feedflow://feed/e2e-article-reader-success" \
  "$REPO_ROOT/e2e/maestro/android/regression/126-deep-link-routing-article.yaml"

run_deep_link_case \
  "feedflow://feedsourcefilter/e2e-feed-android-weekly" \
  "$REPO_ROOT/e2e/maestro/android/regression/126-deep-link-routing-feed-source.yaml"

run_deep_link_case \
  "feedflow://category/e2e-category-news" \
  "$REPO_ROOT/e2e/maestro/android/regression/126-deep-link-routing-category.yaml"
