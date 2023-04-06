./gradlew desktopTest

# ./gradlew packageDmg

tag=$(git describe --tags --abbrev=0 --match *-desktop)
version=$(echo "$tag" | sed 's/-desktop$//')
name="FeedFlow-${version}.dmg"

path="desktopApp/build/release/main/dmg/${name}"

#xcrun notarytool submit ${path} --apple-id mgp.dev.studio@gmail.com --password @keychain:NOTARIZATION_PASSWORD  --wait

#xcrun stapler staple $path
gh release create $tag $path