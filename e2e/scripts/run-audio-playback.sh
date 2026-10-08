#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
PLATFORM="${1:-}"
FLOW_NAME="${2:-174-podcast-audio-playback}"

if [[ "$PLATFORM" != android && "$PLATFORM" != ios ]]; then
  echo "Usage: $0 android|ios [174-podcast-audio-playback|175-now-playing-navigation|176-reader-speech-playback]" >&2
  exit 2
fi

case "$FLOW_NAME" in
  174-podcast-audio-playback|175-now-playing-navigation|176-reader-speech-playback) ;;
  *) echo "Unknown audio flow: $FLOW_NAME" >&2; exit 2 ;;
esac

command -v maestro >/dev/null 2>&1 || { echo "maestro is required" >&2; exit 1; }
command -v python3 >/dev/null 2>&1 || { echo "python3 is required" >&2; exit 1; }

REPORT_ROOT="$REPO_ROOT/.tmp/audio-playback"
mkdir -p "$REPORT_ROOT"
TEMP_DIR="$(mktemp -d "$REPORT_ROOT/run.XXXXXX")"
PORT_FILE="$TEMP_DIR/port"
SERVER_LOG="$TEMP_DIR/server.log"
SERVER_PID=""
MAESTRO_PID=""
MAESTRO_STARTED=false
ANDROID_SERIAL="${ANDROID_SERIAL:-}"
ANDROID_REVERSE_ACTIVE=false
PORT=""

cleanup() {
  local exit_code=$?
  if [[ -n "$MAESTRO_PID" ]]; then
    kill "$MAESTRO_PID" 2>/dev/null || true
    wait "$MAESTRO_PID" 2>/dev/null || true
  fi
  if [[ -n "$SERVER_PID" ]]; then
    kill "$SERVER_PID" 2>/dev/null || true
    wait "$SERVER_PID" 2>/dev/null || true
  fi
  if [[ "$ANDROID_REVERSE_ACTIVE" == true ]]; then
    adb -s "$ANDROID_SERIAL" reverse --remove "tcp:$PORT" || \
      echo "Warning: failed to remove adb reverse mapping tcp:$PORT for $ANDROID_SERIAL." >&2
  fi
  if [[ "$MAESTRO_STARTED" == true ]] && command -v feedflow-restore-dev-feeds >/dev/null 2>&1; then
    echo "Restoring development feeds after audio Maestro flow..."
    if [[ "$PLATFORM" == ios ]]; then
      feedflow-restore-dev-feeds --platform ios --simulator "$SIMULATOR_UDID" || \
        echo "Warning: development-feed restore failed; run feedflow-restore-dev-feeds --platform ios --simulator $SIMULATOR_UDID manually." >&2
    else
      (
        export FEEDFLOW_AUDIO_MAESTRO_BIN="$(command -v maestro)"
        export FEEDFLOW_AUDIO_ANDROID_SERIAL="$ANDROID_SERIAL"
        maestro() {
          "$FEEDFLOW_AUDIO_MAESTRO_BIN" --device "$FEEDFLOW_AUDIO_ANDROID_SERIAL" "$@"
        }
        export -f maestro
        feedflow-restore-dev-feeds --platform android
      ) || \
        echo "Warning: development-feed restore failed; run feedflow-restore-dev-feeds --platform android manually." >&2
    fi
  elif [[ "$MAESTRO_STARTED" == true ]]; then
    echo "Warning: feedflow-restore-dev-feeds is unavailable; restore development feeds manually for $PLATFORM." >&2
  fi
  echo "Audio Maestro logs retained at $TEMP_DIR"
  return "$exit_code"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

python3 "$SCRIPT_DIR/serve-audio-fixture.py" --port 0 --port-file "$PORT_FILE" >"$SERVER_LOG" 2>&1 &
SERVER_PID=$!
for _ in $(seq 1 100); do
  [[ -s "$PORT_FILE" ]] && break
  kill -0 "$SERVER_PID" 2>/dev/null || { cat "$SERVER_LOG" >&2; exit 1; }
  sleep 0.05
done
if [[ ! -s "$PORT_FILE" ]]; then
  cat "$SERVER_LOG" >&2
  echo "Audio fixture server did not report its selected port" >&2
  exit 1
fi

PORT="$(cat "$PORT_FILE")"
case "$PLATFORM" in
  android)
    ANDROID_SERIAL="${ANDROID_SERIAL:-$(adb devices | awk 'NR > 1 && $2 == "device" { print $1; exit }')}"
    [[ -n "$ANDROID_SERIAL" ]] || { echo "No Android device or emulator is connected" >&2; exit 1; }
    adb -s "$ANDROID_SERIAL" reverse "tcp:$PORT" "tcp:$PORT"
    ANDROID_REVERSE_ACTIVE=true
    AUDIO_HOST="127.0.0.1"
    FLOW="$REPO_ROOT/e2e/maestro/android/regression/$FLOW_NAME.yaml"
    MAESTRO=(maestro --platform android --device "$ANDROID_SERIAL")
    ;;
  ios)
    AUDIO_HOST="127.0.0.1"
    SIMULATOR_UDID="${SIMULATOR_UDID:-}"
    if [[ -z "$SIMULATOR_UDID" ]]; then
      SIMULATOR_UDID="$(xcrun simctl list devices booted | awk -F '[()]' '/iPhone 17 Pro/ {print $2; exit}')"
    fi
    [[ -n "$SIMULATOR_UDID" ]] || { echo "No booted iPhone 17 Pro simulator found" >&2; exit 1; }
    FLOW="$REPO_ROOT/e2e/maestro/ios/regression/$FLOW_NAME.yaml"
    MAESTRO=(maestro --platform ios --device "$SIMULATOR_UDID")
    ;;
esac

export AUDIO_URL="http://$AUDIO_HOST:$PORT/feedflow-e2e-tone.wav"
echo "Using local audio fixture at $AUDIO_URL"
cd "$REPO_ROOT"
MAESTRO_STARTED=true
"${MAESTRO[@]}" test -e "AUDIO_URL=$AUDIO_URL" "$FLOW" &
MAESTRO_PID=$!
wait "$MAESTRO_PID"
MAESTRO_PID=""
