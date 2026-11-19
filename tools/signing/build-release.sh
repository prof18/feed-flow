#!/usr/bin/env bash
# Builds a signed release locally with the signing secrets read from 1Password.
# Usage: FEEDFLOW_SIGNING_ITEM=op://<vault>/<item> tools/signing/build-release.sh [gradle task ...]
set -euo pipefail

: "${FEEDFLOW_SIGNING_ITEM:?set FEEDFLOW_SIGNING_ITEM to the 1Password item reference}"
command -v op >/dev/null 2>&1 || { echo "error: the 1Password CLI (op) is not installed." >&2; exit 1; }

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PROPS="$REPO_ROOT/keystore.properties"
TASKS=("${@:-:androidApp:bundleGooglePlayRelease}")

umask 077
WORK_DIR="$(mktemp -d)"
HAD_PROPS=false

cleanup() {
  if [[ "$HAD_PROPS" == true && -f "$WORK_DIR/backup" ]]; then
    mv -f "$WORK_DIR/backup" "$PROPS"
  else
    rm -f "$PROPS"
  fi
  rm -rf "$WORK_DIR"
}
trap cleanup EXIT
trap 'exit 130' INT TERM

if [[ -f "$PROPS" ]]; then
  cp -p "$PROPS" "$WORK_DIR/backup"
  HAD_PROPS=true
fi

read_field() {
  local value
  value="$(op read "$FEEDFLOW_SIGNING_ITEM/$1")"
  printf '%s' "${value//\\/\\\\}"
}

op read --force --out-file "$WORK_DIR/release.jks" "$FEEDFLOW_SIGNING_ITEM/release.jks" >/dev/null
KEY_ALIAS="$(read_field key_alias)"
STORE_PASSWORD="$(read_field store_password)"
KEY_PASSWORD="$(read_field key_password)"
DROPBOX_KEY="$(read_field dropbox_key)"
REVENUECAT_KEY="$(read_field revenuecat_android_api_key)"

cat > "$WORK_DIR/keystore.properties" <<EOF
storeFile=$WORK_DIR/release.jks
keyAlias=$KEY_ALIAS
storePassword=$STORE_PASSWORD
keyPassword=$KEY_PASSWORD
dropbox_key=$DROPBOX_KEY
revenuecat_android_api_key=$REVENUECAT_KEY
EOF
mv -f "$WORK_DIR/keystore.properties" "$PROPS"

(umask 022 && cd "$REPO_ROOT" && ./gradlew "${TASKS[@]}")
