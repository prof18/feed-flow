#!/bin/bash
export JAVA_HOME=/app/jre

# Create config directory if it doesn't exist
mkdir -p "${XDG_CONFIG_HOME:-$HOME/.config}/feedflow"

# Detect UI scale factor for HiDPI displays
# Priority: GDK_SCALE > GDK_DPI_SCALE > GNOME text-scaling-factor > default (1.0)
UI_SCALE=""

# Check GDK_SCALE environment variable (integer scaling)
if [ -n "$GDK_SCALE" ]; then
    UI_SCALE="$GDK_SCALE"
# Check GDK_DPI_SCALE environment variable (fractional scaling)
elif [ -n "$GDK_DPI_SCALE" ]; then
    UI_SCALE="$GDK_DPI_SCALE"
# Try to read GNOME settings
elif command -v gsettings >/dev/null 2>&1; then
    # First try the integer scaling-factor
    GNOME_SCALE=$(gsettings get org.gnome.desktop.interface scaling-factor 2>/dev/null | tr -d "'")
    if [ -n "$GNOME_SCALE" ] && [ "$GNOME_SCALE" != "0" ] && [ "$GNOME_SCALE" != "1" ]; then
        UI_SCALE="$GNOME_SCALE"
    else
        # Then try text-scaling-factor for fractional scaling
        TEXT_SCALE=$(gsettings get org.gnome.desktop.interface text-scaling-factor 2>/dev/null)
        if [ -n "$TEXT_SCALE" ] && [ "$TEXT_SCALE" != "1.0" ] && [ "$TEXT_SCALE" != "1" ]; then
            UI_SCALE="$TEXT_SCALE"
        fi
    fi
fi

# Build JVM args
JVM_ARGS=(
    "-Djava.awt.headless=false"
    "-Dawt.useSystemAAFontSettings=on"
    "-Dswing.aatext=true"
    "-Djava.util.prefs.userRoot=${XDG_CONFIG_HOME:-$HOME/.config}/feedflow"
)

# Add UI scale if detected (supports both integer and fractional values)
if [ -n "$UI_SCALE" ] && [ "$UI_SCALE" != "1" ] && [ "$UI_SCALE" != "1.0" ]; then
    # Ensure the scale value is a valid number
    if [[ "$UI_SCALE" =~ ^[0-9]+\.?[0-9]*$ ]]; then
        JVM_ARGS+=("-Dsun.java2d.uiScale=$UI_SCALE")
    fi
fi

# Set Java preferences location to use Flatpak's config directory
exec /app/jre/bin/java "${JVM_ARGS[@]}" -jar /app/lib/feedflow.jar "$@"