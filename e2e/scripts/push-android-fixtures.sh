#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ANDROID_FIXTURE_DIR="/sdcard/Download/feedflow-e2e"

adb shell mkdir -p "$ANDROID_FIXTURE_DIR"
adb shell rm -f "$ANDROID_FIXTURE_DIR/feedflow-valid-opml-smoke.opml"
adb push "$REPO_ROOT/e2e/fixtures/opml/feedflow-valid-opml-smoke.xml" "$ANDROID_FIXTURE_DIR/" >/dev/null
adb push "$REPO_ROOT/e2e/fixtures/csv/feedflow-articles-smoke.csv" "$ANDROID_FIXTURE_DIR/" >/dev/null
