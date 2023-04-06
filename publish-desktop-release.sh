tag=$(git describe --tags --abbrev=0 --match *-desktop)
version=$(echo "$tag" | sed 's/-desktop$//')
name="FeedFlow-${version}.dmg"
path="desktopApp/build/release/main/dmg/${name}"

xcrun stapler staple $path

git push origin --tags
gh release create $tag $path --notes "Release ${version}"