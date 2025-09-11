#!/bin/bash

# Flatpak build setup script
# This script contains the build commands extracted from the flatpak YAML configuration

set -e

echo "Setting up FeedFlow for Flatpak build..."

# Set build properties
echo "is_release=true" >> desktopApp/src/jvmMain/resources/props.properties
echo "version=1.5.0" >> desktopApp/src/jvmMain/resources/props.properties
echo "flatpak=true" >> desktopApp/src/jvmMain/resources/props.properties

# Update Gradle wrapper distribution URL
sed -i s/distributionUrl.*/distributionUrl=gradle-bin.zip/ gradle/wrapper/gradle-wrapper.properties

# Comment out Android-specific plugins in build.gradle.kts
sed -i \
  -e 's/alias(libs\.plugins\.triplet\.play) apply false/\/\/ &/' \
  -e 's/alias(libs\.plugins\.crashlytics) apply false/\/\/ &/' \
  -e 's/alias(libs\.plugins\.google\.services) apply false/\/\/ &/' \
  build.gradle.kts

# Replace JetBrains JDK 17 toolchain with OpenJDK 21
find . -name "*.gradle.kts" -exec sed -i \
 -e 's/vendor\s*=\s*JvmVendorSpec\.JETBRAINS/\/\/ vendor = JvmVendorSpec.JETBRAINS/' \
 {} \;

# Disable toolchain auto-provisioning
echo "org.gradle.java.installations.auto-detect=false" >> gradle.properties
echo "org.gradle.java.installations.auto-download=false" >> gradle.properties

echo "Flatpak build setup completed!"