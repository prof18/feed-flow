# FeedFlow E2E

Maestro tests use the debug-only in-app seeder before each flow:

```text
feedflow://e2e/reset
feedflow://e2e/seed?profile=content-rich
feedflow://e2e/reset-and-seed?profile=content-rich
```

The stable completion marker is `E2E seed complete` with accessibility identifier `e2e_seed_complete`.

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
e2e/scripts/run-android.sh
e2e/scripts/run-ios.sh
```
