#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$REPO_ROOT"

ANDROID_FLAVOR="${E2E_ANDROID_FLAVOR:-googlePlay}"
case "$ANDROID_FLAVOR" in
  googlePlay)
    GRADLE_TASK=":androidApp:assembleGooglePlayDebug"
    APK_PATH="$REPO_ROOT/androidApp/build/outputs/apk/googlePlay/debug/androidApp-googlePlay-debug.apk"
    ;;
  fdroid)
    GRADLE_TASK=":androidApp:assembleFdroidDebug"
    APK_PATH="$REPO_ROOT/androidApp/build/outputs/apk/fdroid/debug/androidApp-fdroid-debug.apk"
    ;;
  *)
    echo "Unsupported E2E_ANDROID_FLAVOR '$ANDROID_FLAVOR'; use googlePlay or fdroid." >&2
    exit 2
    ;;
esac

ANDROID_SERIAL="${ANDROID_SERIAL:-$(adb get-serialno)}"
if [ "$ANDROID_SERIAL" = unknown ] || [ -z "$ANDROID_SERIAL" ]; then
  echo "Choose an Android device with ANDROID_SERIAL before running E2E tests." >&2
  exit 1
fi
export ANDROID_SERIAL

# The restore utility also invokes Maestro, so bind every invocation to this device.
MAESTRO_BIN="$(command -v maestro)"
mkdir -p "$REPO_ROOT/.tmp"
RUN_OUTPUT="$(mktemp -d "$REPO_ROOT/.tmp/android-e2e.XXXXXX")"
mkdir -p "$RUN_OUTPUT/tools"
{
  printf '#!/usr/bin/env bash\nset -euo pipefail\n'
  printf 'selected_device=%q\nmaestro_binary=%q\n' "$ANDROID_SERIAL" "$MAESTRO_BIN"
  cat <<'SHIM'
provided=false
expects_device=false
for argument in "$@"; do
  candidate=""
  if [ "$expects_device" = true ]; then
    candidate="$argument"
    expects_device=false
  else
    case "$argument" in
      --device|--udid) expects_device=true; continue ;;
      --device=*|--udid=*) candidate="${argument#*=}" ;;
      *) continue ;;
    esac
  fi
  if [ "$candidate" != "$selected_device" ]; then
    echo "Maestro device conflicts with ANDROID_SERIAL." >&2
    exit 2
  fi
  provided=true
done
if [ "$expects_device" = true ]; then
  echo "Maestro device argument has no value." >&2
  exit 2
fi
if [ "$provided" = true ]; then
  exec "$maestro_binary" "$@"
fi
exec "$maestro_binary" "--device=$selected_device" "$@"
SHIM
} > "$RUN_OUTPUT/tools/maestro"
chmod +x "$RUN_OUTPUT/tools/maestro"
export PATH="$RUN_OUTPUT/tools:$PATH"

maestro_started=false

restore_development_content() {
  local original_exit_code=$?
  if [ "$maestro_started" = true ]; then
    echo "Restoring development feeds after Android Maestro run..."
    if ! feedflow-restore-dev-feeds --platform android; then
      echo "Development-feed restore failed; run feedflow-restore-dev-feeds --platform android on this device." >&2
      if [ "$original_exit_code" -eq 0 ]; then
        original_exit_code=1
      fi
    fi
  fi
  exit "$original_exit_code"
}
trap restore_development_content EXIT

./gradlew --quiet --console=plain "$GRADLE_TASK"

if command -v android >/dev/null 2>&1; then
  android run --device="$ANDROID_SERIAL" --apks="$APK_PATH"
else
  adb install -r "$APK_PATH"
  adb shell monkey -p com.prof18.feedflow.debug 1
fi

"$REPO_ROOT/e2e/scripts/push-android-fixtures.sh"

E2E_ANDROID_SUITES="${E2E_ANDROID_SUITES:-smoke regression}"

for suite in $E2E_ANDROID_SUITES; do
  while IFS= read -r flow_file; do
    if [ "$ANDROID_FLAVOR" = fdroid ] && [ "$suite" = regression ]; then
      override_file="$REPO_ROOT/e2e/maestro/android/fdroid/regression/$(basename "$flow_file")"
      if [ -f "$override_file" ]; then
        flow_file="$override_file"
      fi
    fi
    maestro_started=true
    flow_name="${flow_file##*/}"
    if [[ "$flow_name" == "174-podcast-audio-playback.yaml" ||
          "$flow_name" == "175-now-playing-navigation.yaml" ||
          "$flow_name" == "176-reader-speech-playback.yaml" ]]; then
      "$REPO_ROOT/e2e/scripts/run-audio-playback.sh" android "${flow_name%.yaml}"
    elif [ "$flow_file" = "$REPO_ROOT/e2e/maestro/android/smoke/007-reader-mode-core.yaml" ]; then
      python3 "$SCRIPT_DIR/android_reader_tooltip.py" --device "$ANDROID_SERIAL" \
        --flow "$flow_file" --output "$RUN_OUTPUT/reader-tooltip" --parent-restores
    elif [ "$flow_file" = "$REPO_ROOT/e2e/maestro/android/regression/179-share-existing-feed.yaml" ]; then
      python3 "$SCRIPT_DIR/android_share_feed.py" --device "$ANDROID_SERIAL" \
        --flow "$flow_file" --output "$RUN_OUTPUT/share-feed" --parent-restores
    else
      maestro --platform android test "$flow_file"
    fi
  done < <(find "$REPO_ROOT/e2e/maestro/android/$suite" -name '*.yaml' | sort)
done
