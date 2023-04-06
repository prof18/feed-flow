#!/bin/sh -euo pipefail

tag=$(git describe --tags --abbrev=0 --match *-ios)
VERSION_NAME=$(echo "$tag" | sed 's/-ios//')

VERSION_CODE=$(git rev-list HEAD --first-parent --count)

# Prepare directory / file where the generated value will be written.
mkdir -p "${SRCROOT}"/Plist
touch "${SRCROOT}"/Plist/Prefix

echo "Checking folder"
ls "${SRCROOT}"/Plist

# Write content to a file
cat <<EOF > "${SRCROOT}"/Plist/Prefix
#define VERSION_NAME ${VERSION_NAME}
#define VERSION_CODE ${VERSION_CODE}
EOF