#!/bin/bash
export JAVA_HOME=/app/jre

# Create config directory if it doesn't exist
mkdir -p "${XDG_CONFIG_HOME:-$HOME/.config}/feedflow"

# Set Java preferences location to use Flatpak's config directory
exec /app/jre/bin/java \
    -Djava.awt.headless=false \
    -Dawt.useSystemAAFontSettings=on \
    -Dswing.aatext=true \
    -Djava.util.prefs.userRoot="${XDG_CONFIG_HOME:-$HOME/.config}/feedflow" \
    -jar /app/lib/feedflow.jar "$@"