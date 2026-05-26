#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DERIVED_DATA_PATH="$REPO_ROOT/iosApp/build/e2e-derived-data"
APP_PATH="$DERIVED_DATA_PATH/Build/Products/Debug-iphonesimulator/FeedFlow.app"
SIMULATOR_NAME="${SIMULATOR_NAME:-iPad Pro 11-inch (M4) - iOS 18}"
SIMULATOR_UDID="${SIMULATOR_UDID:-$(xcrun simctl list devices available | grep -F "$SIMULATOR_NAME" | grep -Eo '[0-9A-F]{8}-([0-9A-F]{4}-){3}[0-9A-F]{12}' | head -1)}"

cd "$REPO_ROOT"

if [ -z "$SIMULATOR_UDID" ]; then
  echo "No available $SIMULATOR_NAME simulator found." >&2
  exit 1
fi

if [ ! -d "$REPO_ROOT/iosApp/FeedFlow.xcodeproj" ]; then
  cd "$REPO_ROOT/iosApp"
  ./.scripts/generate-project.sh
  cd "$REPO_ROOT"
fi

xcrun simctl boot "$SIMULATOR_UDID" >/dev/null 2>&1 || true

xcodebuild \
  -project iosApp/FeedFlow.xcodeproj \
  -scheme FeedFlow \
  -destination "platform=iOS Simulator,id=$SIMULATOR_UDID" \
  -derivedDataPath "$DERIVED_DATA_PATH" \
  build \
  -quiet

xcrun simctl uninstall "$SIMULATOR_UDID" com.prof18.feedflow.dev >/dev/null 2>&1 || true
xcrun simctl install "$SIMULATOR_UDID" "$APP_PATH"

maestro --platform ios --device "$SIMULATOR_UDID" test "$REPO_ROOT/e2e/maestro/ios/manual-supported/205-ipad-split-layout.yaml"
