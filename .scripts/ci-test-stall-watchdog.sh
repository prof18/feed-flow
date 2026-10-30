#!/usr/bin/env bash
# Prints thread dumps of running Gradle test JVMs when desktop tests stop making progress.
# Diagnostic only: it never signals or stops the processes it inspects. Run it in the background
# next to the Gradle command and stop it once Gradle finishes.
set -uo pipefail

watch_file=${STALL_WATCH_FILE:-desktopApp/build/test-results/jvmTest/binary/in-progress-results-generic.bin}
stall_seconds=${STALL_SECONDS:-300}
poll_seconds=${STALL_POLL_SECONDS:-30}
dump_dir=${STALL_DUMP_DIR:-desktopApp/build/reports/stall-dumps}
process_pattern=${STALL_PROCESS_PATTERN:-Gradle Test Executor}

progress_signature() {
  # Size and modification time together change whenever Gradle records a test event.
  [[ -f "$watch_file" ]] || return 1
  if stat -f '%z %m' "$watch_file" 2>/dev/null; then return 0; fi
  stat -c '%s %Y' "$watch_file"
}

dump_threads() {
  local pid=$1 output=$2 java_bin tool_dir
  java_bin=$(ps -o command= -p "$pid" 2>/dev/null | awk '{print $1}')
  tool_dir=$(dirname "${java_bin:-/nonexistent}")
  # Prefer the JVM's own tools: attach fails across mismatched JDK versions.
  for tool in "$tool_dir/jcmd" "$tool_dir/jstack" jcmd jstack; do
    [[ -x "$tool" ]] || command -v "$tool" >/dev/null 2>&1 || continue
    if [[ "$(basename "$tool")" == jcmd ]]; then
      "$tool" "$pid" Thread.print > "$output" 2>&1 && return 0
    else
      "$tool" "$pid" > "$output" 2>&1 && return 0
    fi
  done
  return 1
}

last_signature=""
last_change=$(date +%s)
last_dump=0
while true; do
  sleep "$poll_seconds"
  now=$(date +%s)
  signature=$(progress_signature) || { last_change=$now; continue; }
  if [[ "$signature" != "$last_signature" ]]; then
    last_signature=$signature
    last_change=$now
    continue
  fi
  (( now - last_change >= stall_seconds && now - last_dump >= stall_seconds )) || continue
  last_dump=$now
  mkdir -p "$dump_dir"
  echo "::warning::Desktop tests made no progress for $((now - last_change))s; printing thread dumps."
  for pid in $(pgrep -f "$process_pattern"); do
    output="$dump_dir/threads-$pid-$now.txt"
    if dump_threads "$pid" "$output"; then
      echo "::group::Thread dump of test JVM $pid"
      cat "$output"
      echo "::endgroup::"
    else
      echo "Could not dump threads of process $pid: $(cat "$output" 2>/dev/null)"
    fi
  done
done
