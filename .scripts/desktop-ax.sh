#!/usr/bin/env bash
# Read and press UI elements of the running Desktop app (main window and dialogs) through macOS
# Accessibility, in the background. Needs no computer-use grant; typing into text fields is not supported.
#
#   .scripts/desktop-ax.sh dump "Add feed"
#   .scripts/desktop-ax.sh press "Add feed" AXButton:Cancel
#
# The app pid comes from the hot reload pid file; set FEEDFLOW_DESKTOP_PID to target another process.
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
source_file="$repo_root/.scripts/lib/desktop-ax.swift"
binary="$repo_root/build/desktop-ax/desktop-ax"

if [[ $# -lt 2 ]]; then
  echo "usage: $0 dump <window title> | press <window title> [AXRole:]<label>" >&2
  exit 2
fi

if [[ ! -x "$binary" || "$source_file" -nt "$binary" ]]; then
  mkdir -p "$(dirname "$binary")"
  swiftc -O "$source_file" -o "$binary"
fi

pid="${FEEDFLOW_DESKTOP_PID:-}"
if [[ -z "$pid" ]]; then
  pid_file="$repo_root/desktopApp/build/run/jvmMain/jvmMain.pid"
  pid="$(sed -n 's/^pid=//p' "$pid_file" 2>/dev/null || true)"
fi
if [[ -z "$pid" ]] || ! kill -0 "$pid" 2>/dev/null; then
  echo "Desktop app is not running; start it with ./gradlew --quiet --console=plain :desktopApp:hotRunJvm" >&2
  exit 1
fi

command="$1"
window="$2"
shift 2
exec "$binary" "$pid" "$window" "$command" "$@"
