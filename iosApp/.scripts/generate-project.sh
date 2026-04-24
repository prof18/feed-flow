#!/bin/sh -eu
cd "$(dirname "$0")/.."

CONFIG_DEBUG_PATH="Assets/Config-Debug.xcconfig"
CONFIG_DEBUG_TEMPLATE_PATH="Assets/Config-Debug.xcconfig.template"

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
