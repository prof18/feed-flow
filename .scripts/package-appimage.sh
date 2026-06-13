#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

ARCH="${ARCH:-x86_64}"
VERSION="${VERSION:-}"
if [[ -z "$VERSION" ]]; then
  MAJOR="$(grep "^MAJOR=" version.properties | cut -d= -f2)"
  MINOR="$(grep "^MINOR=" version.properties | cut -d= -f2)"
  PATCH="$(grep "^PATCH=" version.properties | cut -d= -f2)"
  VERSION="$MAJOR.$MINOR.$PATCH"
fi

APP_NAME="FeedFlow"
APP_ID="com.prof18.feedflow"
# Java's XToolkit derives the X11 WM_CLASS from the main class, replacing dots
# with dashes (see getCorrectXIDString). GNOME matches the running window to the
# desktop entry via this value, so it must be kept in sync with the desktop
# app's mainClass in desktopApp/build.gradle.kts.
WM_CLASS="com-prof18-feedflow-desktop-MainKt"
STAGE_DIR="${STAGE_DIR:-desktopApp/build/release/main-release/app/$APP_NAME}"
DIST_DIR="${DIST_DIR:-dist}"
APPDIR="$DIST_DIR/$APP_NAME.AppDir"
TOOLS_DIR="${TOOLS_DIR:-.tmp/appimage-tools}"
APPIMAGETOOL_URL="${APPIMAGETOOL_URL:-https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-$ARCH.AppImage}"
APPIMAGETOOL="$TOOLS_DIR/appimagetool-$ARCH.AppImage"
OUTPUT_APPIMAGE="$DIST_DIR/$APP_NAME-$VERSION-$ARCH.AppImage"

if [[ ! -d "$STAGE_DIR" ]]; then
  echo "Missing staged app image: $STAGE_DIR" >&2
  echo "Run ./gradlew --quiet --console=plain :desktopApp:packageReleaseAppImage first." >&2
  exit 1
fi

rm -rf "$APPDIR"
mkdir -p \
  "$APPDIR/usr" \
  "$APPDIR/usr/share/applications" \
  "$APPDIR/usr/share/icons/hicolor/512x512/apps" \
  "$APPDIR/usr/share/metainfo" \
  "$TOOLS_DIR" \
  "$DIST_DIR"
cp -a "$STAGE_DIR"/. "$APPDIR/usr/"

if [[ -x "$APPDIR/usr/bin/$APP_NAME" ]]; then
  LAUNCHER="$APP_NAME"
elif [[ -x "$APPDIR/usr/bin/feedflow" ]]; then
  LAUNCHER="feedflow"
else
  echo "Could not find a launcher in $APPDIR/usr/bin." >&2
  find "$APPDIR/usr/bin" -maxdepth 1 -type f -print >&2 || true
  exit 1
fi

cat > "$APPDIR/$APP_ID.desktop" <<EOF
[Desktop Entry]
Type=Application
Name=FeedFlow
GenericName=RSS Reader
Comment=A minimalistic RSS Reader
Icon=$APP_ID
Exec=AppRun
Terminal=false
StartupNotify=true
Categories=Network;News;
Keywords=RSS;Feed;News;Reader;
StartupWMClass=$WM_CLASS
X-AppImage-Name=FeedFlow
X-AppImage-Version=$VERSION
X-AppImage-Arch=$ARCH
EOF

cp desktopApp/src/jvmMain/resources/icons/icon.png "$APPDIR/$APP_ID.png"
cp "$APPDIR/$APP_ID.desktop" "$APPDIR/usr/share/applications/$APP_ID.desktop"
cp desktopApp/src/jvmMain/resources/icons/icon.png "$APPDIR/usr/share/icons/hicolor/512x512/apps/$APP_ID.png"
cp desktopApp/packaging/flatpak/com.prof18.feedflow.metainfo.xml "$APPDIR/usr/share/metainfo/$APP_ID.appdata.xml"
cat > "$APPDIR/AppRun" <<EOF
#!/bin/sh
set -eu

APPDIR="\$(CDPATH= cd -- "\$(dirname -- "\$0")" && pwd)"
export BAMF_DESKTOP_FILE_HINT="\$APPDIR/usr/share/applications/$APP_ID.desktop"

exec "\$APPDIR/usr/bin/$LAUNCHER" "\$@"
EOF
chmod +x "$APPDIR/AppRun"

if [[ ! -x "$APPIMAGETOOL" ]]; then
  curl -L "$APPIMAGETOOL_URL" -o "$APPIMAGETOOL"
  chmod +x "$APPIMAGETOOL"
fi

rm -f "$OUTPUT_APPIMAGE" "$OUTPUT_APPIMAGE.sha256"
APPIMAGE_EXTRACT_AND_RUN=1 ARCH="$ARCH" "$APPIMAGETOOL" "$APPDIR" "$OUTPUT_APPIMAGE"
chmod +x "$OUTPUT_APPIMAGE"

if command -v sha256sum >/dev/null 2>&1; then
  sha256sum "$OUTPUT_APPIMAGE" > "$OUTPUT_APPIMAGE.sha256"
else
  shasum -a 256 "$OUTPUT_APPIMAGE" > "$OUTPUT_APPIMAGE.sha256"
fi

echo "$OUTPUT_APPIMAGE"
