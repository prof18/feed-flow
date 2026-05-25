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

Run all local P0 flows:

```bash
e2e/scripts/run-android.sh
e2e/scripts/run-ios.sh
```

Run one Android flow:

```bash
maestro --platform android test e2e/maestro/android/p0/<flow>.yaml
```

Run one iOS flow:

```bash
SIMULATOR_UDID=$(xcrun simctl list devices booted | awk -F '[()]' '/iPhone 17 Pro/ {print $2; exit}')
maestro --platform ios --device "$SIMULATOR_UDID" test e2e/maestro/ios/p0/<flow>.yaml
```

## Foundation Work

| ID | Work | Status | Blocks | Notes |
| --- | --- | --- | --- | --- |
| F001 | Debug seed deep links for Android and iOS | Passing | all flows | `feedflow://e2e/reset-and-seed?profile=...` |
| F002 | Shared seed runner with reset support | Passing | all flows | Includes database, settings, accounts reset, cached article cleanup |
| F003 | `empty` profile | Passing | P0 001, import/add-feed flows | Used by first-launch flows |
| F004 | `content-rich` profile | Passing | most P0/P1 flows | Uses deterministic public feed/article/image data |
| F005 | Android/iOS P0 wrapper scripts | Passing | P0 verification | `e2e/scripts/run-android.sh`, `e2e/scripts/run-ios.sh` |
| F006 | Stable seed completion marker | Passing | all seeded flows | `E2E seed complete`, `e2e_seed_complete` |
| F007 | Stable ids for navigation buttons | In progress | P0 006, settings flows | Drawer menu id added for P0 003; add remaining ids as flows need them |
| F008 | Stable ids for drawer/library entries | Passing | P1 104, P1 105 | Added stable ids for timeline/read/bookmarks/categories/feed sources |
| F009 | Stable ids for article rows/actions | Not started | P0 004, P0 005, P1 106, P1 107 | Add as flows need them |
| F010 | Stable ids for settings rows/actions | Not started | P0 009, P0 010, P1 113-115 | Add as flows need them |
| F011 | Stable ids for reader toolbar/actions | Not started | P0 007, P1 110, P1 111 | Add as flows need them |
| F012 | OPML fixture files | Not started | P0 011, P1 119 | Put under `e2e/fixtures/opml/` |
| F013 | CSV fixture files | Not started | P0 011, P1 120 | Put under `e2e/fixtures/csv/` |
| F014 | `reader-mode` profile validation flow | Not started | P0 007, P1 110, P1 111 | Profile exists; needs flow coverage |
| F015 | `card-layout` and `compact-list` profile validation flows | Not started | P1 108 | Profiles exist; needs flow coverage |
| F016 | `external-browser` profile validation flow | Not started | P1 112 | Profile exists; may need platform-specific handling |
| F017 | `notifications` profile validation flow | Not started | P1 115 | Profile exists; OS permission state still separate |
| F018 | `android-widget` profile validation flow | Not started | P2 201, P2 202 | Android-only |
| F019 | Mock account seed state | Blocked | P1 116, P1 118 | `sync-linked-mock` name exists; provider-specific mock state still needs implementation |
| F020 | `large-content` profile | Blocked | P2 206 | Not implemented yet |

## P0 Release Gate

Implement these first. The initial target is all P0 tests passing on Android and iOS.

| ID | Test | Profile | Android | iOS | Status | Evidence / Next Step |
| --- | --- | --- | --- | --- | --- | --- |
| P0-001 | First Launch Empty State | `empty` | Passing | Passing | Passing | `e2e/scripts/run-android.sh` and `e2e/scripts/run-ios.sh` passed on 2026-05-25 |
| P0-002 | Seeded Timeline Loads | `content-rich` | Passing | Passing | Passing | `e2e/scripts/run-android.sh` and `e2e/scripts/run-ios.sh` passed on 2026-05-25 |
| P0-003 | Library Filters | `content-rich` | Passing | Passing | Passing | `e2e/scripts/run-android.sh` and `e2e/scripts/run-ios.sh` passed on 2026-05-25 |
| P0-004 | Article Read And Bookmark State | `content-rich` | Not started | Not started | Not started | Needs stable article row/action targeting |
| P0-005 | Mark All Read | `content-rich` | Not started | Not started | Not started | Needs timeline menu/confirm dialog targeting |
| P0-006 | Search Core | `content-rich` | Not started | Not started | Not started | Needs search field/filter targeting |
| P0-007 | Reader Mode Core | `reader-mode` | Not started | Not started | Not started | Needs reader toolbar/action targeting |
| P0-008 | Feed Edit Core | `content-rich` | Not started | Not started | Not started | Needs feed settings/edit targeting |
| P0-009 | Feed List Settings Persist | `content-rich` | Not started | Not started | Not started | Needs settings row targeting |
| P0-010 | Reading Behavior Settings Persist | `content-rich` | Not started | Not started | Not started | Needs settings row targeting |
| P0-011 | Import Export Smoke | `empty` + fixtures | Blocked | Blocked | Blocked | Needs OPML/CSV fixtures and file-picker strategy |
| P0-012 | Blocked Words | `content-rich` | Not started | Not started | Not started | Needs blocked-word screen targeting |
| P0-013 | Relaunch Persistence | `content-rich` | Not started | Not started | Not started | Should reuse helpers from P0-004/P0-009 |

## Recommended P0 Order

Work in this order unless a blocker makes the next item inefficient:

1. P0-003 Library Filters
2. P0-004 Article Read And Bookmark State
3. P0-006 Search Core
4. P0-007 Reader Mode Core
5. P0-009 Feed List Settings Persist
6. P0-010 Reading Behavior Settings Persist
7. P0-005 Mark All Read
8. P0-012 Blocked Words
9. P0-013 Relaunch Persistence
10. P0-008 Feed Edit Core
11. P0-011 Import Export Smoke

## P1 Broader Regression Suite

Start these after the P0 gate is stable.

| ID | Test | Profile | Platforms | Status | Notes |
| --- | --- | --- | --- | --- | --- |
| P1-101 | Add Feed Form Validation | `empty` | Android, iOS | Not started | Prefer local fixture URL, not live RSS |
| P1-102 | Force Add Feed | `empty` | Android, iOS | Not started | Needs failed feed validation fixture/state |
| P1-103 | Feed Suggestions | `empty` | Android, iOS | Not started | May depend on app-provided suggestions |
| P1-104 | Feed Source List Management | `content-rich` | Android, iOS | Not started | Needs category/feed management ids |
| P1-105 | Category Management | `content-rich` | Android, iOS | Not started | Needs category CRUD ids |
| P1-106 | Article Context Menu | `content-rich` | Android, iOS | Not started | Reuse article action ids from P0 |
| P1-107 | Swipe Actions | profile TBD | Android, iOS | Blocked | Needs seeded swipe settings profile or flow setup |
| P1-108 | Feed Layout Matrix | `card-layout`, `compact-list` | Android, iOS | Not started | Good candidate for visual screenshots |
| P1-109 | Feed Order And Mark Above Below | `oldest-first` | Android, iOS | Not started | Needs mark-above/below action targeting |
| P1-110 | Reader Fallback | `reader-mode` | Android, iOS | Not started | Reuse reader ids from P0-007 |
| P1-111 | Reader Image Viewer | `reader-mode` | Android, iOS | Not started | May need screenshot/assertion strategy |
| P1-112 | Link Opening Preferences | `external-browser` | Android, iOS | Not started | Likely platform-specific assertions |
| P1-113 | Sync And Storage Settings | `content-rich` | Android, iOS | Not started | Android has extra settings |
| P1-114 | Appearance Settings | `content-rich` | Android, iOS | Not started | Android OLED/reduce-motion platform-specific |
| P1-115 | Notifications Settings | `notifications` | Android, iOS | Not started | OS permission state must be controlled |
| P1-116 | Account List One-Account Constraint | `sync-linked-mock` | Android, iOS | Blocked | Needs mock linked provider state |
| P1-117 | GReader Provider Forms | `empty` | Android, iOS | Blocked | Needs mocked success/error auth path |
| P1-118 | Cloud Provider Mock States | `sync-linked-mock` | Android, iOS | Blocked | Needs mock linked cloud provider state |
| P1-119 | OPML Import Error States | `empty` + fixtures | Android, iOS | Blocked | Needs invalid/partial OPML fixtures |
| P1-120 | CSV Import Export Filters | `content-rich` + fixtures | Android, iOS | Blocked | Needs CSV fixtures and file-picker strategy |

## P2 Nightly Or Manual-Supported Suite

These should not block the normal release gate until they are reliable.

| ID | Test | Profile | Platforms | Status | Notes |
| --- | --- | --- | --- | --- | --- |
| P2-201 | Android Widget Configuration | `android-widget` | Android | Not started | Android-only |
| P2-202 | Android Widget Launcher Smoke | `android-widget` | Android | Deferred | Needs widget host/launcher automation strategy |
| P2-203 | iOS Widget Deep Link | `content-rich` | iOS | Not started | iOS-only |
| P2-204 | Share Extension Smoke | `empty` | Android, iOS | Deferred | OS share surfaces can be unstable |
| P2-205 | Tablet And Split Layout | `content-rich` | Android tablet, iPad | Deferred | Requires dedicated devices/simulators |
| P2-206 | Large Dataset Pagination | `large-content` | Android, iOS | Blocked | Needs `large-content` seed profile |
| P2-207 | Real Provider Smoke | real providers | Android, iOS | Deferred | Manual/nightly only with staging credentials |
| P2-208 | Background Sync Timing | profile TBD | Android, iOS | Deferred | OS timing makes this manual/nightly |

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
- [ ] Run wrapper script if the flow belongs to P0.
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
