#!/usr/bin/env bash

# Runs the iOS test targets in the iOSTests scheme on an iPhone simulator.
# CloudSyncTests is not included: `./gradlew allTests` runs it after the Kotlin side writes its snapshots.
#
# Usage: .scripts/ios-tests.sh [xcodebuild destination]
# The destination defaults to the newest installed iPhone 18 Pro or iPhone 17 Pro, else any iPhone.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/../iosApp"

[[ -f GoogleService-Info-dev.plist ]] || cp ../config/dummy-google-service.plist GoogleService-Info-dev.plist
[[ -f GoogleService-Info.plist ]] || cp ../config/dummy-google-service.plist GoogleService-Info.plist
[[ -f Assets/Config.xcconfig ]] || cp ../config/dummy-config.xcconfig Assets/Config.xcconfig

./.scripts/generate-project.sh

find_simulator() {
  xcrun simctl list devices available --json | python3 -I -c '
import json, sys

def version(runtime):
    parts = runtime.split(".SimRuntime.iOS-")[-1].split("-")[:2]
    return [int(part) if part.isdigit() else 0 for part in parts]

devices = json.load(sys.stdin)["devices"]
runtimes = sorted((r for r in devices if ".SimRuntime.iOS-" in r), key=version, reverse=True)
iphones = [d for r in runtimes for d in devices[r] if d["name"].startswith("iPhone")]
for name in ("iPhone 18 Pro", "iPhone 17 Pro"):
    match = next((d for d in iphones if d["name"] == name), None)
    if match:
        print(match["udid"])
        sys.exit()
if iphones:
    print(iphones[0]["udid"])
'
}

destination="${1:-}"
if [[ -z "$destination" ]]; then
  udid="$(find_simulator)"
  if [[ -z "$udid" ]]; then
    printf 'No iPhone simulator is installed; install one or pass an xcodebuild destination.\n' >&2
    exit 1
  fi
  destination="platform=iOS Simulator,id=$udid"
fi

command=(xcodebuild -project FeedFlow.xcodeproj -scheme iOSTests -configuration Debug -destination "$destination" test)
if command -v xcbeautify >/dev/null 2>&1; then
  "${command[@]}" | xcbeautify
else
  "${command[@]}" -quiet
fi
