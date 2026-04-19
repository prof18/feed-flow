#!/bin/zsh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TARGET_DIR="$ROOT_DIR/.github/msix-assets"
DEFAULT_ZIP="$ROOT_DIR/tmp/feedflow-windows-msix.zip"
SOURCE_ZIP="${1:-$DEFAULT_ZIP}"
TMP_DIR="$ROOT_DIR/tmp/msix-assets-import"

if [[ ! -f "$SOURCE_ZIP" ]]; then
  echo "Missing Windows MSIX ZIP: $SOURCE_ZIP" >&2
  echo "Export it from $ROOT_DIR/assets/icon-generator/icon-designer.html using 'Export Windows ZIP'." >&2
  exit 1
fi

mkdir -p "$TARGET_DIR" "$TMP_DIR"

unzip -oq "$SOURCE_ZIP" -d "$TMP_DIR"

required_files=(
  "icon.ico"
  "Square44x44Logo.png"
  "Square150x150Logo.png"
  "StoreLogo.png"
  "Square44x44Logo.targetsize-16.png"
  "Square44x44Logo.targetsize-16_altform-unplated.png"
  "Square44x44Logo.targetsize-256.png"
)

for required_file in $required_files; do
  if [[ ! -f "$TMP_DIR/$required_file" ]]; then
    echo "Export ZIP is missing required file: $required_file" >&2
    exit 1
  fi
done

cp "$TMP_DIR"/* "$TARGET_DIR"/
