#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DERIVED_DATA_PATH="$REPO_ROOT/iosApp/build/e2e-derived-data"
APP_PATH="$DERIVED_DATA_PATH/Build/Products/Debug-iphonesimulator/FeedFlow.app"
SIMULATOR_UDID="${SIMULATOR_UDID:-$(xcrun simctl list devices booted | awk -F '[()]' '/iPhone 17 Pro/ {print $2; exit}')}"

cd "$REPO_ROOT"

if [ -z "$SIMULATOR_UDID" ]; then
  echo "No booted iPhone 17 Pro simulator found. Boot one before running iOS E2E flows." >&2
  exit 1
fi

if [ ! -d "$REPO_ROOT/iosApp/FeedFlow.xcodeproj" ]; then
  cd "$REPO_ROOT/iosApp"
  ./.scripts/generate-project.sh
  cd "$REPO_ROOT"
fi

xcodebuild \
  -project iosApp/FeedFlow.xcodeproj \
  -scheme FeedFlow \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -derivedDataPath "$DERIVED_DATA_PATH" \
  build \
  -quiet

xcrun simctl install "$SIMULATOR_UDID" "$APP_PATH"
while IFS= read -r flow_file; do
  maestro --platform ios --device "$SIMULATOR_UDID" test "$flow_file"
done < <(find "$REPO_ROOT/e2e/maestro/ios/release-gate" -name '*.yaml' | sort)
