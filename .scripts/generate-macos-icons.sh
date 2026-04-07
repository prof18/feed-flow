#!/usr/bin/env bash
# Regenerate macOS Assets.car (Liquid Glass) + .icns from layered .icon bundles.
#
# Sources:
#   desktopApp/macos-icon/release/AppIcon.icon
#   desktopApp/macos-icon/debug/AppIcon.icon
#
# Outputs:
#   desktopApp/macos-icon/<variant>/Assets.car
#   desktopApp/src/jvmMain/resources/icons/icon.icns         (release)
#   desktopApp/src/jvmMain/resources/icons-debug/icon.icns   (debug)
#
# The bundle must be named "AppIcon.icon" so actool emits a matching AppIcon.icns
# alongside Assets.car (driven by --app-icon AppIcon).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ICON_ROOT="$REPO_ROOT/desktopApp/macos-icon"
RES_RELEASE="$REPO_ROOT/desktopApp/src/jvmMain/resources/icons"
RES_DEBUG="$REPO_ROOT/desktopApp/src/jvmMain/resources/icons-debug"

compile_variant() {
    local variant="$1"
    local icns_out_dir="$2"
    local src="$ICON_ROOT/$variant/AppIcon.icon"

    if [[ ! -d "$src" ]]; then
        echo "Skipping $variant: $src not found"
        return 0
    fi

    echo "==> Compiling $variant icon bundle"
    local tmp
    tmp="$(mktemp -d)"
    trap 'rm -rf "$tmp"' RETURN

    xcrun actool --compile "$tmp" \
        --platform macosx \
        --minimum-deployment-target 12.0 \
        --app-icon AppIcon \
        --output-partial-info-plist "$tmp/Info.plist" \
        "$src" >/dev/null

    if [[ ! -f "$tmp/Assets.car" ]]; then
        echo "ERROR: actool did not emit Assets.car for $variant" >&2
        return 1
    fi
    if [[ ! -f "$tmp/AppIcon.icns" ]]; then
        echo "ERROR: actool did not emit AppIcon.icns for $variant" >&2
        return 1
    fi

    mkdir -p "$ICON_ROOT/$variant" "$icns_out_dir"
    cp "$tmp/Assets.car" "$ICON_ROOT/$variant/Assets.car"
    cp "$tmp/AppIcon.icns" "$icns_out_dir/icon.icns"

    echo "    Assets.car -> $ICON_ROOT/$variant/Assets.car"
    echo "    icon.icns  -> $icns_out_dir/icon.icns"
}

compile_variant release "$RES_RELEASE"
compile_variant debug "$RES_DEBUG"

echo "Done."
