#!/bin/bash
set -e

echo "Disabling Android targets for Flatpak build..."

# Function to comment out Android-related lines in any file
disable_android_in_file() {
    local file="$1"
    
    # Use awk for more robust pattern matching
    awk '
    BEGIN { in_android_block = 0; brace_count = 0 }
    
    # Match androidTarget lines
    /^[[:space:]]*androidTarget/ {
        print "// " $0
        if ($0 ~ /{[[:space:]]*$/) {
            in_android_block = 1
            brace_count = 1
        }
        next
    }
    
    # Match android { blocks
    /^[[:space:]]*android[[:space:]]*{/ {
        print "// " $0
        in_android_block = 1
        brace_count = 1
        next
    }
    
    # Handle lines inside android blocks
    in_android_block {
        # Special case: looking for opening brace after val declaration
        if (brace_count == -1) {
            if ($0 ~ /{/) {
                brace_count = 1
            } else {
                brace_count = 0
            }
        } else {
            # Count braces normally
            for (i = 1; i <= length($0); i++) {
                char = substr($0, i, 1)
                if (char == "{") brace_count++
                if (char == "}") brace_count--
            }
        }
        
        print "// " $0
        
        # End of block when brace count reaches 0
        if (brace_count <= 0) {
            in_android_block = 0
            brace_count = 0
        }
        next
    }
    
    # Match Android plugin applications
    /alias\(libs\.plugins\.android\./ {
        print "// " $0
        next
    }
    
    # Match kotlin.android plugin
    /alias\(libs\.plugins\.kotlin\.android\)/ {
        print "// " $0
        next
    }
    
    # Match direct android plugin applications
    /apply\("com\.android\./ {
        print "// " $0
        next
    }

    # Match AboutLibraries Android plugin
    /com\.mikepenz\.aboutlibraries\.plugin\.android/ {
        print "// " $0
        next
    }

    # Match Android imports
    /^import.*android\./ {
        print "// " $0
        next
    }
    
    # Match debugImplementation lines (Android-specific)
    /debugImplementation/ {
        print "// " $0
        next
    }
    
    # Match android gradle plugin dependencies
    /libs\.android\.gradle\.plugin/ {
        print "// " $0
        next
    }
    
    # Match LibraryExtension configuration blocks
    /configure<LibraryExtension>/ {
        print "// " $0
        in_android_block = 1
        brace_count = 1
        next
    }
    
    # Match pure Android source sets (not commonJvmAndroid)
    /^[[:space:]]*androidMain[[:space:]]*{/ {
        print "// " $0
        in_android_block = 1
        brace_count = 1
        next
    }
    
    /^[[:space:]]*val androidUnitTest/ {
        print "// " $0
        in_android_block = 1
        # Check if opening brace is on same line
        if ($0 ~ /{/) {
            brace_count = 1
        } else {
            brace_count = -1  # Look for opening brace on next line
        }
        next
    }
    
    /^[[:space:]]*androidUnitTest[[:space:]]*{/ {
        print "// " $0
        in_android_block = 1
        brace_count = 1
        next
    }
    
    # Print all other lines as-is
    { print }
    ' "$file" > "$file.tmp" && mv "$file.tmp" "$file"
}

# Find all build.gradle.kts files and process them
find . -name "build.gradle.kts" -type f | while read -r file; do
    echo "  Processing: $file"
    disable_android_in_file "$file"
done

# Find all Kotlin files in build-logic and process them
find build-logic -name "*.kt" -type f | while read -r file; do
    echo "  Processing: $file"
    disable_android_in_file "$file"
done

# Handle settings.gradle.kts to exclude androidApp module
if [ -f "settings.gradle.kts" ]; then
    echo "  Processing: settings.gradle.kts"
    # Use portable sed syntax that works on both Linux and macOS
    sed 's/^include(":androidApp")/\/\/ include(":androidApp")/' "settings.gradle.kts" > "settings.gradle.kts.tmp" && mv "settings.gradle.kts.tmp" "settings.gradle.kts"
fi

echo "Android targets disabled for Flatpak build!"