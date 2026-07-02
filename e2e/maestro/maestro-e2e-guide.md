# Maestro E2E Guide

This guide explains how to write and run FeedFlow Maestro tests. Use it together with:

- `e2e/maestro/maestro-e2e-tests.md`
- `e2e/maestro/maestro-e2e-tests.html`

## Structure

```text
e2e/
  maestro/
    android/smoke/
    android/regression/
    ios/smoke/
    ios/regression/
    shared/
  fixtures/
    articles/
    opml/
    csv/
  scripts/
    run-android.sh
    run-android-smoke.sh
    run-ios.sh
    run-ios-smoke.sh
```

Use platform-specific flow files when the setup or UI differs between Android and iOS. Shared flows are fine only for commands that are identical on both platforms.

## App IDs

- Android debug app: `com.prof18.feedflow.debug`
- iOS debug app: `com.prof18.feedflow.dev`

Every mobile flow must declare the correct `appId`:

```yaml
appId: com.prof18.feedflow.debug
---
- launchApp
```

## Seed Profiles

Tests should start from a deterministic seed state instead of depending on live feeds or previous app state.

Supported profiles:

- `empty`
- `content-rich`
- `card-layout`
- `big-image-layout`
- `grid-layout`
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
- `sync-upload-required`
- `large-content`

Seed deep links:

```text
feedflow://e2e/reset
feedflow://e2e/seed?profile=content-rich
feedflow://e2e/reset-and-seed?profile=content-rich
feedflow://e2e/reset-and-seed?profile=sync-linked-mock&account=fresh_rss
```

`sync-linked-mock` and `sync-upload-required` can select a seeded account with `account=fresh_rss`, `miniflux`, `bazqux`, `feedbin`, `dropbox`, or `icloud`.

The stable completion marker is:

- visible text: `E2E seed complete`
- accessibility id: `e2e_seed_complete`

## Seeding In Flows

Android uses a debug-only seed activity. After seeding, tap `e2e_open_app` to return to the real app.

```yaml
appId: com.prof18.feedflow.debug
---
- launchApp
- openLink: feedflow://e2e/reset-and-seed?profile=content-rich
- assertVisible: "E2E seed complete"
- tapOn:
    id: e2e_open_app
```

iOS handles the seed link inside the app. The simulator may show a system confirmation dialog for custom schemes, so keep the `Open` tap optional.

```yaml
appId: com.prof18.feedflow.dev
---
- launchApp
- openLink: feedflow://e2e/reset-and-seed?profile=content-rich
- tapOn:
    text: "^Open$"
    optional: true
- assertVisible:
    id: e2e_seed_complete
```

## Fixture Files

Import/export flows use deterministic files under `e2e/fixtures/`.

- Android fixtures are pushed to `/sdcard/Download/feedflow-e2e` by `e2e/scripts/push-android-fixtures.sh`.
- iOS fixtures are copied into the app Documents directory and the simulator Files provider by `e2e/scripts/push-ios-fixtures.sh`.
- The OPML fixture is named `feedflow-valid-opml-smoke.xml` so Android DocumentsUI exposes it through the Documents filter, but its contents are OPML.
- iOS Files may focus a grid item on the first tap; SM-011 deliberately double taps fixture thumbnails.

The wrapper scripts push fixtures after installing the app and before running Maestro flows.

## Writing A New Flow

1. Pick the smallest seed profile that covers the scenario.
2. Put the flow under the right platform and suite folder, for example `e2e/maestro/android/smoke/003-library-filters.yaml`.
3. Start with `launchApp`, then reset and seed through the E2E deep link.
4. Prefer stable accessibility ids over visible text when ids exist.
5. Use visible text for seeded article/feed assertions only when the text is intentionally stable.
6. Use regex text assertions for seeded titles that include real headline details, for example:

```yaml
- assertVisible:
    text: "E2E Newest Unread Article.*"
```

7. Keep each flow focused on one behavior. Do not make one long flow cover unrelated settings, reading, search, and import behavior.
8. Avoid live network dependencies, OAuth, real provider auth, and OS-owned state in smoke and regression tests.

## Common Commands

Tap:

```yaml
- tapOn:
    id: settings_button
```

Assert visible:

```yaml
- assertVisible:
    id: e2e_seed_complete
```

Assert not visible:

```yaml
- assertNotVisible: "E2E Hidden Feed Article"
```

Wait for content:

```yaml
- extendedWaitUntil:
    visible:
      text: "E2E Reader Mode Success Article.*"
    timeout: 5000
```

Scroll to content:

```yaml
- scrollUntilVisible:
    element:
      text: "E2E Pinned Feed Article.*"
    direction: DOWN
    timeout: 10000
```

Go back:

```yaml
- back
```

Use `back` only in Android flows. Prefer explicit visible navigation controls on iOS.

## Running Tests

Preconditions:

- Maestro is installed and available as `maestro`.
- For Android, a device or emulator is running. Default local target: `Resizable_Experimental`.
- For iOS, the `iPhone 17 Pro` simulator is booted and `iosApp/FeedFlow.xcodeproj` exists.

Run the full local automated wrappers before a release. These build, install, push fixtures, and run `smoke` followed by `regression`:

```bash
e2e/scripts/run-android.sh
e2e/scripts/run-ios.sh
```

Run the fast smoke subset while iterating:

```bash
e2e/scripts/run-android-smoke.sh
e2e/scripts/run-ios-smoke.sh
```

The smoke-only wrappers build, install, and run the current smoke flows sequentially:

- Android: `e2e/maestro/android/smoke`
- iOS: `e2e/maestro/ios/smoke`

Run one Android flow against an already installed build:

```bash
maestro --platform android test e2e/maestro/android/smoke/002-seeded-timeline-loads.yaml
```

Run one iOS flow against an already installed build:

```bash
SIMULATOR_UDID=$(xcrun simctl list devices booted | awk -F '[()]' '/iPhone 17 Pro/ {print $2; exit}')
maestro --platform ios --device "$SIMULATOR_UDID" test e2e/maestro/ios/smoke/002-seeded-timeline-loads.yaml
```

Pin the platform when both Android and iOS devices are running. Otherwise Maestro may choose the wrong target.

## Debugging Failures

Maestro writes failure artifacts under:

```text
~/.maestro/tests/<timestamp>/
```

Look for:

- `maestro.log`
- `commands-*.json`
- `screenshot-*.png`
- iOS `xctest_runner_*.log` files

Useful checks:

```bash
adb devices
xcrun simctl list devices booted
maestro --platform android test e2e/maestro/android/smoke/001-first-launch-empty.yaml
maestro --platform ios --device "$SIMULATOR_UDID" test e2e/maestro/ios/smoke/001-first-launch-empty.yaml
```

When iterating as an agent, use the Maestro MCP if available:

1. `list_devices`
2. `inspect_screen`
3. `run` with one inline flow or a specific flow file

Maestro Viewer is available locally at `http://127.0.0.1:10003/` when the MCP server is running.

## When To Run Maestro

Run the relevant Maestro flow when:

- adding or changing a user-visible feature that can be covered through existing app UI, debug seed deep links, fixtures, or test tooling without adding production-only code paths
- adding or changing an E2E flow
- changing debug seed setup
- changing navigation or visible UI covered by existing flows
- changing a seeded profile used by flows

Run both wrapper scripts before handing off changes that affect shared E2E setup, app launch, seeded content, or cross-platform UI behavior.

Update `e2e/maestro/maestro-e2e-tests.md` whenever you add, remove, or significantly reshape a flow. If a feature cannot be covered by Maestro without production-only code changes or unstable OS/provider dependencies, record the reason in the Known Limitations section. If the physical flow inventory changes, update `e2e/maestro/maestro-e2e-tests.html` as well.

For non-E2E code changes, keep following the normal project gates in `AGENTS.md`.
