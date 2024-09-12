#!/bin/sh

VERSION_CODE=$(git rev-list HEAD --first-parent --count)
VERSION_NAME=$(git describe --tags --abbrev=0  --match "*-android")

echo "VERSION_CODE: $VERSION_CODE"
echo "VERSION_NAME: $VERSION_NAME"

VERSION_NAME=${VERSION_NAME%-android}

git checkout -b "release-fdroid-$VERSION_NAME"

echo "Replace versionCode and versionName in androidApp/build.gradle.kts"
sed -i '' "s/versionCode = getVersionCode()/versionCode = $VERSION_CODE/" ./androidApp/build.gradle.kts
sed -i '' "s/versionName = getVersionName()/versionName = \"$VERSION_NAME\"/" ./androidApp/build.gradle.kts

git commit -a -m "Release for fdroid: $VERSION_NAME"
git tag "$VERSION_NAME-fdroid"
git push origin "$VERSION_NAME-fdroid"
git checkout main
git branch -D "release-fdroid-$VERSION_NAME"

