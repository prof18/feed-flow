# Desktop E2E suite

End-to-end flows for the desktop app. Each test launches the real Jewel app in its own JVM, attaches to it with [Spectre](https://github.com/rock3r/spectre)'s launch-and-attach API, and drives it like a user. The flows mirror the mobile Maestro catalog (`e2e/maestro/maestro-e2e-tests.md`) under the same IDs, plus desktop-only flows with the `DSK-` prefix.

## Running

```bash
e2e/scripts/run-desktop.sh            # every flow
e2e/scripts/run-desktop.sh smoke      # SM-001 to SM-013
e2e/scripts/run-desktop.sh regression # REG-*
e2e/scripts/run-desktop.sh desktoponly # DSK-*
```

The script runs `./gradlew --quiet --console=plain :desktopApp:desktopE2eTest -Pfeedflow.e2eSuite=<suite>`; the suite values are `smoke`, `regression`, `desktoponly` and `all` (the default). To run one class or method, pass `--tests` to the Gradle task directly.

- macOS only. On other systems every flow is skipped.
- Local only, before a release. The suite is not part of `allTests`, `check` or CI.
- One run at a time: `/tmp/ffe2e/run.lock` refuses a second run on the same machine.
- Real FeedFlow windows open and take focus while the suite runs. Leave the machine alone until it finishes.
- A release build (`is_release=true` in `props.properties`) is refused, because it would ignore the E2E properties and open the real data.

## How a test runs

`DesktopE2eRule` (`desktopApp/src/jvmTest/kotlin/com/prof18/feedflow/desktop/e2e/harness/`) gives every test:

- an isolated home at `/tmp/ffe2e/<8 letters>`, with the app's data folder in `data/` (`feedflow.e2e.dataDir`). The path is short on purpose: the app binds a Unix socket there and macOS limits socket paths to 104 bytes.
- the Java preferences node `feedflow-e2e` (`feedflow.e2e.prefsNode`), removed before and after each test. The rule stores a 1280×800 window size in it before each launch, because a fresh node opens an 800×600 window that uses the single-pane layout.
- a startup seed (`feedflow.e2e.seed=<action>:<profile>[:<account>]`, plus `feedflow.e2e.seedAudioUrl`), applied through the shared `E2eSeedRunner` before the main window appears. The seed stores cached reader content in the format the platform caches: Markdown on desktop, HTML on Android and iOS. A failed seed exits the app with code 86 and the `FEEDFLOW_E2E_SEED_FAILED` marker on stderr.
- a scripted answer for the import and export file dialog (`feedflow.e2e.fileDialogResponse`): the first line of `file-dialog-response.txt` is the chosen file, and an empty or missing file means Cancel.
- a log of every URL the app would open (`feedflow.e2e.openedUriLog`) in `opened-uris.txt`, instead of opening a browser or mail client.
- a local fixture HTTP server for `e2e/fixtures`, started on first use of `fixtureUrl`.

The app reads these properties only in debug builds. A passing test deletes its home; a failing one keeps it. On failure the rule writes evidence to `desktopApp/build/reports/desktop-e2e/<TestClass>/<method>/`: `home.txt`, `tree.txt`, `windows.txt`, a capture per window (`capture-<index>.png` and `.json`), the app's stdout and stderr, and `evidence-errors.txt` when an item could not be collected. JUnit XML lands in `desktopApp/build/test-results/desktopE2eTest/`.

## Writing a flow

- One test method per flow ID, named with the ID first, for example `` `REG-101 add feed form validation`() ``, in the class for its area (`smoke/`, `regression/`, `desktoponly/`).
- Start the method with `val flow = e2e.start(<Seed>)`. Use `Flow` for every interaction: it polls, waits for idle and fails with the start of the semantics tree.
- Use `EnFeedFlowStrings` (`flow.strings`) for every string that is a resource; never hardcode English UI text.
- Find controls by test tag first (`desktopApp/.../desktop/testing/*E2eIds.kt`), then by text, then by content description.
- No live network: use `flow.fixtureUrl(...)` and `flow.fixtureOpml(...)`, and assert opened links through `flow.openedUris()`.
- Before porting a mobile flow, check that each step's feature exists on desktop (the string's property is rendered in `desktopApp/src/jvmMain`). Record missing features below.

## Known limitations

- Menu-bar keyboard shortcuts (for example Cmd+N for Add feed) do not reach the app: Spectre sends synthetic keys to the Compose window, and the native macOS menu bar does not see them. Keys handled inside a window, such as Escape, Enter and the arrow keys, work.
- Spectre's attach API has no secondary click. Sidebar feed and category menus open with Shift+F10 on the focused row (`Flow.openSidebarMenu`). Article and search-result menus cannot be opened: clicking a row moves focus into the reader pane and no key returns it to the row, so those steps are not covered.
- Window screenshots are refused on the attach path; evidence uses Spectre's `capture`, which returns a PNG of the window's content.
- Spectre cannot resize windows, so flows run in the 1280×800 two-pane layout only.
- The NoFeedsDialog cannot be reached: its only trigger, `onEmptyStateClick`, is passed to `DesktopHomeScreenContent` but never called. SM-001 asserts the empty-state message only.
- Article images are inside the clickable row, whose merged semantics drop the image test tags (`article_image_*`), so flows cannot assert that an image is shown or hidden; SM-009 checks the persisted setting instead.
- SM-004: removing a bookmark from the reader toolbar has no confirmation on desktop (`removeBookmarkDialogMessage` has no desktop caller).
- SM-005: the article list's footer "Mark all as read" button acts without a confirmation; the confirmation belongs to the File menu entry, which Spectre cannot reach.
- SM-007: the desktop reader has no "Listen to article" action (`readerModeTtsListen` has no desktop caller), and the long-press tooltip step is an Android gesture.
- REG-101: the Add feed window stays open after a successful add, ready for the next feed; the flow closes it.
- REG-104, REG-129, REG-131, REG-183: desktop has no feed list screen and no article-list "more" menu, so Feed settings opens from the sidebar feed menu. REG-104 does not cover the fetch-failed warning, which no seed profile provides.
- REG-147: the empty-library view offers Add feed and Import and export only; the Accounts and Feed Suggestions buttons are in the unreachable NoFeedsDialog.
- REG-148, REG-158: desktop has a next-feed button in the article list footer instead of the pull gesture.
- REG-155: in the two-pane layout "Open another feed" toggles the sidebar.
- REG-157: desktop reorders by dragging sidebar rows directly; there are no reorder toggles.
- REG-186: the URL-less article's context menu (no Share) is not covered, because article context menus cannot be opened.
- REG-106, REG-109, REG-143, REG-152, REG-154, REG-168: the article and search-result context menu steps (bookmark, mark read, mark above/below, open comments, the action set) are not covered. REG-106 and REG-154 reach the same states through the reader toolbar.
- REG-107: dragging a row with the mouse also clicks it, which opens the article and marks it read, so only the bookmark swipe direction is asserted.
- REG-108: the grid layout renders as big image rows in the two-pane layout (grid needs the single-pane list).
- REG-109: the feed order is changed in Settings → Feed list → Sort by Date; desktop has no home "Sort & filter" menu.
- DSK-006: Jewel's Markdown renderer exposes an inline image to accessibility by its Markdown source (`![alt](url)`), so the flow finds the image by its alt text in a node of its own; it cannot check that the image loaded.
- REG-163: the row merges its title and image into one node, so the flow checks every RTL row renders; it cannot compare image and title positions.
- REG-169: the Bookmarks footer's Mark all as read acts without the bookmarks-specific confirmation, which belongs to the File menu entry.
- REG-184: the large-content articles point at e2e.feedflow.local, so the reader shows "Unable to parse article"; the flow follows the selected row.
- REG-121: the Reading pane has no Browser row; desktop opens links in the system default browser.
- REG-123: the Android-only background sync Wi-Fi and charging restrictions are skipped.
- REG-124: desktop has no home "more" menu; the flow uses the Refresh feeds toolbar button (not pressed), Settings → Feed list for order and read articles, and Settings → Sync & storage → Delete week-old articles.
- REG-132: the crash-reporting switch renders only in release builds, so the flow asserts it is absent.
- REG-172: the sidebar is toggled with the article list toolbar's navigation button; the View menu shortcut is not reachable.
- iCloud is unavailable in this debug build: the Accounts pane has no iCloud entry (setupICloudSupport() in Main.kt). REG-118 covers Dropbox only, and REG-138 and REG-142 check the entry is absent.
- REG-117: the server subscriptions warning is not on desktop (`serverAccountSubscriptionsWarning` has no desktop caller), so its restart step is skipped.
- REG-120: the CSV rows reference the feed by an id derived from the original OPML URL, so this flow imports the committed OPML unchanged; its e2e.feedflow.local host is an mDNS name that fails locally after Done.
- REG-133: the home pending-upload action lives only in the native File menu; the flow checks the Back up now button on the linked Dropbox account instead, without running it.
- REG-134, REG-162, REG-167: the article list scrolls with Spectre's scroll wheel; rows are tall, so a flow scrolls in steps of 30 notches.
- DSK-001: while a query is active the Settings sidebar lists results instead of panes, and the row highlight is a timed tint with no semantics, so the flow checks the About pane heading and the row.
- DSK-002: the menu-bar shortcuts do not reach the app (see above); the flow checks the reader's Next button, which backs the same Article menu action.
- Settings has no in-window button; flows open it through the sidebar's add menu ("Import feed from OPML"), then pick the pane.

## Not applicable on desktop

| ID | Mobile flow | Reason |
| --- | --- | --- |
| REG-115 | 115-notifications-profile.yaml | Feature not in the desktop app: no notification settings (`settingsNotificationsEnableAllTitle` has no desktop caller; the desktop Notifier is a no-op, desktop/di/DI.kt). |
| REG-126 | 126-deep-link-routing.yaml | Desktop handles only add-feed links (`grep -rn "feedflow://feed" desktopApp/src/jvmMain` prints nothing); covered by REG-189. |
| REG-127 | 127-notifications-secondary-settings.yaml | Feature not in the desktop app: no notification settings (same check as REG-115). |
| REG-128 | 128-notifications-empty-state.yaml | Feature not in the desktop app: no notification settings (same check as REG-115). |
| REG-137 | 137-reading-behavior-browser-selector-persistence.yaml | Desktop opens links in the system default browser; BrowserManager.kt has no browser list. |
| REG-156 | 156-feed-source-reorder-smoke.yaml | Feature not in the desktop app: there is no feed list screen in Settings (`grep -rln "feedSourceList" desktopApp/src/jvmMain` prints nothing); desktop reorders in the sidebar, covered by REG-157. |
| REG-164 | 164-drawer-swipe-gesture.yaml | Android edge-swipe gesture; desktop has no drawer gesture in the two-pane layout. |
| REG-111 | 111-reader-image-viewer.yaml | Feature not in the desktop app: no reader image viewer (`grep -rli "imageViewer" desktopApp/src/jvmMain` prints nothing; the viewer is Android's `ReaderModeE2eIds.IMAGE_VIEWER`). |
| REG-165 | ios/regression/165-ios-reader-scroll-retention.yaml | iOS background/foreground cycle. |
| REG-166 | 166-reader-rotation-retention.yaml | Device rotation. |
| REG-174 | 174-podcast-audio-playback.yaml | Feature not in the desktop app: no in-app audio playback; the desktop audio banner hands the episode to another app (covered by REG-173). |
| REG-175 | 175-now-playing-navigation.yaml | Feature not in the desktop app: no Now playing indicator (`audioNowPlaying` has no desktop caller). |
| REG-176 | 176-reader-speech-playback.yaml | Feature not in the desktop app: no reader speech (`readerModeTtsListen` has no desktop caller). |
| REG-177 | 177-widget-refresh-settings.yaml | Android home-screen widgets. |
| REG-179 | 179-share-existing-feed.yaml | Android share intent; desktop add-feed links are covered by REG-189 and DSK-004. |
| REG-181 | ios/regression/181-adaptive-navigation.yaml | iOS orientation changes; Spectre cannot resize desktop windows. |
| REG-187 | ios/regression/187-scene-snackbar.yaml | iOS sheet presentation. |
| REG-188 | ios/regression/188-ios-reader-link-scroll-retention.yaml | iOS in-app browser. |
| REG-182 | 182-feed-verification-actions.yaml | Feature not in the desktop app: no feed verification UI (`grep -rli "verif" desktopApp/src/jvmMain` prints nothing). |
| REG-185 | 185-back-closes-drawer-and-filter.yaml | Android system Back. |
