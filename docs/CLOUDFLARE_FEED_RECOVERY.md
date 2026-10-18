# Cloudflare feed and reader recovery

Some websites put their feeds or articles behind a Cloudflare challenge. FeedFlow can
recover those requests through an isolated browser session, then reuse that session for
ordinary feed refresh and reader requests to the same website. Recovery is enabled in the
Android and iOS apps, in debug and release builds. Desktop and the iOS extensions (share,
widget) have no recovery binding, so they never record a challenge and never show a prompt.

## Design rules

- **Inert until challenged (D1).** A feed without a recovery record, or an article whose
  anonymous request is not a challenge, keeps the pre-existing fetch path: the same
  parsers and tiers, conditional GET and batch insert, with no new locks and no WebView or
  WebKit calls. The only additions are bookkeeping on an empty table: a refresh reads the
  account's recovery records once, and feed-source writes delete records whose feed no
  longer exists.
- **Last resort (D2).** A challenge is handled like a 403: the parser moves on to the next
  permitted tier. Verification is required only when no tier succeeded and the last
  attempted tier was challenged. A challenge means a `cf-mitigated: challenge` response
  header. A plain 403/429, an HTML page or a Cloudflare `server` header alone is not one.
- **Session first for approved feeds (D3).** A feed whose website was verified fetches
  through its saved website session first. A new challenge on that request asks for
  verification again. Any other session failure (no identity, redirect, HTTP error, I/O,
  timeout) falls back to the normal tiers, and if those succeed the record is removed.
- **Conditional GET kept (D4).** Session requests send the feed's stored validators and
  record the response the same way, so a `304` still counts as not modified.
- **Reader is anonymous first (D5).** Article HTML is fetched anonymously, as before. The
  existing fallback user agent retry now also runs after a challenge response, and response
  charsets are decoded by the shared `ResponseTextDecoder`. Only when that result is a challenge and the article's website has a saved
  session is a session request made. A failed session request keeps the anonymous result.
- **Two modes (D6).** A record is either *verification required* (waiting for the user) or
  *session approved* (a website profile is attached). Clearing a website always retires its
  profile; a new verification creates a new profile identifier.
- **One ticket (D7).** A recovery ticket is the only ownership guard. It is bound to the
  account, and its work is cancelled when the ticket is revoked or the account changes.
  Locks are held only while one feed's or one article's result is committed.
- **Lazy platform work (D8).** Nothing touches WebView profiles, the default WebView user
  agent or `WKWebsiteDataStore` at startup. That work runs only when a verification starts,
  when a session request runs for a recorded feed or article, or when the user clears a
  website. Profile housekeeping (retiring other accounts' profiles, deleting retired stores,
  retiring orphans) runs at those same moments. It skips a profile that a running
  verification has acquired and not yet released, even before its platform store exists.
- **Captured bytes use the normal parsers (D9).** Feed bytes captured in the browser are
  decoded and handed to the RSS parser; article HTML goes to the shared Klead reader
  pipeline.
- **One migration (D10).** Recovery records and profiles live in two tables added by a
  single database migration.
- **Edit keeps its save order (D11).** Feed preferences are saved before the
  account-specific edit, exactly as before.
- **Fetch-failed flag (D12).** Android and Desktop refresh mark a challenged feed as failed,
  as for any failure. iOS serial background refresh keeps its existing behaviour and does
  not set the flag.
- **Platforms (D13).** Only the Android and iOS app targets bind a website session adapter.
- **Relative item links (D14)** are accepted in verified feeds, exactly as in normal feeds.
- **One verification per site (D15).** A successful feed verification also approves every
  other feed of the same account and origin that is waiting for verification, attaching the
  same profile. If the session does not work for one of them, D3 falls back to the normal
  tiers and a new challenge marks it as waiting again.

## Verification flow

1. A feed or article request returns a confirmed challenge (see D2).
2. While the app is in the foreground, an explanation offers **Open website**. Add and Edit
   keep the URL valid instead of showing an invalid-URL error. Reader mode keeps any
   available feed content readable and points to the article-opening settings.
3. Confirming opens a native verification screen for the approved HTTPS origin. The
   browser stays hidden while checking and appears when the website needs interaction.
4. After navigation completes, FeedFlow fetches the requested URL inside the verified
   browser and captures the response. **Check again** retries checks that finish without
   another navigation. A response that is still a challenge, a redirect or too large is
   rejected.
5. The result is committed only while its ticket is still valid. Cancelling or failing
   keeps existing subscriptions, preferences and offline content unchanged.

A challenge found during regular refresh records the feed as waiting for verification. The
app root shows the explanation when the app is in the foreground; background refresh never
opens UI. Cancel hides the prompt for that feed until the app returns from the background.
A server `Retry-After` delay postpones the prompt. Confirming retries the feed.

The feed source list shows **Verify website** for feeds waiting for verification and
**Clear website verification** for feeds with a saved session.

Reader verification replaces the fallback with the extracted article and keeps its website
session, so another article on the same website can reuse it.

## Saved sessions

A website profile belongs to one account and one HTTPS origin. Android uses an isolated
WebView profile and needs WebView's multi-profile and browsing-data-deletion features;
engines without them do not offer verification. iOS uses an isolated persistent
`WKWebsiteDataStore`.

Session requests read the profile's native cookie store and send the browser user agent the
clearance was issued to. Only the `cf_clearance` cookie that matches the exact approved URL
is sent. Session requests never follow redirects, never use a shared cookie jar and never
forward clearance to another origin. Profiles survive app restarts and OS or WebView
updates: housekeeping never retires a profile because the engine version changed, so saved
sessions keep working. Only when the user starts a new verification for that website after
such an update is the old profile replaced by a new one for the current browser.

The browser transfer is
[`feed-recovery-transfer.js`](../androidApp/src/main/assets/feed-recovery-transfer.js).
Android loads it into WebView; iOS bundles the same file and evaluates it in WKWebView. It
performs a same-origin fetch in the verified browser and transfers bounded, ordered byte
chunks to native code.

## Ownership, storage and clearing

A recovered feed's items, metadata and approval are committed together. Reader offline
content is staged beside the previous copy and promoted only while its article and ticket
are current; a failed save keeps the previous copy. Backgrounding, cancelling, or changing
the account or the open article revokes unfinished verification work.

**Clear website verification** retires the profile for every feed using it and deletes its
website data. Existing articles and other websites' sessions are untouched.

Android backup and device transfer exclude `app_webview`, which holds the website session
data. Cookies never enter sync models, backups or logs; logs carry hosts and status codes
only.

Limits are centralized in
[`RecoveryPolicy`](../shared/src/commonMain/kotlin/com/prof18/feedflow/shared/domain/recovery/RecoveryPolicy.kt):

- One visible verification at a time.
- Verification: 120 seconds; browser fetch: 20 seconds; session request: 30 seconds.
- Feed and article bodies: 5 MiB; transfer chunks: 64 KiB.

## Regression checks

| Coverage | What it exercises |
| --- | --- |
| Shared `allTests` | `FeedAttemptTest`, `FeedFetcherRepositoryRecoveryTest`, `FeedSourcesRepositoryLocalChallengeTest`, `ConditionalGetInterceptorChallengeTest`: challenge detection, tiers and the inert path. `RecoveryTicketTest`, `FeedRecoveryRepositoryTest`, `FeedRecoveryCoordinatorTest`, `FeedRecoveryServiceTest`: ownership, guarded commits, one verification per site. `ReaderContentFetcherRecoveryTest`, `ReaderModeRecoveryTest`: anonymous-first reader and staged saves. `AddFeedViewModelRecoveryTest`, `EditFeedViewModelRecoveryTest`, `FeedSourceListViewModelRecoveryTest`, `FeedRefreshVerificationViewModelTest`: UI state. `OkHttpWebsiteSessionHttpTest`, `IosWebsiteSessionHttpTest`, `SerialFeedFetcherRepositoryRecoveryTest`, `CapturedFeedParserTest`, `ResponseTextDecoderTest`, `RecoveryOriginTest`. `FeedRecoveryStorageTest` covers the migration. |
| Android host tests (Robolectric, in `allTests`) | `FeedVerificationLazinessTest` (no WebView work at startup), `WebsiteVerificationUiTest` (explanation dialog and verification screen). |
| Android instrumentation | `AndroidWebsiteIdentityProviderTest`, `AndroidFeedVerificationSessionTest`, `ReaderVerificationUiTest` (including recreation). They need a device and run outside `allTests`. |
| iOS `FeedRecoveryTests` | `WebsiteHTTPTransportTests`, `WebsiteClearanceCookiesTests`, `WebsiteRecoveryLazinessTests` (no WebKit store calls without profiles), `WebsiteBrowserLifecycleTests`, `WebsiteBrowserNavigationPolicyTests`, `WebsiteBrowserNavigationStateTests`, and `FeedRecoveryBrowserProfileTests` (store isolation, clearing and persistence). |
| Maestro REG-182 | Foreground prompt and source-list verification actions through the regular app UI, using a debug-only fixture. |

`FeedVerificationTestActivity` is a minimal, unexported Android debug host for the
production WebView session; it has no UI of its own. See [the testing guide](TESTING.md)
and [the Maestro guide](../e2e/maestro/maestro-e2e-guide.md) for tooling.

```sh
./gradlew --quiet --console=plain detekt allTests
./gradlew --quiet --console=plain :androidApp:connectedGooglePlayDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.prof18.feedflow.android.feedverification
xcodebuild -project iosApp/FeedFlow.xcodeproj -scheme FeedRecoveryTests \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' test -quiet
```

Regenerate the Xcode project after changing `project.yml`. Lease a device or simulator
before instrumentation or simulator tests. After Maestro fixture runs, restore the
development subscriptions as described in the Maestro guide.

## Release limits

| Scenario | Expected behaviour |
| --- | --- |
| Ordinary feed | Add and refresh succeed without a prompt; cold start is unchanged. |
| Challenged Add or Edit | An explanation appears instead of an invalid-URL error. Confirming resumes automatically; failing or cancelling does not partially save the subscription. |
| Challenged refresh | Existing articles stay available. Prompts appear only in the foreground; cancel and retry delays are respected. |
| Saved-session refresh | Foreground and background refresh reuse the website session without opening the browser. |
| Reader | Verification replaces the fallback with reader content and lets another article on the same website reuse the session. |
| Cancel or account change | Late results cannot update a different article or account or replace the previous offline copy. |
| Clear | Every feed sharing that profile loses approval; other websites and existing articles are untouched. |

Candidate live feeds include
[BrooklynVegan's genre feed](https://www.brooklynvegan.com/tags/in-defense-of-the-genre/feed/),
[RSSHub Anthropic Engineering](https://rsshub.app/anthropic/engineering) and
[Neowin](https://www.neowin.net/news/rss). A feed and its articles are separate requests and
may hit different challenges.

Deterministic fixtures do not prove live Cloudflare behaviour. Before release, run trials on
a physical Android device and a physical iPhone with release builds: add each candidate
feed, verify, refresh in the foreground, force-quit and refresh again, let background
refresh run, open an article from the same website, then clear the verification. A cookie,
page title, checkbox or a successful request to another URL is not proof that the requested
content was recovered.
