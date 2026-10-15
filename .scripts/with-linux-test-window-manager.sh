#!/usr/bin/env bash
set -euo pipefail

if [[ $# -eq 0 || -z "${DISPLAY:-}" ]]; then
  echo "Provide a command and a private Xvfb DISPLAY." >&2
  exit 2
fi
for tool in openbox xprop; do
  if ! command -v "$tool" >/dev/null; then
    echo "Linux desktop tests require $tool." >&2
    exit 2
  fi
done

window_manager_registered() {
  local property
  property=$(xprop -root _NET_SUPPORTING_WM_CHECK 2>/dev/null) || return 1
  [[ "$property" =~ window[[:space:]]id[[:space:]]#[[:space:]](0x[[:xdigit:]]+) ]] || return 1
  (( ${BASH_REMATCH[1]} != 0 ))
}

if window_manager_registered; then
  echo "Refusing to replace an existing window manager; use a private Xvfb display." >&2
  exit 2
fi

log_directory=desktopApp/build/reports/tests
mkdir -p "$log_directory"
manager_log="$log_directory/linux-window-manager-$$.log"
openbox --sm-disable > "$manager_log" 2>&1 &
manager_pid=$!

finish() {
  local command_status=$1 manager_status=0
  trap - EXIT
  if kill -0 "$manager_pid" 2>/dev/null; then
    kill -TERM "$manager_pid" 2>/dev/null || true
  fi
  wait "$manager_pid" || manager_status=$?
  if kill -0 "$manager_pid" 2>/dev/null; then
    echo "Owned window manager remains present." >&2
    command_status=1
  elif [[ $command_status -eq 0 && $manager_status -ne 0 ]]; then
    echo "Owned window manager exited with status $manager_status." >&2
    command_status=1
  fi
  echo "Owned window manager pid=$manager_pid exit=$manager_status; log=$manager_log" >&2
  exit "$command_status"
}
trap 'finish "$?"' EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

for ((attempt=0; attempt<100; attempt++)); do
  if ! kill -0 "$manager_pid" 2>/dev/null; then
    echo "Owned window manager exited before becoming ready." >&2
    exit 1
  fi
  if window_manager_registered; then
    "$@"
    if ! kill -0 "$manager_pid" 2>/dev/null; then
      echo "Owned window manager exited during the test command." >&2
      exit 1
    fi
    exit 0
  fi
  sleep 0.1
done
echo "Owned window manager did not register within 100 probes." >&2
exit 1
