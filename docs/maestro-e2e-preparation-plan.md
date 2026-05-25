# Maestro E2E Preparation Plan

This document describes the preparation work needed before writing Maestro tests for FeedFlow on Android and iOS.

The goal is to make release regression testing deterministic, fast, and repeatable without forcing every Maestro flow to create feeds, import files, configure settings, or connect accounts through the UI.

## Goals

- Run Maestro tests against Android and iOS debug builds.
- Start each test from known content and known settings.
- Avoid live RSS feeds, OAuth, background timing, and OS-owned state where possible.
- Keep the app resettable between tests.
- Use the same seed profiles on Android and iOS.
- Keep production builds unaffected.

## Non Goals

- Do not replace unit tests for parser, database, sync conflict, and repository logic.
- Do not depend on live RSS feeds for release-gate tests.
- Do not test real Dropbox, Google Drive, iCloud, FreshRSS, Miniflux, Feedbin, or BazQux auth in the fast gate.
- Do not rely on manual settings verification.

## Current Storage Model

### Main App Database

FeedFlow uses SQLDelight for the main content database.

- Android debug app id: `com.prof18.feedflow.debug`
- Android main database name: `FeedFlowDB-debug`
- Android main database path: `/data/data/com.prof18.feedflow.debug/databases/FeedFlowDB-debug`
- iOS debug bundle id: `com.prof18.feedflow.dev`
- iOS app group: `group.com.prof18.feedflow`
- iOS main database directory: `<app-group-container>/databases`
- iOS main database name: `FeedFlowDB-debug`

Production builds use `FeedFlowDB`.

### Sync Database

Feed sync has a second SQLDelight database.

- Debug sync database name: `FeedFlowFeedSyncDB-debug`
- Production sync database name: `FeedFlowFeedSyncDB`

Most Maestro tests should not need the sync database. Use it only for mocked sync-linked state or sync-specific UI flows.

### Settings

Settings do not live in the main database.

- Android uses `SharedPreferencesSettings` backed by `feedflow.shared.pref`.
- iOS uses `KeychainSettings` through service `FeedFlow2`.
- Android widget settings also use the shared settings object.

Because iOS settings are in Keychain, copying settings files from outside the app is not a good primary strategy.

### Reader Content Cache

Saved reader-mode content is stored as files, not only in the database.

- Android path: app files directory, `articles/<feedItemId>.html`
- iOS path: `<app-group-container>/articles/<feedItemId>.html`

If a test needs offline cached reader content, seed both the database item state and the matching HTML file.

## Recommended Approach

Use a debug-only in-app E2E seeding hook as the primary setup mechanism.

The hook should:

- reset app content
- reset app settings
- insert deterministic database fixtures
- apply deterministic settings profiles
- optionally write cached reader HTML files
- optionally seed mock account states
- report completion with a stable UI marker that Maestro can wait for

This is preferable to copying a prebuilt DB as the only setup method because it also handles Android SharedPreferences and iOS Keychain settings through the app's own repositories.

DB file copying can still be useful later for speed, but it should be a secondary optimization.

## Proposed Seed Entry Point

Add a debug-only deep link:

```text
feedflow://e2e/reset
feedflow://e2e/seed?profile=content-rich
feedflow://e2e/reset-and-seed?profile=content-rich
```

Release builds must ignore these links.

Maestro can call the seed link before a flow:

```yaml
- openLink: feedflow://e2e/reset-and-seed?profile=content-rich
- assertVisible: "E2E seed complete"
```

The completion screen or banner should only exist in debug builds and should expose a stable accessibility identifier.

## Implementation Shape

### Shared Seeding API

Create a shared seeding component that can be called from Android and iOS debug entry points.

Suggested shape:

```kotlin
internal class E2eSeedRunner(
    private val databaseHelper: DatabaseHelper,
    private val settingsRepository: SettingsRepository,
    private val feedAppearanceSettingsRepository: FeedAppearanceSettingsRepository,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    private val accountsRepository: AccountsRepository,
    // Android only when available:
    private val widgetSettingsRepository: WidgetSettingsRepository?,
)
```

The runner should expose:

```kotlin
suspend fun reset()
suspend fun seed(profile: E2eSeedProfile)
suspend fun resetAndSeed(profile: E2eSeedProfile)
```

Keep fixture data in code or JSON, but insert it through repository/database helper APIs where possible. If raw SQL is used, it must use the SQLDelight schema and should be covered by tests.

### Android Entry Point

Preferred options:

1. Add a debug-only activity or deep-link handler under `androidApp/src/debug`.
2. Call the shared `E2eSeedRunner`.
3. Show an `E2E seed complete` screen or return to the app with a visible debug marker.

The release manifest must not expose the debug seed entry point.

### iOS Entry Point

Preferred options:

1. Add a `#if DEBUG` handler for the same E2E deep links.
2. Call into the shared `E2eSeedRunner` from Swift/Kotlin interop.
3. Show an `E2E seed complete` view or debug banner that Maestro can assert.

The release build should not perform any seed operation.

### Reset Requirements

Reset must clear:

- main FeedFlow database data
- sync database data if the profile uses sync state
- app settings
- feed appearance settings
- Android widget settings
- cached reader article files
- pending sync/upload flags
- mock account state

Reset should not require uninstalling the app.

## Seed Profiles

Use named profiles. Do not create one profile per test unless the state is genuinely unique.

### `empty`

Purpose: first-launch and no-data states.

Content:

- no feeds
- no articles
- default settings

### `content-rich`

Purpose: baseline release regression profile.

Content:

- categories: `Technology`, `News`, `Empty Category`
- feeds:
  - `Android Weekly`, category `Technology`
  - `Swift Weekly`, category `Technology`
  - `World News`, category `News`
  - `Uncategorized Feed`, no category
  - `Fetch Failed Feed`, category `News`, `fetch_failed = true`
  - `Hidden Feed`, category `Technology`, hidden from timeline
  - `Pinned Feed`, no category, pinned
- articles:
  - unread newest item
  - read item
  - bookmarked unread item
  - bookmarked read item
  - item with image
  - item without image
  - item with comments URL
  - old item eligible for delete-old flow
  - article with duplicated title/subtitle
  - article matching blocked word
  - reader-mode success fixture
  - reader-mode fallback fixture

Settings:

- default theme
- list layout
- reader mode enabled
- show read articles disabled
- mark read on scroll enabled
- auto-hide read disabled
- newest first

### `card-layout`

Purpose: verify card UI and image rendering.

Based on `content-rich`, with:

- feed layout: `CARD`
- hide images: `false`
- hide date: `false`
- hide description: `false`
- description line limit: `THREE`

### `compact-list`

Purpose: verify dense list settings.

Based on `content-rich`, with:

- feed layout: `LIST`
- hide images: `true`
- hide date: `true`
- hide feed source: `true`
- hide unread dot: `true`
- hide description: `true`
- remove title from description: `true`

### `reader-mode`

Purpose: reader screen, cached content, font size, navigation.

Based on `content-rich`, with:

- reader mode enabled
- save reader content enabled
- prefetch article content disabled
- reader font size set to a non-default value
- cached HTML files for selected reader fixtures

### `external-browser`

Purpose: global and per-feed link opening behavior.

Based on `content-rich`, with:

- global reader mode disabled
- selected feeds using:
  - `DEFAULT`
  - `READER_MODE`
  - `INTERNAL_BROWSER`
  - `PREFERRED_BROWSER`

### `read-behavior`

Purpose: read-state settings.

Based on `content-rich`, with:

- show read articles enabled
- mark read on scroll enabled
- auto-hide read enabled

### `oldest-first`

Purpose: order and mark-above/below behavior.

Based on `content-rich`, with:

- feed order: `OLDEST_FIRST`

### `notifications`

Purpose: notification settings UI.

Based on `content-rich`, with:

- notification mode: `FEED_SOURCE`
- several feeds notification-enabled
- several feeds notification-disabled
- Android sync period set to a non-default value

OS notification permission itself should still be controlled by the device/simulator setup.

### `android-widget`

Purpose: Android widget settings.

Based on `content-rich`, with:

- widget layout: `CARD`
- widget header visible
- widget hide images disabled
- custom widget background color
- custom widget background opacity
- custom widget text color mode
- custom widget font scale

### `sync-linked-mock`

Purpose: accounts UI and one-account constraint.

Based on `content-rich`, with one mock linked provider at a time:

- Dropbox linked
- Google Drive linked
- iCloud linked on iOS
- FreshRSS linked
- Miniflux linked
- Feedbin linked
- BazQux linked

These should be mock states only. Real provider auth belongs outside the fast release gate.

## Settings To Seed

### General Settings

- `FAVOURITE_BROWSER_ID`
- `MARK_FEED_AS_READ_WHEN_SCROLLING`
- `SHOW_READ_ARTICLES_TIMELINE`
- `HIDE_READ_ITEMS`
- `USE_READER_MODE`
- `SAVE_ITEM_CONTENT_ON_OPEN`
- `PREFETCH_ARTICLE_CONTENT`
- `IS_SYNC_UPLOAD_REQUIRED`
- `READER_MODE_FONT_SIZE`
- `AUTO_DELETE_PERIOD`
- `CRASH_REPORTING_ENABLED`
- `SYNC_PERIOD`
- `BACKGROUND_SYNC_WIFI_ONLY`
- `BACKGROUND_SYNC_CHARGING_ONLY`
- `THEME_MODE`
- `REDUCE_MOTION_ENABLED`
- `REFRESH_FEEDS_ON_LAUNCH`
- `SHOW_RSS_PARSING_ERRORS`
- `NOTIFICATION_MODE`

### Feed Appearance Settings

- `FEED_ORDER`
- `REMOVE_TITLE_FROM_DESCRIPTION`
- `HIDE_DESCRIPTION`
- `FEED_LIST_FONT_SCALE_FACTOR`
- `HIDE_IMAGES`
- `HIDE_DATE`
- `LEFT_SWIPE_ACTION`
- `RIGHT_SWIPE_ACTION`
- `DATE_FORMAT`
- `TIME_FORMAT`
- `FEED_LAYOUT`
- `HIDE_UNREAD_DOT`
- `HIDE_UNREAD_COUNT`
- `HIDE_FEED_SOURCE`
- `DESCRIPTION_LINE_LIMIT`

### Android Widget Settings

- `FEED_WIDGET_LAYOUT`
- `WIDGET_SHOW_HEADER`
- `WIDGET_FONT_SCALE_FACTOR`
- `WIDGET_BACKGROUND_COLOR`
- `WIDGET_BACKGROUND_OPACITY_PERCENT`
- `WIDGET_TEXT_COLOR_MODE`
- `WIDGET_HIDE_IMAGES`

### Account Settings

Mock only for fast Maestro tests:

- Dropbox data and timestamps
- Google Drive linked flag and timestamps
- iCloud enabled flag and timestamps
- GReader-style sync URL, username, password, account type, last sync date

## Database Fixture Requirements

Seed data must cover:

- feed source categories
- feed sources
- feed source preferences
- feed items
- read/bookmark state
- blocked words
- FTS search rows through normal insert triggers
- old article deletion candidates
- hidden feed filtering
- pinned feed drawer behavior
- fetch-failed feed warning
- notification-enabled feed preferences

Prefer inserting through existing helper APIs so triggers and mapping stay honest. If fixture creation uses direct SQL, add a validation test that loads the profile through app repositories and asserts expected counts/states.

## External DB Copy Option

If later we want faster setup than the in-app seeder, we can generate a SQLite fixture and copy it before launching the app.

Android sketch:

```bash
adb shell pm clear com.prof18.feedflow.debug
adb push e2e/fixtures/FeedFlowDB-debug /data/local/tmp/FeedFlowDB-debug
adb shell run-as com.prof18.feedflow.debug mkdir -p databases
adb shell run-as com.prof18.feedflow.debug cp /data/local/tmp/FeedFlowDB-debug databases/FeedFlowDB-debug
adb shell run-as com.prof18.feedflow.debug rm -f databases/FeedFlowDB-debug-wal databases/FeedFlowDB-debug-shm
maestro test e2e/maestro/android
```

iOS simulator sketch:

```bash
xcrun simctl terminate booted com.prof18.feedflow.dev
GROUP_DIR=$(xcrun simctl get_app_container booted com.prof18.feedflow.dev group.com.prof18.feedflow)
mkdir -p "$GROUP_DIR/databases"
cp e2e/fixtures/FeedFlowDB-debug "$GROUP_DIR/databases/FeedFlowDB-debug"
rm -f "$GROUP_DIR/databases/FeedFlowDB-debug-wal" "$GROUP_DIR/databases/FeedFlowDB-debug-shm"
maestro test e2e/maestro/ios
```

This does not solve iOS Keychain settings, so it should not be the primary setup mechanism.

## Proposed Repo Layout

```text
e2e/
  maestro/
    android/
    ios/
    shared/
  fixtures/
    articles/
    opml/
    csv/
  scripts/
    run-android.sh
    run-ios.sh
    seed-android-db.sh
    seed-ios-db.sh
```

The exact layout can change during implementation, but the split should keep platform flows separate and shared test data centralized.

## Implementation Phases

### Phase 1: Testability Foundation

- Add debug-only E2E seed deep link.
- Add shared `E2eSeedRunner`.
- Add reset support for app content and settings.
- Add `empty` and `content-rich` profiles.
- Add stable accessibility identifiers for:
  - navigation buttons
  - drawer entries
  - article rows
  - article actions
  - settings rows
  - sheet actions
  - reader toolbar actions
  - search field and filter chips
- Add a visible `E2E seed complete` marker.

### Phase 2: First Maestro Gate

- Add Android and iOS scripts to build/install/run Maestro.
- Add smoke tests using `empty`.
- Add content tests using `content-rich`.
- Add settings UI tests for the most important settings.
- Run locally on one Android target and one iOS simulator.

### Phase 3: Profile Expansion

- Add `card-layout`, `compact-list`, `reader-mode`, `external-browser`, `read-behavior`, and `oldest-first`.
- Add cached article HTML fixtures.
- Add OPML and CSV fixtures.
- Add notification and widget profiles.

### Phase 4: CI Integration

- Run release-gate Maestro tests on release branches or pre-release tags.
- Keep regression tests available locally and optionally nightly.
- Keep provider-auth and OS-widget tests manual or nightly until they are stable.

## Release Gate Recommendation

The fast gate should use:

- `empty`
- `content-rich`
- `reader-mode`
- one settings UI mutation flow
- one import/export flow

The broader gate should add:

- layout profiles
- notification settings
- account mock states
- widget settings
- platform-specific navigation/layout tests

## Definition Of Done

Preparation is complete when:

- Android and iOS can reset and seed from Maestro without manual steps.
- Seed profiles are deterministic and documented.
- Settings can be seeded on both platforms.
- The app exposes stable identifiers for critical UI controls.
- Release-gate Maestro flows can run repeatedly without depending on network content.
- Docs explain how to add a new profile and how to add a new test.
