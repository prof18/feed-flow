#!/bin/bash
set -e

echo "Disabling Android targets for Flatpak build..."

# Files to modify
FILES=(
    "shared/build.gradle.kts"
    "sharedUI/build.gradle.kts"
    "core/build.gradle.kts"
    "database/build.gradle.kts"
    "i18n/build.gradle.kts"
    "feedSync/dropbox/build.gradle.kts"
    "feedSync/database/build.gradle.kts"
    "feedSync/greader/build.gradle.kts"
    "feedSync/icloud/build.gradle.kts"
    "feedSync/networkcore/build.gradle.kts"
)

for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        # Comment out androidTarget and android{} blocks
        sed -i '
            /^\s*androidTarget/s/^/\/\/ /
            /^\s*android\s*{/,/^\s*}/ {
                s/^/\/\/ /
            }
        ' "$file"
        
        echo "  Modified: $file"
    else
        echo "  Warning: $file not found"
    fi
done

# Comment out android plugins in root build.gradle.kts
if [ -f "build.gradle.kts" ]; then
    sed -i '
        /alias(libs\.plugins\.android\.application)/s/^/\/\/ /
        /alias(libs\.plugins\.android\.library)/s/^/\/\/ /
        /alias(libs\.plugins\.kotlin\.android)/s/^/\/\/ /
    ' "build.gradle.kts"
    echo "  Modified: build.gradle.kts"
fi

echo "Android targets disabled for Flatpak build!"