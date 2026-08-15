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
  "Square150x150Logo.scale-400.png"
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

# The icon designer does not export the Start-menu tile sizes, so derive them
# from the 600x600 master. Without this they would silently keep the previous
# icon after a redesign. The wide tile pads to the manifest's BackgroundColor.
TILE_BACKGROUND="3D4DB7"
MASTER="$TARGET_DIR/Square150x150Logo.scale-400.png"

sips -z 310 310 "$MASTER" --out "$TARGET_DIR/Square310x310Logo.png" >/dev/null
sips -z 71 71 "$MASTER" --out "$TARGET_DIR/Square71x71Logo.png" >/dev/null

sips -z 104 104 "$MASTER" --out "$TMP_DIR/wide-source.png" >/dev/null
sips --padToHeightWidth 150 310 --padColor "$TILE_BACKGROUND" \
  "$TMP_DIR/wide-source.png" --out "$TARGET_DIR/Wide310x150Logo.png" >/dev/null

echo "Generated tile assets: Square310x310Logo.png, Square71x71Logo.png, Wide310x150Logo.png"
