#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SIMULATOR_UDID="${SIMULATOR_UDID:-$(xcrun simctl list devices booted | awk -F '[()]' '/iPhone 17 Pro/ {print $2; exit}')}"
APP_ID="${APP_ID:-com.prof18.feedflow.dev}"

if [ -z "$SIMULATOR_UDID" ]; then
  echo "No booted iPhone 17 Pro simulator found. Boot one before pushing iOS fixtures." >&2
  exit 1
fi

APP_CONTAINER="$(xcrun simctl get_app_container "$SIMULATOR_UDID" "$APP_ID" data)"
IOS_FIXTURE_DIR="$APP_CONTAINER/Documents/e2e-import"
SIMULATOR_DATA_DIR="$HOME/Library/Developer/CoreSimulator/Devices/$SIMULATOR_UDID/data"

mkdir -p "$IOS_FIXTURE_DIR"
cp "$REPO_ROOT/e2e/fixtures/opml/feedflow-valid-opml-smoke.xml" "$IOS_FIXTURE_DIR/"
cp "$REPO_ROOT/e2e/fixtures/csv/feedflow-articles-smoke.csv" "$IOS_FIXTURE_DIR/"

while IFS= read -r files_provider_dir; do
  cp "$REPO_ROOT/e2e/fixtures/csv/feedflow-articles-smoke.csv" "$files_provider_dir/"
  files_provider_fixture_dir="$files_provider_dir/feedflow-e2e"
  mkdir -p "$files_provider_fixture_dir"
  cp "$REPO_ROOT/e2e/fixtures/opml/feedflow-valid-opml-smoke.xml" "$files_provider_fixture_dir/"
  cp "$REPO_ROOT/e2e/fixtures/csv/feedflow-articles-smoke.csv" "$files_provider_fixture_dir/"
done < <(find "$SIMULATOR_DATA_DIR/Containers/Shared/AppGroup" -path '*/File Provider Storage' -type d -print)
