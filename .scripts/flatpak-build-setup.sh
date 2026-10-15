#!/bin/bash

# Flatpak build setup script
# This script contains the build commands extracted from the flatpak YAML configuration

set -e

echo "Setting up FeedFlow for Flatpak build..."

# Offline builds need both installed compilers before any checkout mutation.
flatpak_sdk_root="${FEEDFLOW_FLATPAK_SDK_ROOT:-/usr/lib/sdk}"
flatpak_jdk21="$flatpak_sdk_root/openjdk21/jvm/openjdk-21"
flatpak_jdk25="$flatpak_sdk_root/openjdk25/jvm/openjdk-25"
for flatpak_major in 21 25; do
  flatpak_compiler="$flatpak_sdk_root/openjdk$flatpak_major/jvm/openjdk-$flatpak_major/bin/javac"
  if [[ ! -x "$flatpak_compiler" ]]; then
    echo "Missing Flatpak JDK $flatpak_major compiler: $flatpak_compiler" >&2
    exit 1
  fi
  flatpak_javac_version="$("$flatpak_compiler" -version 2>&1)"
  if [[ ! "$flatpak_javac_version" =~ ^javac\ $flatpak_major([.+[:space:]]|$) ]]; then
    echo "Expected Flatpak JDK $flatpak_major; detected: $flatpak_javac_version" >&2
    exit 1
  fi
done

flatpak_version_major="$(awk -F= '$1 == "MAJOR" {print $2}' version.properties)"
flatpak_version_minor="$(awk -F= '$1 == "MINOR" {print $2}' version.properties)"
flatpak_version_patch="$(awk -F= '$1 == "PATCH" {print $2}' version.properties)"
flatpak_app_version="$flatpak_version_major.$flatpak_version_minor.$flatpak_version_patch"
if [[ ! "$flatpak_app_version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Invalid app version in version.properties" >&2
  exit 1
fi

# Set build properties
echo "is_release=true" >> desktopApp/src/jvmMain/resources/props.properties
echo "version=$flatpak_app_version" >> desktopApp/src/jvmMain/resources/props.properties
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

# Use the SDK's installed toolchains rather than requiring JetBrains as the vendor.
find . -name "*.gradle.kts" -type f | while read -r file; do
  sed 's/vendor[[:space:]]*=[[:space:]]*JvmVendorSpec\.JETBRAINS/\/\/ vendor = JvmVendorSpec.JETBRAINS/' "$file" > "$file.tmp" &&
    mv "$file.tmp" "$file"
done

# Disable toolchain auto-provisioning
echo "org.gradle.java.installations.auto-detect=false" >> gradle.properties
echo "org.gradle.java.installations.auto-download=false" >> gradle.properties
echo "org.gradle.java.installations.paths=$flatpak_jdk21,$flatpak_jdk25" >> gradle.properties

echo "Flatpak build setup completed!"
