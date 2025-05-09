#!/bin/sh

echo "ANDROID"
git describe --tags --abbrev=0  --match "*-android"
echo "Remember to update the release notes for fdroid"
echo "\nIOS"
git describe --tags --abbrev=0  --match "*-ios"
echo "\nDESKTOP"
git describe --tags --abbrev=0  --match "*-desktop"