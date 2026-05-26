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
- the pass evidence is recorded in this document

Use the Maestro CLI and Maestro MCP as needed. Prefer the CLI for repeatable final verification and the MCP for inspection, screenshots, quick iteration, and debugging.

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
| F007 | Stable ids for navigation buttons | In progress | RG-006, settings flows | Drawer menu/settings ids added; home search and overflow button ids added for RG-006/RG-005; add remaining ids as flows need them |
| F008 | Stable ids for drawer/library entries | Passing | REG-104, REG-105 | Added stable ids for timeline/read/bookmarks/categories/feed sources |
| F009 | Stable ids for article rows/actions | In progress | RG-004, RG-005, REG-106, REG-107 | Article row ids added for RG-004; add context and swipe action ids as flows need them |
| F010 | Stable ids for settings rows/actions | In progress | RG-009, RG-010, REG-113-115 | Feed-list, reading behavior, sync/storage, and notifications settings ids added; add other settings ids as flows need them |
| F011 | Stable ids for reader toolbar/actions | Passing | RG-007, REG-110, REG-111 | Reader article, bookmark, browser, font menu, overflow, back, and navigation ids added for RG-007; image viewer ids still deferred to REG-111 |
| F012 | OPML fixture files | Passing | RG-011, REG-119 | `e2e/fixtures/opml/feedflow-valid-opml-smoke.xml`; OPML content uses `.xml` extension so Android DocumentsUI shows it as a document |
| F013 | CSV fixture files | Passing | RG-011, REG-120 | `e2e/fixtures/csv/feedflow-articles-smoke.csv`; `feed_source_id` matches the OPML-imported feed URL hash |
| F014 | `reader-mode` profile validation flow | Passing | RG-007, REG-110, REG-111 | Covered by RG-007 reader-mode flow |
| F015 | `card-layout` and `compact-list` profile validation flows | Passing | REG-108 | Android and iOS card/compact profile flows passed on 2026-05-25 |
| F016 | `external-browser` profile validation flow | Passing | REG-112 | Android and iOS reader-mode override flow passed on 2026-05-25; external OS/browser branches remain in REG-112 |
| F017 | `notifications` profile validation flow | Passing | REG-115 | Android and iOS notifications profile flows passed on 2026-05-25; iOS requests notification permission when the simulator is unset |
| F018 | `android-widget` profile validation flow | Not started | MAN-201, MAN-202 | Android-only |
| F019 | Mock account seed state | Blocked | REG-116, REG-118 | `sync-linked-mock` name exists; provider-specific mock state still needs implementation |
| F020 | `large-content` profile | Blocked | MAN-206 | Not implemented yet |
| F021 | Stable ids and hooks for search controls | Passing | RG-006 | Android search field/filter ids added; iOS uses seeded query/filter hooks because SwiftUI `.searchable` is OS-owned and flaky to type into with Maestro |

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
| REG-102 | Force Add Feed | `empty` | Android, iOS | Not started | Needs failed feed validation fixture/state |
| REG-103 | Feed Suggestions | `empty` | Android, iOS | Not started | May depend on app-provided suggestions |
| REG-104 | Feed Source List Management | `content-rich` | Android, iOS | Not started | Needs category/feed management ids |
| REG-105 | Category Management | `content-rich` | Android, iOS | Not started | Needs category CRUD ids |
| REG-106 | Article Context Menu | `content-rich` | Android, iOS | Passing | Android and iOS `106-article-context-menu.yaml` passed via Maestro CLI on 2026-05-26 |
| REG-107 | Swipe Actions | profile TBD | Android, iOS | Blocked | Needs seeded swipe settings profile or flow setup |
| REG-108 | Feed Layout Matrix | `card-layout`, `compact-list` | Android, iOS | Passing | Android and iOS `108-feed-layout-matrix-card.yaml` and `108-feed-layout-matrix-compact.yaml` passed via Maestro CLI on 2026-05-25 |
| REG-109 | Feed Order And Mark Above Below | `oldest-first` | Android, iOS | Not started | Needs mark-above/below action targeting |
| REG-110 | Reader Fallback | `reader-mode` | Android, iOS | Passing | Android and iOS `110-reader-fallback.yaml` passed via Maestro CLI on 2026-05-26 |
| REG-111 | Reader Image Viewer | `reader-mode` | Android, iOS | Not started | May need screenshot/assertion strategy |
| REG-112 | Link Opening Preferences | `external-browser` | Android, iOS | In progress | Android and iOS `112-link-opening-preferences.yaml` passed via Maestro CLI on 2026-05-25 for the deterministic per-feed Reader Mode override; preferred/external browser branch assertions still need a stable strategy |
| REG-113 | Sync And Storage Settings | `content-rich` | Android, iOS | Passing | Android and iOS `113-sync-storage-settings.yaml` passed via Maestro CLI on 2026-05-26; Android covers the extra sync-period dropdown |
| REG-114 | Appearance Settings | `content-rich` | Android, iOS | Not started | Android OLED/reduce-motion platform-specific |
| REG-115 | Notifications Settings | `notifications` | Android, iOS | Passing | Android and iOS `115-notifications-profile.yaml` passed via Maestro CLI on 2026-05-25 |
| REG-116 | Account List One-Account Constraint | `sync-linked-mock` | Android, iOS | Blocked | Needs mock linked provider state |
| REG-117 | GReader Provider Forms | `empty` | Android, iOS | Blocked | Needs mocked success/error auth path |
| REG-118 | Cloud Provider Mock States | `sync-linked-mock` | Android, iOS | Blocked | Needs mock linked cloud provider state |
| REG-119 | OPML Import Error States | `empty` + fixtures | Android, iOS | Blocked | Needs invalid/partial OPML fixtures |
| REG-120 | CSV Import Export Filters | `content-rich` + fixtures | Android, iOS | Blocked | Needs CSV fixtures and file-picker strategy |

## Manual-Supported Suite

These should not block the normal release gate until they are reliable.

| ID | Test | Profile | Platforms | Status | Notes |
| --- | --- | --- | --- | --- | --- |
| MAN-201 | Android Widget Configuration | `android-widget` | Android | Not started | Android-only |
| MAN-202 | Android Widget Launcher Smoke | `android-widget` | Android | Deferred | Needs widget host/launcher automation strategy |
| MAN-203 | iOS Widget Deep Link | `content-rich` | iOS | Not started | iOS-only |
| MAN-204 | Share Extension Smoke | `empty` | Android, iOS | Deferred | OS share surfaces can be unstable |
| MAN-205 | Tablet And Split Layout | `content-rich` | Android tablet, iPad | Deferred | Requires dedicated devices/simulators |
| MAN-206 | Large Dataset Pagination | `large-content` | Android, iOS | Blocked | Needs `large-content` seed profile |
| MAN-207 | Real Provider Smoke | real providers | Android, iOS | Deferred | Manual/nightly only with staging credentials |
| MAN-208 | Background Sync Timing | profile TBD | Android, iOS | Deferred | OS timing makes this manual/nightly |

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
