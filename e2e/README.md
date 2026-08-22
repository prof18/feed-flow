# FeedFlow E2E

Maestro tests use the debug-only in-app seeder before each flow:

```text
feedflow://e2e/reset
feedflow://e2e/seed?profile=content-rich
feedflow://e2e/reset-and-seed?profile=content-rich
```

The stable completion marker is `E2E seed complete` with accessibility identifier `e2e_seed_complete`.

## Restore development feeds

On this development machine, `feedflow-restore-dev-feeds` sends the personal OPML from
`/Users/mg/Workspace/feedflow/rss.xml` to the debug-only E2E seeder, which writes its feed
sources and categories directly into the local database after an E2E run has replaced test data.
The Android and iOS wrapper scripts call it automatically when it is installed on `PATH`;
otherwise run it directly after installing a debug app:

```bash
feedflow-restore-dev-feeds --platform android
feedflow-restore-dev-feeds --platform ios
```

## Profiles

- `empty`
- `content-rich`
- `card-layout`
- `compact-list`
- `reader-mode`
- `external-browser`
- `read-behavior`
- `oldest-first`
- `swipe-actions`
- `swipe-disabled`
- `notifications`
- `android-widget`
- `sync-linked-mock`

## Run

```bash
# Full automated Android and iOS suites.
e2e/scripts/run-android.sh
e2e/scripts/run-ios.sh

# Fast smoke subsets.
e2e/scripts/run-android-smoke.sh
e2e/scripts/run-ios-smoke.sh
```
