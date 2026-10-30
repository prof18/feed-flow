# Article notifications

FeedFlow checks RSS feeds in the background. Article notifications use the existing refresh work, so delivery timing remains controlled by the operating system. This is background RSS checking, not instant push delivery from YouTube. The feature reuses stored feed item titles, source titles, image URLs, and feed notification settings; it does not scrape pages or call the YouTube API.

## Modes and feed settings

The global notification mode is one of **By feed** (`FEED_SOURCE`), **By category** (`CATEGORY`), **All combined** (`GROUPED`), or **Per article** (`ARTICLE`). The default remains **By feed**, and stored mode values are preserved. Per-article mode still honors each feed's notification enabled setting. Changing the global mode does not change feed settings. The other three modes retain their existing summary notification behavior.

## Per-article batch

A background refresh selects pending articles from notification-enabled feeds, excluding items already marked as notified or blocked by current eligibility rules. It keeps the existing behavior for read items. Turning notifications on for a feed (per feed, for all feeds, or in the feed's settings) marks the articles it already has as notified, and a feed's first successful fetch marks its back-catalog as notified, so only articles that arrive later are announced. "First fetch" means the feed has no `last_sync_timestamp` yet; local fetches and GReader/Feedbin account syncs stamp every subscribed feed after a successful sync, including feeds that returned no articles. Articles a sync account reports as read are marked notified too. This applies to every notification mode. The newest 20 are considered for individual notifications per refresh; the other pending IDs are represented by one overflow notification whose text gives the remainder count for that batch. A later batch replaces that overflow notification; the count is not an accumulated unread total.

Each article notification uses the trimmed article title, falling back to the existing new-articles title if it is blank, and the feed's source title as its body. It uses the stored image URL when available. Images must be valid HTTP(S) URLs. Each platform allows up to one second per image and five seconds of image preparation per batch; after the budget is used, subsequent notifications are sent without images. These timers bound requested image work; native decoding and cancellation cleanup are cooperative, so they are not hard real-time deadlines. Android asks Coil for a bitmap bounded to 1024 pixels. iOS requests a Nuke thumbnail bounded to 1024 pixels, encodes it as JPEG at quality 0.8, and omits attachments larger than 2 MiB. Invalid, missing, failed, or timed-out images fall back to text. The iOS attachment helper removes staged files after submission and prunes its own orphan files older than 24 hours when it starts.

Article notifications use stable identifiers derived from the full item ID. Repeating a submission before acknowledgement updates the same platform notification request/key rather than creating a second entry. The sender acknowledges each article ID only after native submission succeeds; the overflow sender acknowledges only the remainder IDs it represents. Acknowledgement updates notification-pending state, not read state. Android keeps its active notifications under a conservative 40-key soft limit (the system drops posts beyond 50 per app): it posts only the newest articles that fit and folds the rest into the overflow notification, whose key costs no extra slot when it is already showing. It defers the batch only when not even the overflow fits. These rules reduce duplicates and avoid acknowledging known failed submissions, but a process crash between OS submission and database acknowledgement can still cause a retry. Submission success does not guarantee visible delivery.

The first successful submission in a batch requests the existing sound; later submissions in that batch are silent. Permission, channel settings, Focus, OS scheduling, tray limits, and platform presentation remain controlled by the operating system. Android may visually group separate notifications automatically, so individual payloads do not guarantee physically separate presentation.

## Taps and opening articles

Tapping an article opens FeedFlow with a link containing that article's encoded item ID. The app then uses the configured article-opening behavior, including feed-specific overrides. URL-less items open from their feed-provided content. If reader mode cannot handle a URL, including YouTube links, existing opening rules fall back to the configured browser. The notification work does not make a new YouTube lookup.

The overflow notification has no article URL. On Android it uses the existing main-activity intent flags, which recreate the main task. On iOS it activates the app without requesting a specific route. Neither platform's overflow tap acknowledges pending items; acknowledgement happens after the overflow notification request is accepted.

## Verification

Focused automated tests check the shared query, cap, remainder and exact-ID acknowledgement; native payloads, image fallback, capacity or submission failures, and sound selection. For example:

```sh
./gradlew --quiet --console=plain :shared:jvmTest --tests 'com.prof18.feedflow.shared.domain.notification.ArticleNotificationRepositoryTest'
./gradlew --quiet --console=plain :androidApp:testGooglePlayDebugUnitTest --tests 'com.prof18.feedflow.android.notifications.AndroidNotifierTest' --tests 'com.prof18.feedflow.android.notifications.AndroidNotifierRegressionTest' --tests 'com.prof18.feedflow.android.notifications.AndroidNotificationImageLoaderTest'
(cd iosApp && ./.scripts/generate-project.sh)
xcodebuild -project iosApp/FeedFlow.xcodeproj -scheme NotificationTests -destination "$SIMULATOR_DESTINATION" test -quiet
```

Set `SIMULATOR_DESTINATION` to an available simulator, for example `platform=iOS Simulator,name=iPhone 17 Pro`. If that simulator is absent, choose an available leased simulator and use its exact Xcode destination; the current implementation validation uses iPhone 17 as the approved substitute. The standalone `NotificationTests` scheme covers the native sender, image attachment loader, and queued deep-link store. Shared Android host coverage for worker integration is in `FeedDownloadWorkerTest` and runs with `./gradlew --quiet --console=plain :shared:testAndroidHostTest`.

Native sender tests do not exercise full app routing or visible OS presentation. The named Android and iOS Maestro notification and deep-link flows in the [E2E catalog](../e2e/maestro/maestro-e2e-tests.md) cover app-level routing and settings behavior. Human validation on a phone is still needed to confirm actual OS presentation, image display, sound, warm and cold taps, and browser or YouTube handoff on the user's installed setup. Record the device and OS and treat background timing as OS-controlled.

The repository gate is `./gradlew --quiet --console=plain detekt allTests`, plus the Android and Desktop builds, iOS formatting, iOS `FeedFlow` build, and the named Maestro flows described in [AGENTS.md](../AGENTS.md). The standalone notification test command above supplements that gate. Passing a sender test or gate does not establish that the OS displayed a notification.
