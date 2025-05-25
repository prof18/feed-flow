#!/bin/sh -euo pipefail
VERSION_PROPERTIES_FILE="${SRCROOT}/../version.properties"

if [ ! -f "$VERSION_PROPERTIES_FILE" ]; then
    echo "Error: version.properties not found at $VERSION_PROPERTIES_FILE. SRCROOT is $SRCROOT"
    exit 1
fi

MAJOR=$(grep '^MAJOR=' "$VERSION_PROPERTIES_FILE" | cut -d'=' -f2)
MINOR=$(grep '^MINOR=' "$VERSION_PROPERTIES_FILE" | cut -d'=' -f2)
PATCH=$(grep '^PATCH=' "$VERSION_PROPERTIES_FILE" | cut -d'=' -f2)

if [ -z "$MAJOR" ] || [ -z "$MINOR" ] || [ -z "$PATCH" ]; then
    echo "Error: Could not read MAJOR, MINOR, or PATCH from $VERSION_PROPERTIES_FILE"
    exit 1
fi

VERSION_NAME="${MAJOR}.${MINOR}.${PATCH}"
VERSION_CODE=""
if [ -n "${GITHUB_RUN_NUMBER:-}" ]; then
    # CI/Pre-release/Prod/Hotfix build number
    VERSION_CODE=$((GITHUB_RUN_NUMBER + 5000))
else
    # Local build code
    VERSION_CODE="1"
fi

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