# Maestro E2E Test Catalog

This document lists the Maestro tests FeedFlow should have before using E2E automation as a release regression gate.

Tests are grouped by intent:

- Release Gate: fast pre-release checks that should run locally before handoff and in CI before release
- Regression: broader functional coverage for local validation or scheduled CI
- Manual Supported: OS/provider flows that usually need manual setup, staging credentials, or nightly devices

Unless stated otherwise, each test should run on both Android and iOS.

## Test Conventions

Use seeded profiles from `docs/maestro-e2e-preparation-plan.md`.

Each test should:

- start by resetting/seeding the app
- avoid live network dependencies
- use stable accessibility identifiers where possible
- verify visible user behavior, not internal implementation
- keep one main assertion theme per flow
- avoid depending on state from previous flows

Suggested directory shape:

```text
e2e/maestro/android/release-gate/
e2e/maestro/android/regression/
e2e/maestro/android/manual-supported/
e2e/maestro/ios/release-gate/
e2e/maestro/ios/regression/
e2e/maestro/ios/manual-supported/
e2e/maestro/shared/
```

## Release Gate

### 001 First Launch Empty State

Profile: `empty`

Coverage:

- app launches
- no-feeds empty state appears
- add-feed entry point is visible
- settings entry point is reachable

Assertions:

- no crash
- empty copy is visible
- add feed action opens the add-feed screen or sheet

### 002 Seeded Timeline Loads

Profile: `content-rich`

Coverage:

- timeline loads seeded articles
- unread count appears
- newest article appears first
- hidden feed articles are not visible in Timeline

Assertions:

- known newest article is visible
- hidden-feed article is absent from Timeline
- pinned feed is visible in library/drawer area

### 003 Library Filters

Profile: `content-rich`

Coverage:

- Timeline filter
- Read filter
- Bookmarks filter
- source filter
- category filter
- uncategorized filter

Assertions:

- each filter shows an article unique to that filter
- empty category shows empty state
- returning to Timeline restores the main list

### 004 Article Read And Bookmark State

Profile: `content-rich`

Coverage:

- open article
- article becomes read
- toggle bookmark
- return to list
- verify read/bookmark state

Assertions:

- article moves or changes read indicator as expected
- article appears in Bookmarks after bookmark action
- article appears in Read after being opened

### 005 Mark All Read

Profile: `content-rich`

Coverage:

- mark all read from Timeline menu
- confirm dialog
- unread count updates
- Timeline empty state appears if show-read is disabled

Assertions:

- confirmation dialog appears
- unread count becomes zero or disappears
- Read filter contains the previously unread article

### 006 Search Core

Profile: `content-rich`

Coverage:

- open search
- search all articles
- search read articles
- search bookmarks
- search current feed when launched from a feed context
- no-results state

Assertions:

- known matching article appears
- blocked-word article does not appear
- no-results state appears for missing query

### 007 Reader Mode Core

Profile: `reader-mode`

Coverage:

- open reader-mode article
- cached content loads
- font-size sheet/control opens
- bookmark from reader
- previous and next article navigation
- open in browser action is available

Assertions:

- known reader content appears
- bookmark state changes
- previous/next changes article title

### 008 Feed Edit Core

Profile: `content-rich`

Coverage:

- open feed settings
- edit feed title
- change category
- pin/unpin
- hide from timeline
- save

Assertions:

- renamed feed appears in library
- changed category grouping is visible
- hidden feed disappears from Timeline

### 009 Feed List Settings Persist

Profile: `content-rich`

Coverage:

- open Settings
- change feed layout to card
- hide images
- change feed order
- return to Timeline
- relaunch app

Assertions:

- card layout is visible
- images are hidden
- order persists after relaunch

### 010 Reading Behavior Settings Persist

Profile: `content-rich`

Coverage:

- open Settings
- toggle reader mode
- toggle show read articles
- toggle auto-hide read items
- relaunch app

Assertions:

- settings state persists
- article opening behavior follows selected reader mode
- read articles visibility follows the setting

### 011 Import Export Smoke

Profile: `empty` and fixture files

Coverage:

- import valid OPML
- export OPML
- import CSV article fixture
- export CSV article fixture

Assertions:

- valid OPML creates expected feeds/categories
- export success state appears
- CSV import success state appears

### 012 Blocked Words

Profile: `content-rich`

Coverage:

- open blocked words screen
- add blocked word
- return to Timeline/Search
- remove blocked word

Assertions:

- matching article disappears after adding word
- matching article returns after deleting word

### 013 Relaunch Persistence

Profile: `content-rich`

Coverage:

- mutate read state
- mutate bookmark state
- mutate a setting
- terminate and relaunch app

Assertions:

- read state persists
- bookmark state persists
- setting persists

## Regression Suite

### 101 Add Feed Form Validation

Profile: `empty`

Coverage:

- URL input
- add button disabled/enabled behavior
- invalid URL error
- loading state

Assertions:

- invalid input shows validation or error state
- valid local fixture feed proceeds

### 102 Force Add Feed

Profile: `empty`

Coverage:

- failed feed validation
- force-add warning
- acknowledgement checkbox
- add anyway action

Assertions:

- force-add action is disabled until acknowledged
- feed appears after force-add

### 103 Feed Suggestions

Profile: `empty`

Coverage:

- open feed suggestions
- switch suggestion categories
- add suggested feed
- added state

Assertions:

- category chips change suggestions
- added feed appears in library

### 104 Feed Source List Management

Profile: `content-rich`

Coverage:

- open feed source list
- expand/collapse categories
- rename feed
- delete feed with confirmation
- warning icon for fetch-failed feed

Assertions:

- category expansion persists during screen session
- deleted feed disappears
- fetch-failed feed has warning state

### 105 Category Management

Profile: `content-rich`

Coverage:

- add category
- rename category
- duplicate-name validation
- delete empty category
- delete all feeds in category

Assertions:

- duplicate category is rejected
- category rename updates library grouping
- delete confirmation prevents accidental deletion

### 106 Article Context Menu

Profile: `content-rich`

Coverage:

- open article context menu
- mark read/unread
- bookmark/unbookmark
- mark above read
- mark below read
- share action
- comments action
- feed settings action
- feed website action

Assertions:

- each visible action opens expected UI or mutates visible state

### 107 Swipe Actions

Profiles: `content-rich`, plus seeded swipe action settings

Coverage:

- left swipe read toggle
- right swipe bookmark toggle
- open-in-browser swipe action
- disabled swipe action

Assertions:

- configured action matches swipe direction
- disabled action does not mutate state

### 108 Feed Layout Matrix

Profiles: `card-layout`, `compact-list`

Coverage:

- list layout
- card layout
- hide images
- hide date
- hide description
- hide feed source
- hide unread dot
- description line limit
- date format
- time format

Assertions:

- expected text/media elements are visible or absent
- layout remains usable on compact phone size

### 109 Feed Order And Mark Above Below

Profile: `oldest-first`

Coverage:

- oldest first order
- mark above read
- mark below read
- change to newest first

Assertions:

- visual order changes
- mark-above/below affects the correct side for each sort order

### 110 Reader Fallback

Profile: `reader-mode`

Coverage:

- article with cached reader content
- article without readable extracted content
- fallback web view
- share/bookmark/archive/comments from fallback

Assertions:

- fallback state opens without crashing
- toolbar actions remain available

### 111 Reader Image Viewer

Profile: `reader-mode`

Coverage:

- open reader article with image
- tap image
- full-screen image viewer
- close image viewer

Assertions:

- image viewer opens
- reader state is restored after close

### 112 Link Opening Preferences

Profile: `external-browser`

Coverage:

- global reader mode enabled/disabled
- per-feed default
- per-feed reader mode
- per-feed internal browser
- per-feed preferred browser

Assertions:

- each feed opens using the expected route or OS action

### 113 Sync And Storage Settings

Profile: `content-rich`

Coverage:

- refresh on app launch
- RSS parsing errors toggle
- auto-delete period
- clear downloaded articles
- Android clear image cache
- Android background sync period
- Android Wi-Fi and charging restrictions

Assertions:

- toggles persist
- destructive actions show confirmation
- clear actions show completion or visible state change

### 114 Appearance Settings

Profile: `content-rich`

Coverage:

- system theme
- light theme
- dark theme
- Android OLED theme
- hide unread count
- Android reduce motion

Assertions:

- selected option persists
- unread count visibility follows setting

### 115 Notifications Settings

Profile: `notifications`

Coverage:

- permission missing state
- enable all notifications
- per-feed toggles
- notification grouping mode
- Android notification check frequency
- Android sync restrictions from notifications screen

Assertions:

- all toggles persist
- mode picker changes selected mode
- no-feeds state appears with `empty`

### 116 Account List One-Account Constraint

Profile: `sync-linked-mock`

Coverage:

- account list
- one linked provider
- other providers disabled
- unlink provider

Assertions:

- selected provider has linked/check state
- disabled providers cannot be opened until unlink
- after unlink, providers are available

### 117 GReader Provider Forms

Profile: `empty`

Coverage:

- FreshRSS form
- Miniflux form
- Feedbin form
- BazQux form
- password visibility
- disabled connect button until required fields exist
- mocked success and error states

Assertions:

- form validation works
- success state appears with mock
- auth error state appears with mock

### 118 Cloud Provider Mock States

Profile: `sync-linked-mock`

Coverage:

- Dropbox linked/unlinked state
- Google Drive linked/unlinked state
- iCloud linked/unlinked state on iOS
- backup/upload action visible
- last sync timestamp visible

Assertions:

- linked provider screen shows success content
- unlink returns to account list state

### 119 OPML Import Error States

Profile: `empty`

Coverage:

- invalid OPML
- partial OPML with failed feeds
- retry path

Assertions:

- invalid OPML message appears
- partial import report is visible
- valid feeds from partial import are present

### 120 CSV Import Export Filters

Profile: `content-rich`

Coverage:

- export all
- export read
- export unread
- export bookmarked
- import CSV with known items

Assertions:

- each export path reaches success
- imported articles appear with expected read/bookmark state

## Manual-Supported Suite

### 201 Android Widget Configuration

Platform: Android only

Profile: `android-widget`

Coverage:

- widget settings screen
- preview
- layout setting
- show header
- hide images
- background color picker
- background opacity
- text color mode
- font scale

Assertions:

- preview changes after settings mutate
- invalid color is rejected
- confirm button appears when launched as widget configuration

### 202 Android Widget Launcher Smoke

Platform: Android only

Profile: `android-widget`

Coverage:

- add widget from launcher or widget host if automation supports it
- widget shows seeded article
- widget tap opens article/app

Assertions:

- widget content is non-empty
- tap deep-links into FeedFlow

### 203 iOS Widget Deep Link

Platform: iOS only

Profile: `content-rich`

Coverage:

- open `feedflow://feed/<id>` deep link
- navigate to matching article

Assertions:

- target article opens or is highlighted

### 204 Share Extension Smoke

Platform: iOS and Android where applicable

Profile: `empty`

Coverage:

- send URL to FeedFlow share extension/intent
- add feed screen receives URL

Assertions:

- shared URL is prefilled
- user can save from share flow

### 205 Tablet And Split Layout

Platforms:

- Android tablet/foldable target
- iPad simulator

Profile: `content-rich`

Coverage:

- sidebar visible behavior
- split navigation
- settings presentation
- reader presentation
- rotation

Assertions:

- no overlapping controls
- selected feed/category remains visible
- article route opens correctly

### 206 Large Dataset Pagination

Profile: `large-content`

Coverage:

- 150+ seeded articles
- scroll near bottom
- load next page
- search large data

Assertions:

- pagination loads additional articles
- scroll remains responsive enough for automation

### 207 Real Provider Smoke

Manual-supported or nightly only.

Coverage:

- Dropbox auth
- Google Drive auth
- iCloud availability
- FreshRSS staging server
- Miniflux staging server
- Feedbin staging account
- BazQux staging account

Assertions:

- provider can connect
- initial sync completes
- unlink works

These should not block the normal release gate unless reliable staging credentials and cleanup exist.

### 208 Background Sync Timing

Manual-supported or nightly only.

Coverage:

- Android WorkManager scheduling
- iOS background refresh where possible
- notification delivery

Assertions:

- scheduled work is registered
- app can refresh in background under controlled conditions

## Platform-Specific Coverage Summary

### Android Only

- OLED theme
- reduce motion
- background sync period
- Wi-Fi only sync
- charging only sync
- clear image cache
- widget settings
- widget configuration activity
- Android notification frequency

### iOS Only

- iCloud account state
- app group database path
- Keychain-backed settings
- iPad split layout
- iOS widget deep link
- iOS share extension
- iOS reader/navigation toolbar variants

## Coverage Matrix

| Area | Release Gate | Regression | Manual Supported |
| --- | --- | --- | --- |
| Empty state | yes | yes | no |
| Seeded content | yes | yes | yes |
| Timeline filters | yes | yes | yes |
| Read/bookmark state | yes | yes | yes |
| Search | yes | yes | large data |
| Reader mode | yes | fallback/image | visual/platform |
| Feed CRUD | edit core | full CRUD | no |
| Category CRUD | no | yes | no |
| Settings UI | core | full | visual/platform |
| Seeded settings | yes | yes | yes |
| Import/export | smoke | errors/filters | no |
| Blocked words | yes | yes | no |
| Notifications | no | settings | delivery |
| Accounts | no | mock states | real providers |
| Widgets | no | settings where easy | launcher/widget host |
| Share/deep link | no | no | yes |
| Tablet/iPad | no | no | yes |

## Initial Test Order

Implement in this order:

1. `001_first_launch_empty_state`
2. `002_seeded_timeline_loads`
3. `003_library_filters`
4. `004_article_read_and_bookmark_state`
5. `006_search_core`
6. `007_reader_mode_core`
7. `009_feed_list_settings_persist`
8. `010_reading_behavior_settings_persist`
9. `011_import_export_smoke`
10. `013_relaunch_persistence`

That first batch gives the highest release confidence with the least platform-specific complexity.

## Manual Check Replacement Rule

Manual checking should only remain for OS-owned or provider-owned flows that are not stable under automation.

For app-owned settings:

- seed the setting in at least one profile
- add one UI test that changes it
- verify persistence after relaunch when the setting affects durable behavior

This means settings coverage should not rely on manual release checks.
