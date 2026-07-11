#!/bin/bash

# Flatpak build setup script
# This script contains the build commands extracted from the flatpak YAML configuration

set -e

echo "Setting up FeedFlow for Flatpak build..."

# Set build properties
echo "is_release=true" >> desktopApp/src/jvmMain/resources/props.properties
echo "version=1.15.0" >> desktopApp/src/jvmMain/resources/props.properties
echo "flatpak=true" >> desktopApp/src/jvmMain/resources/props.properties

# Update Gradle wrapper distribution URL
sed 's/distributionUrl.*/distributionUrl=gradle-bin.zip/' gradle/wrapper/gradle-wrapper.properties > gradle/wrapper/gradle-wrapper.properties.tmp &&
  mv gradle/wrapper/gradle-wrapper.properties.tmp gradle/wrapper/gradle-wrapper.properties

# Comment out Android-specific plugins in build.gradle.kts
sed \
  -e 's/alias(libs\.plugins\.android\.application) apply false/\/\/ &/' \
  -e 's/alias(libs\.plugins\.android\.library) apply false/\/\/ &/' \
  -e 's/alias(libs\.plugins\.triplet\.play) apply false/\/\/ &/' \
  -e 's/alias(libs\.plugins\.crashlytics) apply false/\/\/ &/' \
  -e 's/alias(libs\.plugins\.google\.services) apply false/\/\/ &/' \
  -e 's/alias(libs\.plugins\.about\.libraries\.android) apply false/\/\/ &/' \
  build.gradle.kts > build.gradle.kts.tmp &&
  mv build.gradle.kts.tmp build.gradle.kts

# Replace JetBrains JDK 17 toolchain with OpenJDK 21
find . -name "*.gradle.kts" -type f | while read -r file; do
  sed 's/vendor[[:space:]]*=[[:space:]]*JvmVendorSpec\.JETBRAINS/\/\/ vendor = JvmVendorSpec.JETBRAINS/' "$file" > "$file.tmp" &&
    mv "$file.tmp" "$file"
done

# Disable toolchain auto-provisioning
echo "org.gradle.java.installations.auto-detect=false" >> gradle.properties
echo "org.gradle.java.installations.auto-download=false" >> gradle.properties

echo "Flatpak build setup completed!"
