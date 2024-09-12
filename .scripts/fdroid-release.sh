#!/bin/sh

# execute command and save to variable
VERSION_CODE=$(git rev-list HEAD --first-parent --count)
VERSION_NAME=$(git describe --tags --abbrev=0  --match "*-android")
#$VERSION_CODE = $(git rev-list HEAD --first-parent --count)

#git describe --tags --abbrev=0  --match "*-android"


# delete "-android" from the version name
VERSION_NAME=${VERSION_NAME%-android}

echo "VERSION_CODE: $VERSION_CODE"
echo "VERSION_NAME: $VERSION_NAME"

# replace VERSION_CODE inside the build.gradle.kts file
sed -i "s/versionName = \"[0-9]\+\.[0-9]\+\.[0-9]\+\"/versionName = \"$VERSION_NAME\"/" ../androidApp/build.gradle.kts

#sed -i "s/versionCode = [0-9]\+/versionCode = $VERSION_CODE/" ../androidApp/build.gradle.kts