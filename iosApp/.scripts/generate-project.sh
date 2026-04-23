#!/bin/sh -eu
cd "$(dirname "$0")/.."

PACKAGE_RESOLVED_PATH="FeedFlow.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/Package.resolved"
CONFIG_DEBUG_PATH="Assets/Config-Debug.xcconfig"
CONFIG_DEBUG_TEMPLATE_PATH="Assets/Config-Debug.xcconfig.template"
PACKAGE_RESOLVED_BACKUP=""
cleanup() {
  if [ -n "$PACKAGE_RESOLVED_BACKUP" ] && [ -f "$PACKAGE_RESOLVED_BACKUP" ]; then
    rm -f "$PACKAGE_RESOLVED_BACKUP"
  fi
}
trap cleanup EXIT

if [ -f "$PACKAGE_RESOLVED_PATH" ]; then
  PACKAGE_RESOLVED_BACKUP="$(mktemp)"
  cp "$PACKAGE_RESOLVED_PATH" "$PACKAGE_RESOLVED_BACKUP"
fi

if [ ! -f "$CONFIG_DEBUG_PATH" ]; then
  cp "$CONFIG_DEBUG_TEMPLATE_PATH" "$CONFIG_DEBUG_PATH"
fi

xcodegen generate --spec project.yml --project .

# Fix lastKnownFileType for iOS 26 .icon bundles (XcodeGen doesn't recognize this type)
PBXPROJ="FeedFlow.xcodeproj/project.pbxproj"
/usr/bin/sed -i '' \
  -E 's#(PBXFileReference;[[:space:]]+lastKnownFileType = )folder(;[[:space:]]+name = [A-Za-z]+\.icon;)#\1"folder.iconcomposer.icon"\2#g' \
  "$PBXPROJ"

# Mark app-extension schemes as extension schemes. XcodeGen does not set this
# when the scheme also launches a host app (yonaskolb/XcodeGen#1523).
for scheme in WidgetExtension ShareExtension; do
  SCHEME="FeedFlow.xcodeproj/xcshareddata/xcschemes/${scheme}.xcscheme"
  if [ -f "$SCHEME" ] && ! /usr/bin/grep -q 'wasCreatedForAppExtension' "$SCHEME"; then
    /usr/bin/sed -i '' \
      -E 's#(   version = ")#   wasCreatedForAppExtension = "YES"\'$'\n''\1#' \
      "$SCHEME"
  fi
done

if [ -n "$PACKAGE_RESOLVED_BACKUP" ]; then
  mkdir -p "$(dirname "$PACKAGE_RESOLVED_PATH")"
  cp "$PACKAGE_RESOLVED_BACKUP" "$PACKAGE_RESOLVED_PATH"
fi
