#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
REPO_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"

PROJECT_PATH="$REPO_ROOT/iosApp/FeedFlow.xcodeproj"
SCHEME="FeedFlow"
BUILD_CONFIGURATION="${BUILD_CONFIGURATION:-Debug}"
PACKAGE_FLAGS=(
  -onlyUsePackageVersionsFromResolvedFile
)
RUN_AFTER_BUILD=1
VERBOSE_BUILD=1
USE_XCBEAUTIFY=1
TARGET_KIND=""
TARGET_QUERY=""

usage() {
  cat <<'EOF'
Usage:
  .scripts/run-ios-target.sh --simulator "<simulator name>" [--build-only] [--verbose-build]
  .scripts/run-ios-target.sh --device "<device name fragment>" [--build-only] [--verbose-build]

Examples:
  .scripts/run-ios-target.sh --simulator "iPhone 17 Pro"
  .scripts/run-ios-target.sh --device "iPhone 16e" --verbose-build
EOF
}

log() {
  printf '%s\n' "$*"
}

fail() {
  printf 'error: %s\n' "$*" >&2
  exit 1
}

require_value() {
  if [ "$#" -lt 2 ] || [ -z "$2" ]; then
    fail "missing value for $1"
  fi
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --simulator)
      require_value "$@"
      TARGET_KIND="simulator"
      TARGET_QUERY="$2"
      shift 2
      ;;
    --device)
      require_value "$@"
      TARGET_KIND="device"
      TARGET_QUERY="$2"
      shift 2
      ;;
    --build-only)
      RUN_AFTER_BUILD=0
      shift
      ;;
    --verbose-build)
      VERBOSE_BUILD=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      usage >&2
      fail "unknown argument: $1"
      ;;
  esac
done

[ -n "$TARGET_KIND" ] || fail "you must pass either --simulator or --device"
[ -f "$PROJECT_PATH/project.pbxproj" ] || fail "Xcode project not found at $PROJECT_PATH"

for tool in python3 xcodebuild xcrun /usr/libexec/PlistBuddy; do
  if [ "$tool" = "/usr/libexec/PlistBuddy" ]; then
    [ -x "$tool" ] || fail "required tool not found: $tool"
  else
    command -v "$tool" >/dev/null 2>&1 || fail "required tool not found: $tool"
  fi
done

if command -v xcbeautify >/dev/null 2>&1; then
  USE_XCBEAUTIFY=1
fi

get_bundle_id() {
  local app_path="$1"
  /usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$app_path/Info.plist"
}

run_xcodebuild() {
  shift

  if [ "$VERBOSE_BUILD" -eq 1 ]; then
    if [ "$USE_XCBEAUTIFY" -eq 1 ]; then
      xcodebuild "$@" 2>&1 | xcbeautify --quiet
    else
      xcodebuild "$@"
    fi
  else
    xcodebuild "$@" -quiet
  fi
}

run_devicectl_step() {
  local step_name="$1"
  shift

  log "$step_name"

  if [ "$VERBOSE_BUILD" -eq 1 ]; then
    xcrun devicectl "$@"
  else
    xcrun devicectl "$@" --quiet
  fi
}

resolve_app_path() {
  local destination="$1"
  local settings_file build_dir product_name
  settings_file="$(mktemp)"

  xcodebuild \
    -showBuildSettings \
    -project "$PROJECT_PATH" \
    -scheme "$SCHEME" \
    -configuration "$BUILD_CONFIGURATION" \
    "${PACKAGE_FLAGS[@]}" \
    -destination "$destination" >"$settings_file"

  build_dir="$(awk -F ' = ' '/TARGET_BUILD_DIR = / {print $2; exit}' "$settings_file")"
  product_name="$(awk -F ' = ' '/FULL_PRODUCT_NAME = / {print $2; exit}' "$settings_file")"
  rm -f "$settings_file"

  [ -n "$build_dir" ] || fail "unable to resolve TARGET_BUILD_DIR for $destination"
  [ -n "$product_name" ] || fail "unable to resolve FULL_PRODUCT_NAME for $destination"

  printf '%s/%s\n' "$build_dir" "$product_name"
}

resolve_simulator_udid() {
  local device_name="$1"
  local json_file
  json_file="$(mktemp)"

  xcrun simctl list devices available -j >"$json_file"
  python3 - "$json_file" "$device_name" <<'PY'
import json
import sys

json_path, device_name = sys.argv[1], sys.argv[2]
needle = device_name.casefold()

with open(json_path, "r", encoding="utf-8") as fh:
    data = json.load(fh)

exact = []
fuzzy = []
for devices in data.get("devices", {}).values():
    for device in devices:
        if not device.get("isAvailable"):
            continue
        name = device.get("name", "")
        entry = (name, device.get("udid", ""))
        if name.casefold() == needle:
            exact.append(entry)
        elif needle in name.casefold():
            fuzzy.append(entry)

matches = exact or fuzzy
if not matches:
    raise SystemExit(f"error: no available simulator matched '{device_name}'")
if len(matches) > 1:
    names = ", ".join(name for name, _ in matches)
    raise SystemExit(f"error: simulator query '{device_name}' matched multiple devices: {names}")

print(matches[0][1])
PY
  rm -f "$json_file"
}

resolve_device_udid() {
  local device_query="$1"
  local json_file
  json_file="$(mktemp)"

  xcrun devicectl list devices --json-output "$json_file" >/dev/null 2>&1
  python3 - "$json_file" "$device_query" <<'PY'
import json
import sys

json_path, device_query = sys.argv[1], sys.argv[2]
needle = device_query.casefold()

with open(json_path, "r", encoding="utf-8") as fh:
    data = json.load(fh)

matches = []
for device in data.get("result", {}).get("devices", []):
    capabilities = {
        item.get("featureIdentifier", "")
        for item in device.get("capabilities", [])
    }
    pairing_state = device.get("connectionProperties", {}).get("pairingState", "")
    can_connect = "com.apple.coredevice.feature.connectdevice" in capabilities
    can_install_and_launch = {
        "com.apple.coredevice.feature.installapp",
        "com.apple.coredevice.feature.launchapplication",
    }.issubset(capabilities)
    if pairing_state != "paired" or not (can_connect or can_install_and_launch):
        continue

    device_name = device.get("deviceProperties", {}).get("name", "")
    marketing_name = device.get("hardwareProperties", {}).get("marketingName", "")
    product_type = device.get("hardwareProperties", {}).get("productType", "")
    udid = device.get("hardwareProperties", {}).get("udid", "")
    haystacks = [device_name, marketing_name, product_type]

    if any(needle in value.casefold() for value in haystacks if value):
        matches.append((device_name, marketing_name, udid))

if not matches:
    raise SystemExit(f"error: no connected paired device matched '{device_query}'")
if len(matches) > 1:
    names = ", ".join(name for name, _, _ in matches)
    raise SystemExit(f"error: device query '{device_query}' matched multiple devices: {names}")

print(matches[0][2])
PY
  rm -f "$json_file"
}

build_for_simulator() {
  local simulator_name="$1"
  local simulator_udid destination app_path bundle_id

  if ! simulator_udid="$(resolve_simulator_udid "$simulator_name")"; then
    exit 1
  fi
  [ -n "$simulator_udid" ] || fail "resolved simulator UDID was empty for $simulator_name"
  destination="platform=iOS Simulator,id=$simulator_udid"

  log "Building FeedFlow for simulator: $simulator_name"
  run_xcodebuild "ios-simulator-$simulator_name" \
    -project "$PROJECT_PATH" \
    -scheme "$SCHEME" \
    -configuration "$BUILD_CONFIGURATION" \
    "${PACKAGE_FLAGS[@]}" \
    -destination "$destination" \
    build

  app_path="$(resolve_app_path "$destination")"
  [ -d "$app_path" ] || fail "built app not found at $app_path"

  if [ "$RUN_AFTER_BUILD" -eq 0 ]; then
    log "Build completed: $app_path"
    return
  fi

  bundle_id="$(get_bundle_id "$app_path")"

  log "Booting simulator: $simulator_name"
  xcrun simctl boot "$simulator_udid" >/dev/null 2>&1 || true
  xcrun simctl bootstatus "$simulator_udid" -b
  open -a Simulator --args -CurrentDeviceUDID "$simulator_udid"

  log "Installing app on simulator"
  xcrun simctl uninstall "$simulator_udid" "$bundle_id" >/dev/null 2>&1 || true
  xcrun simctl install "$simulator_udid" "$app_path"

  log "Launching $bundle_id"
  xcrun simctl launch "$simulator_udid" "$bundle_id"
}

build_for_device() {
  local device_query="$1"
  local device_udid destination app_path bundle_id

  if ! device_udid="$(resolve_device_udid "$device_query")"; then
    exit 1
  fi
  [ -n "$device_udid" ] || fail "resolved device UDID was empty for $device_query"
  destination="platform=iOS,id=$device_udid"

  log "Building FeedFlow for device: $device_query"
  run_xcodebuild "ios-device-$device_query" \
    -project "$PROJECT_PATH" \
    -scheme "$SCHEME" \
    -configuration "$BUILD_CONFIGURATION" \
    "${PACKAGE_FLAGS[@]}" \
    -destination "$destination" \
    build

  app_path="$(resolve_app_path "$destination")"
  [ -d "$app_path" ] || fail "built app not found at $app_path"

  if [ "$RUN_AFTER_BUILD" -eq 0 ]; then
    log "Build completed: $app_path"
    return
  fi

  bundle_id="$(get_bundle_id "$app_path")"

  run_devicectl_step "Installing app on device" \
    device install app --device "$device_udid" "$app_path"

  run_devicectl_step "Launching $bundle_id" \
    device process launch \
    --device "$device_udid" \
    --terminate-existing \
    "$bundle_id"
}

case "$TARGET_KIND" in
  simulator)
    build_for_simulator "$TARGET_QUERY"
    ;;
  device)
    build_for_device "$TARGET_QUERY"
    ;;
esac
