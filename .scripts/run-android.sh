#!/bin/sh
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/.."

./gradlew :androidApp:installGooglePlayDebug --quiet --console=plain
adb shell monkey -p com.prof18.feedflow.debug -c android.intent.category.LAUNCHER 1
