#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$REPO_ROOT"

step() {
  printf '\n[%s] %s\n' "$(date '+%H:%M:%S')" "$1"
}

run_cmd() {
  printf '+ %s\n' "$*"
  "$@"
}

run_shell() {
  printf '+ %s\n' "$1"
  bash -lc "$1"
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    printf 'Missing required command: %s\n' "$1" >&2
    exit 1
  fi
}

prepare_ios_cloud_sync_inputs() {
  if [[ "$(uname -s)" != "Darwin" ]]; then
    return
  fi

  require_command xcodegen

  [[ -f iosApp/GoogleService-Info-dev.plist ]] || \
    run_cmd cp config/dummy-google-service.plist iosApp/GoogleService-Info-dev.plist
  [[ -f iosApp/GoogleService-Info.plist ]] || \
    run_cmd cp config/dummy-google-service.plist iosApp/GoogleService-Info.plist
  [[ -f iosApp/Assets/Config.xcconfig ]] || \
    run_cmd cp config/dummy-config.xcconfig iosApp/Assets/Config.xcconfig

  if [[ ! -d iosApp/FeedFlow.xcodeproj ]]; then
    step "Generate Xcode project"
    run_shell 'cd iosApp && ./.scripts/generate-project.sh'
  fi
}

run_checks_job() {
  step "Checks job: refresh translations"
  run_cmd bash .scripts/refresh-translations.sh

  step "Checks job: run SwiftLint"
  require_command swiftlint
  run_shell 'cd iosApp && swiftlint'

  step "Checks job: detekt + allTests"
  prepare_ios_cloud_sync_inputs
  run_cmd ./gradlew --quiet --console=plain detekt allTests
}

run_android_build_job() {
  step "Android build job: assemble debug app"
  run_cmd ./gradlew --quiet --console=plain :androidApp:assembleGooglePlayDebug
}

run_desktop_build_job() {
  step "Desktop build job: package distribution"
  run_cmd ./gradlew --quiet --console=plain :desktopApp:packageDistributionForCurrentOS
}

run_ios_build_job() {
  if [[ "$(uname -s)" != "Darwin" ]]; then
    printf 'The iOS CI job can only run on macOS.\n' >&2
    exit 1
  fi

  require_command xcodebuild

  step "iOS build job: build iOS app"
  if command -v xcbeautify >/dev/null 2>&1; then
    run_shell 'cd iosApp && set -o pipefail && xcodebuild -project FeedFlow.xcodeproj -configuration Debug -scheme FeedFlow -sdk iphonesimulator -destination "generic/platform=iOS Simulator" ARCHS=arm64 ONLY_ACTIVE_ARCH=YES build | xcbeautify --renderer github-actions'
  else
    printf 'xcbeautify not found, falling back to raw xcodebuild output.\n'
    run_shell 'cd iosApp && xcodebuild -project FeedFlow.xcodeproj -configuration Debug -scheme FeedFlow -sdk iphonesimulator -destination "generic/platform=iOS Simulator" ARCHS=arm64 ONLY_ACTIVE_ARCH=YES build'
  fi
}

if [[ $# -gt 0 ]]; then
  case "$1" in
    -h|--help)
      cat <<'EOF'
Usage: ./.scripts/ci.sh
EOF
      exit 0
      ;;
    *)
      printf 'Unknown argument: %s\n' "$1" >&2
      exit 1
      ;;
  esac
fi

step "Running local CI workflow equivalent from .github/workflows/code-checks.yaml"
run_checks_job
run_android_build_job
run_desktop_build_job
run_ios_build_job

step "Local CI workflow completed successfully"
