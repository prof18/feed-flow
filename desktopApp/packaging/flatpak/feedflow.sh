#!/bin/bash
export JAVA_HOME=/app/jre

# Create config directory if it doesn't exist
mkdir -p "${XDG_CONFIG_HOME:-$HOME/.config}/feedflow"

# Detect UI scale for HiDPI setups inside the sandbox.
detect_ui_scale() {
    local gdk_scale
    local gdk_dpi_scale
    local scale="1"

    gdk_scale="${GDK_SCALE:-}"
    gdk_dpi_scale="${GDK_DPI_SCALE:-}"

    if [[ -n "$gdk_scale" || -n "$gdk_dpi_scale" ]]; then
        scale="${gdk_scale:-1}"
        if [[ -n "$gdk_dpi_scale" ]]; then
            scale=$(awk -v a="$scale" -v b="$gdk_dpi_scale" 'BEGIN { printf "%.3f", a * b }')
        fi
        echo "$scale"
        return
    fi

    if command -v gsettings >/dev/null 2>&1; then
        local gnome_scale
        local text_scale
        gnome_scale=$(gsettings get org.gnome.desktop.interface scaling-factor 2>/dev/null | awk '{for (i=NF; i>=1; i--) if ($i+0==$i) {print $i; exit}}')
        text_scale=$(gsettings get org.gnome.desktop.interface text-scaling-factor 2>/dev/null | awk '{for (i=NF; i>=1; i--) if ($i+0==$i) {print $i; exit}}')

        if [[ -n "$gnome_scale" ]]; then
            scale="$gnome_scale"
        fi
        if [[ -n "$text_scale" && "$text_scale" != "1" ]]; then
            scale=$(awk -v a="$scale" -v b="$text_scale" 'BEGIN { printf "%.3f", a * b }')
        fi
    fi

    echo "$scale"
}

UI_SCALE="$(detect_ui_scale)"
if [[ -z "$UI_SCALE" || "$UI_SCALE" == "0" ]]; then
    UI_SCALE="1"
fi

# Set Java preferences location to use Flatpak's config directory
exec /app/jre/bin/java \
    -Djava.awt.headless=false \
    -Dawt.useSystemAAFontSettings=on \
    -Dswing.aatext=true \
    -Dsun.java2d.uiScale="$UI_SCALE" \
    -Djava.util.prefs.userRoot="${XDG_CONFIG_HOME:-$HOME/.config}/feedflow" \
    -jar /app/lib/feedflow.jar "$@"
