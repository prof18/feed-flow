#!/bin/sh

echo "ANDROID"
git describe --tags --abbrev=0  --match "*-android"
echo "\nIOS"
git describe --tags --abbrev=0  --match "*-ios"
echo "\nDESKTOP"
git describe --tags --abbrev=0  --match "*-desktop"
echo "Remember to upgrade the windows update UUID -> https://www.guidgen.com/"