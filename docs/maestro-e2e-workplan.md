# Maestro E2E Workplan

This is the trackable implementation list for FeedFlow Maestro tests. It is derived from:

- `docs/maestro-e2e-preparation-plan.md`
- `docs/maestro-e2e-test-catalog.md`
- `docs/maestro-e2e-guide.md`

## Done Rule

A test is done only when:

- the Android flow passes, unless the test is iOS-only
- the iOS flow passes, unless the test is Android-only
- the flow starts from a deterministic seed state
- the flow does not depend on live feeds, real OAuth, or previous app state
- any required stable accessibility ids or seed data are committed with the flow
- the flow does not require production UI or production behavior changes solely for E2E enablement
- the pass evidence is recorded in this document

Use the Maestro CLI and Maestro MCP as needed. Prefer the CLI for repeatable final verification and the MCP for inspection, screenshots, quick iteration, and debugging.

Do not add visible controls, debug-gated production behavior, or provider mock branches to production code solely to make a Maestro flow pass. If a branch cannot be covered with existing app behavior, existing seed/deep-link support, fixtures, launch arguments, or non-production test files, leave it unimplemented and document the blocker here.

## Status Legend

- `Not started`: no flow exists yet
- `In progress`: flow or app support exists, but it is not passing on all required platforms
- `Passing`: done by the rule above
- `Blocked`: needs app support, fixture data, seed profile work, or OS/provider setup first
- `Deferred`: intentionally outside the fast release gate

## Current Commands

Run all local release-gate flows:

```bash
e2e/scripts/run-android.sh
e2e/scripts/run-ios.sh
```

Run one Android flow:

```bash
maestro --platform android test e2e/maestro/android/release-gate/<flow>.yaml
```

Run one iOS flow:

```bash
SIMULATOR_UDID=$(xcrun simctl list devices booted | awk -F '[()]' '/iPhone 17 Pro/ {print $2; exit}')
maestro --platform ios --device "$SIMULATOR_UDID" test e2e/maestro/ios/release-gate/<flow>.yaml
```

## Foundation Work

| ID | Work | Status | Blocks | Notes |
| --- | --- | --- | --- | --- |
| F001 | Debug seed deep links for Android and iOS | Passing | all flows | `feedflow://e2e/reset-and-seed?profile=...` |
| F002 | Shared seed runner with reset support | Passing | all flows | Includes database, settings, accounts reset, cached article cleanup |
| F003 | `empty` profile | Passing | RG-001, import/add-feed flows | Used by first-launch flows |
| F004 | `content-rich` profile | Passing | most release-gate/regression flows | Uses deterministic public feed/article/image data |
| F005 | Android/iOS release-gate wrapper scripts | Passing | release-gate verification | `e2e/scripts/run-android.sh`, `e2e/scripts/run-ios.sh`; flows run sequentially to avoid seed reset races |
| F006 | Stable seed completion marker | Passing | all seeded flows | `E2E seed complete`, `e2e_seed_complete` |
| F007 | Stable ids for navigation buttons | In progress | RG-006, settings flows | Drawer menu/settings ids added; home search and overflow button ids added for RG-006/RG-005; do not add new production ids solely for currently blocked E2E branches |
| F008 | Stable ids for drawer/library entries | Passing | REG-104, REG-105 | Added stable ids for timeline/read/bookmarks/categories/feed sources |
| F009 | Stable ids for article rows/actions | In progress | RG-004, RG-005, REG-106, REG-107 | Article row ids added for RG-004; do not add new production ids solely for currently blocked E2E branches |
| F010 | Stable ids for settings rows/actions | In progress | RG-009, RG-010, REG-113-115, REG-117 | Feed-list, reading behavior, appearance, sync/storage, notifications, and accounts settings ids added; do not add new production ids solely for currently blocked E2E branches |
| F011 | Stable ids for reader toolbar/actions | Passing | RG-007, REG-110, REG-111 | Reader article, bookmark, browser, font menu, overflow, back, navigation, and image viewer ids added |
| F012 | OPML fixture files | Passing | RG-011, REG-119 | `e2e/fixtures/opml/feedflow-valid-opml-smoke.xml` and `e2e/fixtures/opml/zz-feedflow-invalid-opml.xml`; OPML content uses `.xml` extension so Android DocumentsUI shows it as a document |
| F013 | CSV fixture files | Passing | RG-011, REG-120 | `e2e/fixtures/csv/feedflow-articles-smoke.csv`; `feed_source_id` matches the OPML-imported feed URL hash and rows cover unread, read, bookmarked unread, and bookmarked read states |
| F014 | `reader-mode` profile validation flow | Passing | RG-007, REG-110, REG-111 | Covered by RG-007 reader-mode flow |
| F015 | `card-layout` and `compact-list` profile validation flows | Passing | REG-108 | Android and iOS card/compact profile flows passed on 2026-05-25 |
| F016 | `external-browser` profile validation flow | Passing | REG-112 | Android and iOS reader-mode override flow passed on 2026-05-25; external OS/browser branches remain in REG-112 |
| F017 | `notifications` profile validation flow | Passing | REG-115 | Android and iOS notifications profile flows passed on 2026-05-25; iOS requests notification permission when the simulator is unset |
| F018 | `android-widget` profile validation flow | Not started | MAN-201, MAN-202 | Android-only; requires a stable widget-host automation strategy without production UI changes |
| F019 | Mock account seed state | In progress | REG-116, REG-118 | `sync-linked-mock` can seed a linked account via `account=fresh_rss`, `miniflux`, `bazqux`, `feedbin`, `dropbox`, or `icloud`; Google Drive mock auth is not testable without production provider/mock behavior changes |
| F020 | `large-content` profile | Passing | REG-134, MAN-206 | Seeds 55 additional unread articles for pagination beyond the first 40-item page and large-dataset search coverage |
| F021 | Stable ids and hooks for search controls | Passing | RG-006 | Android search field/filter ids added; iOS uses seeded query/filter hooks because SwiftUI `.searchable` is OS-owned and flaky to type into with Maestro |
| F022 | `sync-upload-required` profile | Passing | REG-133 | Seeds content-rich data, a linked mock sync account, and pending upload state so the Home backup action is visible without live credentials |

## Release Gate

Implement these first. The initial target is all release-gate tests passing on Android and iOS.

| ID | Test | Profile | Android | iOS | Status | Evidence / Next Step |
| --- | --- | --- | --- | --- | --- | --- |
| RG-001 | First Launch Empty State | `empty` | Passing | Passing | Passing | `e2e/scripts/run-android.sh` and `e2e/scripts/run-ios.sh` passed on 2026-05-25 |
| RG-002 | Seeded Timeline Loads | `content-rich` | Passing | Passing | Passing | `e2e/scripts/run-android.sh` and `e2e/scripts/run-ios.sh` passed on 2026-05-25 |
| RG-003 | Library Filters | `content-rich` | Passing | Passing | Passing | `e2e/scripts/run-android.sh` and `e2e/scripts/run-ios.sh` passed on 2026-05-25 |
| RG-004 | Article Read And Bookmark State | `content-rich` | Passing | Passing | Passing | `e2e/scripts/run-android.sh` and `e2e/scripts/run-ios.sh` passed on 2026-05-25 |
| RG-005 | Mark All Read | `content-rich` | Passing | Passing | Passing | Android and iOS `005-mark-all-read.yaml` passed on 2026-05-25; `e2e/scripts/run-ios.sh` passed on 2026-05-25; Android wrapper passed RG-005 then stopped in existing RG-010 final hide-read assertion |
| RG-006 | Search Core | `content-rich` | Passing | Passing | Passing | `e2e/scripts/run-android.sh` and `e2e/scripts/run-ios.sh` passed on 2026-05-25; iOS split into search result and bookmark-filter flows |
| RG-007 | Reader Mode Core | `reader-mode` | Passing | Passing | Passing | `e2e/scripts/run-android.sh` and `e2e/scripts/run-ios.sh` passed on 2026-05-25 |
| RG-008 | Feed Edit Core | `content-rich` | Passing | Passing | Passing | Android and iOS `008-feed-edit-core.yaml` passed on 2026-05-25 |
| RG-009 | Feed List Settings Persist | `content-rich` | Passing | Passing | Passing | `e2e/scripts/run-android.sh` and `e2e/scripts/run-ios.sh` passed on 2026-05-25 |
| RG-010 | Reading Behavior Settings Persist | `content-rich` | Passing | Passing | Passing | `e2e/scripts/run-android.sh` and `e2e/scripts/run-ios.sh` passed on 2026-05-25 |
| RG-011 | Import Export Smoke | `empty` + fixtures | Passing | Passing | Passing | Android and iOS `011-import-export-smoke.yaml` passed on 2026-05-25; Android uses pushed Downloads fixtures, iOS uses pushed Files-provider fixtures and the system picker |
| RG-012 | Blocked Words | `content-rich` | Passing | Passing | Passing | Android `012-blocked-words.yaml` passed via Maestro MCP on 2026-05-25; iOS `012-blocked-words.yaml` passed via Maestro CLI on 2026-05-25. Android covers filtering/restoration; iOS covers deterministic add/delete settings coverage |
| RG-013 | Relaunch Persistence | `content-rich` | Passing | Passing | Passing | Android and iOS `013-relaunch-persistence.yaml` passed via Maestro MCP on 2026-05-25 |

## Recommended Release-Gate Order

Work in this order unless a blocker makes the next item inefficient:

1. RG-003 Library Filters
2. RG-004 Article Read And Bookmark State
3. RG-006 Search Core
4. RG-007 Reader Mode Core
5. RG-009 Feed List Settings Persist
6. RG-010 Reading Behavior Settings Persist
7. RG-005 Mark All Read
8. RG-012 Blocked Words
9. RG-013 Relaunch Persistence
10. RG-008 Feed Edit Core
11. RG-011 Import Export Smoke

## Regression Suite

Start these after the release gate is stable.

| ID | Test | Profile | Platforms | Status | Notes |
| --- | --- | --- | --- | --- | --- |
| REG-101 | Add Feed Form Validation | `empty` | Android, iOS | Passing | Android and iOS `101-add-feed-form-validation.yaml` passed via Maestro CLI on 2026-05-26 using deterministic fixture URL validation |
| REG-102 | Force Add Feed | `empty` | Android, iOS | Passing | Android and iOS `102-force-add-feed.yaml` passed via Maestro CLI on 2026-05-26 using DEBUG deterministic force-add failure state |
| REG-103 | Feed Suggestions | `empty` | Android, iOS | Passing | Android and iOS `103-feed-suggestions.yaml` passed via Maestro CLI on 2026-05-26 using app-provided Business suggestions |
| REG-104 | Feed Source List Management | `content-rich` | Android, iOS | Passing | Android and iOS `104-feed-source-list-management.yaml` passed via Maestro CLI on 2026-05-26; Android covers inline rename, both cover expand/collapse, delete confirmation, and fetch-failed warning |
| REG-105 | Category Management | `content-rich` | Android, iOS | Passing | Android `105-category-management.yaml`, iOS `105-category-add-validation.yaml`, and iOS `105-category-management.yaml` passed via Maestro CLI on 2026-05-26 |
| REG-106 | Article Context Menu | `content-rich` | Android, iOS | Passing | Android and iOS `106-article-context-menu.yaml` passed via Maestro CLI on 2026-05-26 |
| REG-107 | Swipe Actions | `swipe-actions`, `swipe-disabled` | Android, iOS | In progress | Android and iOS `107-swipe-actions.yaml` passed via Maestro CLI on 2026-05-26 for left read and right bookmark swipes; Android also covers disabled swipes. iOS disabled and open-in-browser swipe branches are not currently testable without a stable gesture/OS-browser strategy; disabled full-width gestures open the row instead of exposing a no-op action |
| REG-108 | Feed Layout Matrix | `card-layout`, `compact-list` | Android, iOS | Passing | Android and iOS `108-feed-layout-matrix-card.yaml` and `108-feed-layout-matrix-compact.yaml` passed via Maestro CLI on 2026-05-25 |
| REG-109 | Feed Order And Mark Above Below | `oldest-first` | Android, iOS | Passing | Android and iOS `109-feed-order-mark-above-below.yaml` passed via Maestro CLI on 2026-05-26 |
| REG-110 | Reader Fallback | `reader-mode` | Android, iOS | Passing | Android and iOS `110-reader-fallback.yaml` passed via Maestro CLI on 2026-05-26 |
| REG-111 | Reader Image Viewer | `reader-mode` | Android, iOS | Passing | Android and iOS `111-reader-image-viewer.yaml` passed via Maestro CLI on 2026-05-26 |
| REG-112 | Link Opening Preferences | `external-browser` | Android, iOS | In progress | Android and iOS `112-link-opening-preferences.yaml` passed via Maestro CLI on 2026-05-25 for the deterministic per-feed Reader Mode override; preferred/external browser branch assertions remain unimplemented until they can be verified through existing app behavior and stable OS/browser automation |
| REG-113 | Sync And Storage Settings | `content-rich` | Android, iOS | Passing | Android and iOS `113-sync-storage-settings.yaml` passed via Maestro CLI on 2026-05-26; Android covers the extra sync-period dropdown |
| REG-114 | Appearance Settings | `content-rich` | Android, iOS | Passing | Android and iOS `114-appearance-settings.yaml` passed via Maestro CLI on 2026-05-26; Android covers Black theme and reduce motion |
| REG-115 | Notifications Settings | `notifications` | Android, iOS | Passing | Android and iOS `115-notifications-profile.yaml` passed via Maestro CLI on 2026-05-25 |
| REG-116 | Account List One-Account Constraint | `sync-linked-mock` | Android, iOS | Passing | Android and iOS `116-account-list-one-account-constraint.yaml` passed via Maestro CLI on 2026-05-26 using `account=fresh_rss`; covers one-account list constraint, linked FreshRSS state, disconnect, and unlocked provider list |
| REG-117 | GReader Provider Forms | `empty` | Android, iOS | In progress | Android and iOS `117-greader-provider-form-validation.yaml` passed via Maestro CLI on 2026-05-26 for provider navigation and required-field disabled states; Android also covers filled FreshRSS connect enablement and password reveal. Mocked success/error auth paths are not testable without live provider auth or production mock behavior changes; the iOS filled-form branch remains blocked by input instability |
| REG-118 | Cloud Provider Mock States | `sync-linked-mock` | Android, iOS | In progress | Android and iOS `118-cloud-provider-mock-states.yaml` passed via Maestro CLI on 2026-05-26 for seeded Dropbox linked state; iOS also covers seeded iCloud linked state. Google Drive, Dropbox unlink, and provider-specific backup actions are not testable without live provider auth, existing provider mock support, or stable provider-specific non-production hooks. iOS iCloud unlink is covered by REG-138. |
| REG-119 | OPML Import Error States | `empty` + fixtures | Android, iOS | In progress | Android and iOS `119-opml-import-error-states.yaml` passed via Maestro CLI on 2026-05-26 for invalid OPML and choose-another-file recovery; partial failed-feed reporting remains blocked because local OPML imports do not produce `feedSourceWithError` or `notValidFeedSources` entries |
| REG-120 | CSV Import Export Filters | `empty` + fixtures | Android, iOS | In progress | Android and iOS `120-csv-import-article-states.yaml` passed via Maestro CLI on 2026-05-26 for CSV import plus read/bookmark state assertions; export filter paths remain unimplemented until Android and iOS document-save automation is stable without app changes |
| REG-121 | Reading Behavior Secondary Settings | `content-rich` | Android, iOS | Passing | Android and iOS `121-reading-behavior-secondary-settings.yaml` passed via Maestro CLI on 2026-05-26; covers browser row visibility, save-reader-content toggle, prefetch confirmation, and mark-read-when-scrolling toggle. |
| REG-122 | Feed List Detail Controls | `content-rich` | Android, iOS | Passing | Android and iOS `122-feed-list-settings-detail-controls.yaml` passed via Maestro CLI on 2026-05-26; covers text scale visibility, secondary hide toggles, description line limit, date/time format, and swipe action pickers. |
| REG-123 | Sync Storage Advanced Settings | `content-rich` | Android, iOS | Passing | Android and iOS `123-sync-storage-advanced-settings.yaml` passed via Maestro CLI on 2026-05-26; covers clear-downloaded confirmation on both platforms plus Android Wi-Fi/charging restrictions and clear image cache confirmation. |
| REG-124 | Home Overflow Secondary Actions | `content-rich` | Android, iOS | Passing | Android and iOS `124-home-overflow-secondary-actions.yaml` passed via Maestro CLI on 2026-05-26; covers force-refresh item visibility, sort/filter sheet controls, and clear-old-articles confirmation. |
| REG-125 | About And Support Navigation | `content-rich` | Android, iOS | Passing | Android and iOS `125-about-support-navigation.yaml` passed via Maestro CLI on 2026-05-26; covers About & Support, About screen, and open-source licenses navigation without external email/browser actions. |
| REG-126 | Deep Link Routing | `content-rich` | iOS | Passing | iOS `126-deep-link-routing.yaml` passed via Maestro CLI on 2026-05-26 for `feedflow://feed/<id>` article routing into reader mode. Android notification deep links remain blocked from reliable Maestro coverage because production notification routing uses explicit `MainActivity` intents and category/filter links are not exposed through a stable `openLink` path. |
| REG-127 | Notifications Secondary Settings | `notifications` | Android, iOS | Passing | Android and iOS `127-notifications-secondary-settings.yaml` passed via Maestro CLI on 2026-05-26; covers enable-all mutation, per-feed toggle mutation, grouping picker mutation, and Android check-period plus Wi-Fi/charging restrictions from the notifications screen. |
| REG-128 | Notifications Empty State | `empty` | Android, iOS | Passing | Android and iOS `128-notifications-empty-state.yaml` passed via Maestro CLI on 2026-05-26; covers the no-feeds notification settings state. |
| REG-129 | Edit Feed Secondary Options | `notifications` | Android, iOS | Passing | Android and iOS `129-edit-feed-secondary-options.yaml` passed via Maestro CLI on 2026-05-26; covers the article context-menu route to feed settings, edit-feed link-opening preference mutation, notification toggle mutation, and save. |
| REG-130 | Add Feed Secondary Options | `notifications` | Android, iOS | Passing | Android and iOS `130-add-feed-secondary-options.yaml` passed via Maestro CLI on 2026-05-26; covers selecting an existing seeded category and toggling feed notifications on the add-feed form without depending on live feed validation. |
| REG-131 | Home Source Filter Edit Entry | `content-rich` | Android, iOS | Passing | Android and iOS `131-home-source-filter-edit-entry.yaml` passed via Maestro CLI on 2026-05-26; covers selecting a feed-source filter from the drawer and opening that source's edit screen from the Home overflow menu. |
| REG-132 | About Support Secondary Options | `content-rich` | Android, iOS | Passing | Android and iOS `132-about-support-secondary-options.yaml` passed via Maestro CLI on 2026-05-26; covers crash-reporting toggle mutation and support-link visibility without opening external email/browser surfaces. |
| REG-133 | Home Sync Backup Action | `sync-upload-required` | Android, iOS | Passing | Android and iOS `133-home-sync-backup-action.yaml` passed via Maestro CLI on 2026-05-26; covers the pending-upload Home overflow action with a linked mock sync account and no live provider credentials. |
| REG-134 | Large Content Pagination And Search | `large-content` | Android, iOS | In progress | Android `134-large-content-pagination-search.yaml` passed via Maestro CLI on 2026-05-26 for pagination beyond the first 40-item page plus large-seed search. iOS `134-large-content-pagination.yaml` passed via Maestro CLI on 2026-05-26 for pagination; iOS large-search assertion remains blocked because XCTest hierarchy retrieval times out after opening the large search result screen. |
| REG-135 | Feed List Secondary Persistence | `content-rich` | Android, iOS | Passing | Android and iOS `135-feed-list-secondary-persistence.yaml` passed via Maestro CLI on 2026-05-26; covers relaunch persistence for description line limit, date format, time format, and left/right swipe action dropdowns. |
| REG-136 | Sync Storage Secondary Persistence | `content-rich` | Android, iOS | Passing | Android and iOS `136-sync-storage-secondary-persistence.yaml` passed via Maestro CLI on 2026-05-26; covers relaunch persistence for auto-delete period on both platforms and sync period on Android. |
| REG-137 | Reading Behavior Browser Selector Persistence | `content-rich` | Android, iOS | Passing | Android and iOS `137-reading-behavior-browser-selector-persistence.yaml` passed via Maestro CLI on 2026-05-26; covers browser selector mutation and relaunch persistence without asserting OS/browser launch. Android selects the installed Chrome option in the release emulator; iOS selects the always-present Default browser option. |
| REG-138 | iCloud Disconnect | `sync-linked-mock` | iOS | Passing | iOS `138-cloud-provider-disconnect.yaml` passed via Maestro CLI on 2026-05-26 for unlinking a seeded iCloud account and returning to the unlocked add-account state. Android Dropbox disconnect remains blocked because the production unlink path revokes Dropbox access with seeded fake credentials and stays in loading without provider mock support. |

## Manual-Supported Suite

These should not block the normal release gate until they are reliable.

| ID | Test | Profile | Platforms | Status | Notes |
| --- | --- | --- | --- | --- | --- |
| MAN-201 | Android Widget Configuration | `android-widget` | Android | Not started | Android-only; requires stable launcher/widget-host automation without production UI changes |
| MAN-202 | Android Widget Launcher Smoke | `android-widget` | Android | Deferred | Needs widget host/launcher automation strategy |
| MAN-203 | iOS Widget Deep Link | `content-rich` | iOS | Not started | iOS-only; requires stable SpringBoard/widget automation without production UI changes |
| MAN-204 | Share Extension Smoke | `empty` | Android, iOS | Deferred | OS share surfaces can be unstable |
| MAN-205 | Tablet And Split Layout | `content-rich` | Android tablet, iPad | Deferred | Requires dedicated devices/simulators |
| MAN-206 | Large Dataset Pagination | `large-content` | Android, iOS | Passing | Covered by REG-134 with the non-production `large-content` seed profile |
| MAN-207 | Real Provider Smoke | real providers | Android, iOS | Deferred | Manual/nightly only with staging credentials |
| MAN-208 | Background Sync Timing | profile TBD | Android, iOS | Deferred | OS timing makes this manual/nightly |

## Coverage Audit

Last audited: 2026-05-26. This section tracks user-facing Android/iOS features that are uncovered or only partially covered by the current Maestro plan. Do not solve these by adding production UI, visible debug controls, provider mock branches, or debug-gated production behavior solely for E2E. Use existing behavior, fixtures, seed profiles, deep links, launch arguments, or non-production test files.

| Area | Platforms | Current Coverage | Gap / Constraint |
| --- | --- | --- | --- |
| Article context menu full action set | Android, iOS | REG-106 covers bookmark and mark-read mutations; REG-109 covers mark-all-above/below; REG-129 covers open feed settings | Share, share comments, open comments, and open feed website are not asserted. These touch OS share/browser surfaces, so keep the reliable mutation checks separate from any manual/OS-owned checks. |
| Home overflow menu | Android, iOS | RG-005 covers mark-all-read; REG-124 covers force-refresh visibility, sort/view-options sheet controls, and clear-old-articles confirmation; REG-131 covers the source-filter edit entry point; REG-133 covers the pending-upload backup action | No remaining app-owned Home overflow menu gaps are known. |
| Reading behavior settings | Android, iOS | RG-010 covers reader mode, show-read, and auto-hide read; REG-121 covers browser row visibility, save reader content, prefetch article content confirmation, and mark-read-when-scrolling; REG-137 covers browser selector mutation and relaunch persistence | OS browser launch assertions remain uncovered because they require stable browser or OS-level automation. |
| Feed list settings detail matrix | Android, iOS | RG-009/REG-108 cover layout, image visibility, order, and seeded compact/card profiles; REG-122 covers font scale visibility, hide date, hide unread dot, hide feed source, remove duplicated title from description, description line limit, date format, time format, and swipe-action pickers; REG-135 covers relaunch persistence for secondary dropdown values | Relaunch persistence for every secondary toggle is not directly exercised, but the app-owned dropdown persistence path is covered. |
| Sync and storage settings destructive/platform actions | Android, iOS | REG-113 covers refresh-on-launch, RSS parsing errors, auto-delete picker, Android sync period, and clear-downloaded cancel path; REG-123 covers clear-downloaded confirmation, Android clear image cache, and Android Wi-Fi-only and charging-only restrictions; REG-136 covers relaunch persistence for auto-delete and Android sync period dropdowns | Relaunch persistence for every secondary toggle is not directly exercised because checked state is not exposed as stable text, but the app-owned dropdown persistence path is covered. |
| Notifications settings mutations | Android, iOS | REG-115 asserts seeded notification profile and grouping picker; REG-127 covers enable-all mutation, per-feed toggle mutation, grouping mutation, Android check-period mutation, and Android sync restrictions from the notifications screen; REG-128 covers the empty/no-feeds state | Denied-permission/open-settings path remains uncovered because it requires controlling OS notification permission state reliably across runs. |
| Add/edit feed secondary options | Android, iOS | RG-008 covers edit title/category/pin/hide; REG-101-103 cover add/feed suggestions basics; REG-129 covers edit-feed link-opening preference mutation and notification toggle mutation; REG-130 covers add-feed category selection and notification toggle mutation | Live feed validation/OAuth paths remain outside release-gate coverage. |
| Link opening preferences | Android, iOS | REG-112 covers deterministic reader-mode override only | Default/internal/preferred browser paths and OS external-browser assertions remain unimplemented because they require stable browser or OS-level automation. |
| Swipe actions | Android, iOS | REG-107 covers read/bookmark swipes; Android covers disabled swipes | iOS disabled-swipe and open-in-browser swipe branches remain uncovered because the gesture currently opens the row or escapes to OS/browser surfaces. |
| Account provider auth and cloud actions | Android, iOS | REG-116 covers one-account constraint; REG-117 covers required-field validation; REG-118 covers seeded Dropbox and iOS iCloud linked screens; REG-138 covers iOS iCloud unlink | Mocked success/error auth, Google Drive mock state, Android/Dropbox cloud unlink, provider-specific backup/upload actions, and real provider sync remain uncovered without live credentials or existing provider-side mock support. |
| Import/export advanced paths | Android, iOS | RG-011 covers import smoke; REG-119 covers invalid OPML; REG-120 covers CSV import/read/bookmark state | OPML partial-failure reporting and CSV/OPML export filter/save paths remain uncovered until fixture/provider data and document-save automation are stable without app changes. |
| Widgets | Android, iOS | MAN-201-203 are not passing | Android widget settings/configuration/launcher and iOS widget deep links are uncovered; both need stable widget-host/SpringBoard automation. |
| Share extension / share intent | Android, iOS | MAN-204 is deferred | Android `ACTION_SEND` add-feed activity and iOS Share Extension receive/add-feed flows are uncovered because OS share surfaces are unstable. |
| App deep links from notifications/widgets | Android, iOS | REG-126 covers iOS `feedflow://feed/<id>` article routing into reader mode | Android notification deep links, feed-source filter links, category links, and widget deep links remain uncovered. Android notification routing uses explicit `MainActivity` intents, and iOS feed-source/category routing is handled through notification delegate events rather than direct `openLink`, so keep these blocked unless they can be driven through existing OS notification/widget surfaces. |
| Tablet, iPad, split layout, rotation | Android tablet, iPad | MAN-205 is deferred | Sidebar/split navigation, settings presentation, reader presentation, and rotation remain uncovered on dedicated large-screen devices. |
| Large dataset behavior | Android, iOS | REG-134/MAN-206 cover feed pagination beyond the first 40-item page on Android and iOS; REG-134 covers large-dataset search on Android | iOS large-dataset search remains uncovered because Maestro/XCTest hierarchy retrieval times out after opening the large search result screen. Broader performance profiling stays outside Maestro release-gate coverage. |
| About and support | Android, iOS | REG-125 covers About & Support navigation, About screen content, and open-source licenses; REG-132 covers crash-reporting toggle mutation and report-issue visibility | FAQ is feature-flagged off, and external email/browser actions should stay manual unless assertable through existing in-app state. |
| Background sync and notification delivery | Android, iOS | MAN-208 is deferred | WorkManager scheduling, iOS background refresh, notification delivery, and notification deep-link routing remain manual/nightly candidates. |

## Per-Test Work Template

Use this checklist when implementing a test:

```text
Test ID:
Profile:
Platforms:
Flow files:
- Android:
- iOS:

Implementation:
- [ ] Confirm seed data supports the scenario.
- [ ] Add missing stable ids or app test hooks.
- [ ] Write Android flow.
- [ ] Write iOS flow.
- [ ] Run Android flow with Maestro CLI or MCP.
- [ ] Run iOS flow with Maestro CLI or MCP.
- [ ] Run wrapper script if the flow belongs to the release gate.
- [ ] Record pass evidence in this workplan.

Done evidence:
- Android:
- iOS:
```

## Progress Update Rules

When a flow passes:

1. Update its platform cell to `Passing`.
2. If all required platforms pass, update test status to `Passing`.
3. Record the exact command used and the date in the evidence column.
4. If a flow fails because app support is missing, mark the missing support in the foundation table and set the test to `Blocked`.

Do not mark a test as passing based only on code review or syntax checks.
