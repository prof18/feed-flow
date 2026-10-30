#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ANDROID_FIXTURE_DIR="/sdcard/Download"

adb shell mkdir -p "$ANDROID_FIXTURE_DIR"
adb push "$REPO_ROOT/e2e/fixtures/opml/." "$ANDROID_FIXTURE_DIR/" >/dev/null
adb push "$REPO_ROOT/e2e/fixtures/csv/." "$ANDROID_FIXTURE_DIR/" >/dev/null
