#!/usr/bin/env bash

# Runs the iOS test targets in the iOSTests scheme on an iPhone simulator.
# CloudSyncTests is not included: `./gradlew allTests` runs it after the Kotlin side writes its snapshots.
# Both runs share one DerivedData folder and the same build settings (see SwiftCloudSyncTest), so the
# Swift packages are fetched and compiled once whichever runs first.
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

# Link the framework here instead of through FeedFlowKitBuilder's nested Gradle build.
(cd .. && ./gradlew --quiet --console=plain :shared:linkDebugFrameworkIosSimulatorArm64)
framework_dir="$(cd ../shared/build/bin/iosSimulatorArm64/debugFramework && pwd)"
derived_data="$(cd .. && pwd)/build/ios-test-derived-data"
source_packages="$(cd .. && pwd)/build/ios-source-packages"

destination="${1:-}"
if [[ -z "$destination" ]]; then
  udid="$(bash "$SCRIPT_DIR/ios-test-simulator.sh")"
  if [[ -z "$udid" ]]; then
    printf 'No iPhone simulator is installed; install one or pass an xcodebuild destination.\n' >&2
    exit 1
  fi
  destination="platform=iOS Simulator,id=$udid"
fi

# FRAMEWORK_SEARCH_PATHS is quoted exactly as SwiftCloudSyncTest passes it: any difference rebuilds every Swift package.
command=(
  xcodebuild -project FeedFlow.xcodeproj -scheme iOSTests -configuration Debug
  -destination "$destination"
  -derivedDataPath "$derived_data"
  -clonedSourcePackagesDirPath "$source_packages"
  OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES
  "FRAMEWORK_SEARCH_PATHS=\"$framework_dir\""
  test
)
if command -v xcbeautify >/dev/null 2>&1; then
  "${command[@]}" | xcbeautify
else
  "${command[@]}" -quiet
fi
