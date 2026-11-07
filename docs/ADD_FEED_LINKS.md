# Add-feed links

A website or another app can send a feed to FeedFlow with a link. The link opens the Add feed screen with the feed address already filled in. It never adds or verifies the feed by itself: the user still taps Add.

## Link formats

| Shape | Example | Becomes |
|---|---|---|
| App link | `feedflow://add?url=https%3A%2F%2Fexample.com%2Ffeed.xml` | `https://example.com/feed.xml` |
| Feed scheme | `feed://example.com/feed.xml` | `https://example.com/feed.xml` |
| Secure feed scheme | `feeds://example.com/feed.xml` | `https://example.com/feed.xml` |
| Wrapped feed scheme | `feed:https://example.com/feed.xml` (or `feed:http://…`) | the wrapped URL, unchanged |
| Web link | `https://www.feedflow.dev/add/?url=https%3A%2F%2Fexample.com%2Ffeed.xml` | `https://example.com/feed.xml` |

The `url` parameter is the percent-encoded feed address, so feed URLs with their own `?` and `#` survive. `feed://` and `feeds://` always become `https://`; the user can edit the field before adding.

Site owners should publish the web link once it is live (see "Status" below). It opens FeedFlow directly on phones that have it installed (Android App Links, iOS Universal Links) and falls back to a page on www.feedflow.dev everywhere else. That page shows the feed address, a copy button, an "Open in FeedFlow" button for the app link, and the download links. Only `www.feedflow.dev` works: the apex domain redirects to `www`, and Apple refuses association files behind a redirect.

## Status

The custom schemes (`feedflow://add`, `feed://`, `feeds://`) and the fallback page work today. Opening the web link straight in the apps is not live yet. The Android `autoVerify` filter and the association files in `website/static/.well-known/` are in place; Android verifies the link once the website is deployed and a build with the filter is released. iOS declares `applinks:www.feedflow.dev` and verifies it the same way, through Apple's CDN copy of the association file. Until both are deployed and released, the web link shows the fallback page.

## How each platform receives them

| Platform | Registration | Delivery |
|---|---|---|
| Android | Intent filters on `MainActivity` in `androidApp/src/main/AndroidManifest.xml`: `feedflow://add`, `feed`, `feeds`, and, once live, a verified (`autoVerify`) filter for `https://www.feedflow.dev/add` and `/add/` | `MainActivity` pushes an `AddFeed(initialUrl)` route. A link already handled is not replayed when Android restores the activity. |
| iOS | `CFBundleURLTypes` in `iosApp/Info.plist` (`feedflow`, `feed`, `feeds`) and, once live, the `applinks:www.feedflow.dev` associated domain | `onOpenURL` in `FeedFlowApp.swift` sets `AppState.pendingAddFeedUrl`. `RegularView` closes any open sheet, waits for it to dismiss, then opens Add feed pre-filled. |
| macOS | `CFBundleURLTypes` through `macExtraPlistKeys` in `desktopApp/build.gradle.kts` | An Apple Event, received by `Desktop.setOpenURIHandler` in `Main.kt` |
| Windows (MSIX) | `uap3:Protocol` extensions in `.github/msix-manifest-template.xml` | A new process with the link as its first argument |
| Linux (Flatpak) | `MimeType=x-scheme-handler/…` and `Exec=feedflow %u` in `desktopApp/packaging/flatpak/com.prof18.feedflow.desktop` | A new process with the link as its first argument |

On desktop, a new process that finds FeedFlow already running forwards the link over the instance-activation socket (`open-link <link>`, see `InstanceActivation.kt`). Links wait in `AddFeedLinkInbox` until the main window opens the Add feed dialog through `DesktopDialogWindowNavigator.openAddFeed`.

## Rules

- `AddFeedLinkParser` in `core` is the only place that decides whether a link is an add-feed link and which feed URL it carries. Every platform calls it with the raw link.
- Only `http` and `https` feed URLs with a host pass, written with an explicit scheme, at most 2048 characters, and with a host the HTTP client can use (`HttpHostValidator`). Anything else, such as `javascript:` or `file:`, opens nothing.
- Links only pre-fill. Any website or app can fire them, so adding stays a user action.

## Limitations

- MSI/EXE, DEB/RPM and AppImage desktop builds do not register the schemes. Their users copy the address from the fallback page.
- The fallback page is English only.

## Verifying the website association

The association files live in `website/static/.well-known/` and are published with the website. `assetlinks.json` lists two certificate fingerprints for `com.prof18.feedflow`: the Play app-signing key and the key F-Droid signs its builds with. A new signing key on either store means adding its fingerprint there.

- Apple's CDN copy: <https://app-site-association.cdn-apple.com/a/v1/www.feedflow.dev> (it can take up to a day to refresh after a deploy).
- Google's Digital Asset Links check: <https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=https://www.feedflow.dev&relation=delegate_permission/common.handle_all_urls>
- On an Android device with a release build: `adb shell pm get-app-links com.prof18.feedflow` must show `www.feedflow.dev` as verified.

Maestro coverage is REG-189 (`189-add-feed-deep-link.yaml`); the desktop protocol is covered by `AddFeedLinkInboxTest`, `InstanceActivationTest` and `DesktopDialogWindowNavigatorTest`.
