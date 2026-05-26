#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
APK_PATH="$REPO_ROOT/androidApp/build/outputs/apk/googlePlay/debug/androidApp-googlePlay-debug.apk"
APP_ID="com.prof18.feedflow.debug"
SHARE_ACTIVITY="com.prof18.feedflow.android.addfeed.AddFeedExtensionActivity"
SERVER_PORT="18765"
SERVER_LOG="${TMPDIR:-/tmp}/feedflow-share-intent-http.log"
SHARE_URL="http://10.0.2.2:$SERVER_PORT/share-intent-feed.xml"

cd "$REPO_ROOT"

python3 -m http.server "$SERVER_PORT" \
  --bind 127.0.0.1 \
  --directory "$REPO_ROOT/e2e/fixtures/rss" \
  >"$SERVER_LOG" 2>&1 &
SERVER_PID="$!"

cleanup() {
  kill "$SERVER_PID" >/dev/null 2>&1 || true
}
trap cleanup EXIT

SERVER_READY=false
for _ in {1..40}; do
  if curl --connect-timeout 1 --max-time 2 -fsS "http://127.0.0.1:$SERVER_PORT/share-intent-feed.xml" >/dev/null 2>&1; then
    SERVER_READY=true
    break
  fi
  sleep 0.25
done

if [[ "$SERVER_READY" != true ]]; then
  echo "Local share-intent fixture server did not start. Log:" >&2
  cat "$SERVER_LOG" >&2
  exit 1
fi

./gradlew --quiet --console=plain :androidApp:assembleGooglePlayDebug

if command -v android >/dev/null 2>&1; then
  android run --apks="$APK_PATH"
else
  adb install -r "$APK_PATH"
  adb shell monkey -p "$APP_ID" 1
fi

maestro --platform android test "$REPO_ROOT/e2e/maestro/android/manual-supported/204-android-share-intent-seed.yaml"

adb shell am start -W \
  -a android.intent.action.SEND \
  -t text/plain \
  --es android.intent.extra.TEXT "$SHARE_URL" \
  -n "$APP_ID/$SHARE_ACTIVITY" \
  >/dev/null

maestro --platform android test "$REPO_ROOT/e2e/maestro/android/manual-supported/204-android-share-intent.yaml"
