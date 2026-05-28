# Maestro E2E Tests

A catalog of every Maestro flow currently in the suite. For how to author, run, and debug flows see [`maestro-e2e-guide.md`](./maestro-e2e-guide.md).

- **Release Gate** — 13 flows, both platforms, run before every release (`e2e/scripts/run-android.sh` and `e2e/scripts/run-ios.sh`)
- **Regression Suite** — 54 flows, both platforms, run for broader local/CI validation
- **Manual-Supported** — 10 flows, run from dedicated wrapper scripts; not part of the release gate
- **Known Limitations** — what is intentionally not covered and why

## Running

```bash
# Whole release gate
e2e/scripts/run-android.sh
e2e/scripts/run-ios.sh

# A single Android flow
maestro --platform android test e2e/maestro/android/release-gate/<flow>.yaml

# A single iOS flow
SIMULATOR_UDID=$(xcrun simctl list devices booted | awk -F '[()]' '/iPhone 17 Pro/ {print $2; exit}')
maestro --platform ios --device "$SIMULATOR_UDID" test e2e/maestro/ios/release-gate/<flow>.yaml
```

Manual-supported flows have their own wrapper scripts under `e2e/scripts/run-android-*.sh` and `e2e/scripts/run-ios-ipad-*.sh`.

## Release Gate

Run before every release. Flow files live in `e2e/maestro/{android,ios}/release-gate/`.

| ID | Flow | Profile | Coverage |
| --- | --- | --- | --- |
| RG-001 | `001-first-launch-empty.yaml` | `empty` | App launches, empty timeline message visible, seed marker stable. |
| RG-002 | `002-seeded-timeline-loads.yaml` | `content-rich` | Seeded timeline renders newest article, unread count, hidden feed excluded from Timeline. |
| RG-003 | `003-library-filters.yaml` | `content-rich` | Drawer filters: Timeline, Read, Bookmarks, source filter, category filter, uncategorized. |
| RG-004 | `004-article-read-bookmark-state.yaml` | `content-rich` | Open article, bookmark via reader toolbar, article appears under Read/Bookmarks filters. |
| RG-005 | `005-mark-all-read.yaml` | `content-rich` | Home overflow → Mark all as read confirmation, articles move to Read filter. |
| RG-006 | `006-search-core.yaml` (+ iOS `006-search-bookmark-filter.yaml`) | `content-rich` | Search query, filter chips (All / Read / Bookmarks). iOS uses seeded query/filter deeplink. |
| RG-007 | `007-reader-mode-core.yaml` | `reader-mode` | Open article in reader, next-article button, more menu, Font Size entry visible. |
| RG-008 | `008-feed-edit-core.yaml` | `content-rich` | Drawer feed-source long-press → Edit feed, change title, hide, pin, category, save. |
| RG-009 | `009-feed-list-settings-persist.yaml` | `content-rich` | Feed list settings: layout, image visibility, order — mutated and persisted across relaunch. |
| RG-010 | `010-reading-behavior-settings-persist.yaml` | `content-rich` | Reading behaviour: reader mode, show-read, auto-hide read — mutated and persisted across relaunch. |
| RG-011 | `011-import-export-smoke.yaml` | `empty` + fixtures | OPML import (Android Downloads / iOS Files), CSV import, end-to-end success. |
| RG-012 | `012-blocked-words.yaml` | `content-rich` | Add blocked word, blocked article disappears from Timeline; iOS covers the deterministic settings add/delete path. |
| RG-013 | `013-relaunch-persistence.yaml` | `content-rich` | Reader-mode + bookmark mutations survive app relaunch. |

## Regression Suite

Run for broader functional coverage. Flow files live in `e2e/maestro/{android,ios}/regression/`.

| ID | Flow | Profile | Platforms | Coverage |
| --- | --- | --- | --- | --- |
| REG-101 | `101-add-feed-form-validation.yaml` | `empty` | Android, iOS | Add Feed URL field validation, Save button starts disabled, category selector opens the Categories sheet. Android additionally types a URL via `inputText` and asserts the Save button becomes enabled. |
| REG-103 | `103-feed-suggestions.yaml` | `empty` | Android, iOS | Drawer → Feed Suggestions screen, Business category, add suggestion → "Added" confirmation. |
| REG-104 | `104-feed-source-list-management.yaml` | `content-rich` | Android, iOS | Settings → Feeds: expand/collapse category, inline rename (Android), delete confirmation, fetch-failed warning. |
| REG-105 | `105-category-management.yaml` (+ iOS `105-category-add-validation.yaml`) | `content-rich` | Android, iOS | Add Feed category sheet: create new category, duplicate-name validation. Drawer category menu: rename, delete all feeds. Drawer "Delete category" path is iOS-only because Android only renders categories that still own a feed source. |
| REG-106 | `106-article-context-menu.yaml` | `content-rich` | Android, iOS | Article long-press: Mark as read/unread, Add/Remove bookmark mutations. |
| REG-107 | `107-swipe-actions.yaml` | `swipe-actions`, `swipe-disabled` | Android, iOS | Left = toggle read, right = toggle bookmark swipes. Android also covers the disabled-swipe variant. |
| REG-108 | `108-feed-layout-matrix-card.yaml`, `108-feed-layout-matrix-compact.yaml` | `card-layout`, `compact-list` | Android, iOS | Card and Compact feed-list layouts render the seeded items. |
| REG-109 | `109-feed-order-mark-above-below.yaml` | `oldest-first` | Android, iOS | Oldest-first ordering profile, Mark all above / below as read article context-menu actions. |
| REG-110 | `110-reader-fallback.yaml` | `reader-mode` | Android, iOS | Reader fallback path: article fails extraction, fallback web view + Open in browser button visible. |
| REG-111 | `111-reader-image-viewer.yaml` | `reader-mode` | Android, iOS | Reader image viewer: open image, share button visible, close. |
| REG-112 | `112-link-opening-preferences.yaml` | `external-browser` | Android, iOS | Per-feed Reader Mode override forces the article into reader mode despite the global external-browser preference. |
| REG-113 | `113-sync-storage-settings.yaml` | `content-rich` | Android, iOS | Refresh-on-launch, RSS parsing errors, auto-delete picker, Android sync-period, clear-downloaded cancel path. |
| REG-114 | `114-appearance-settings.yaml` | `content-rich` | Android, iOS | Theme picker (Dark), hide unread count, Android Black theme + reduce motion. |
| REG-115 | `115-notifications-profile.yaml` | `notifications` | Android, iOS | Seeded notifications settings: per-feed toggle and grouping picker visibility. |
| REG-116 | `116-account-list-one-account-constraint.yaml` | `sync-linked-mock` | Android, iOS | One-account constraint: with FreshRSS linked, other providers are disabled; disconnect unlocks them. |
| REG-117 | `117-greader-provider-form-validation.yaml` | `empty` | Android, iOS | FreshRSS/Miniflux/BazQux/Feedbin provider forms: required-field state, FreshRSS connect enablement + password-visibility (Android). |
| REG-118 | `118-cloud-provider-mock-states.yaml` | `sync-linked-mock` | Android, iOS | Seeded Dropbox linked state (both platforms) and iCloud linked state (iOS). |
| REG-119 | `119-opml-import-error-states.yaml` | `empty` + fixtures | Android, iOS | Invalid OPML import → error screen + Choose another file recovery. |
| REG-120 | `120-csv-import-article-states.yaml` | `empty` + fixtures | Android, iOS | CSV article import → unread/read/bookmarked-unread/bookmarked-read state assertions. |
| REG-121 | `121-reading-behavior-secondary-settings.yaml` | `content-rich` | Android, iOS | Browser row visibility, save-reader-content toggle, prefetch confirmation, mark-read-when-scrolling toggle. |
| REG-122 | `122-feed-list-settings-detail-controls.yaml` | `content-rich` | Android, iOS | Font scale visibility, secondary hide toggles, description line limit, date/time format, swipe-action pickers. |
| REG-123 | `123-sync-storage-advanced-settings.yaml` | `content-rich` | Android, iOS | Clear-downloaded confirmation (both); Android Wi-Fi-only, charging-only, clear image cache confirmation. |
| REG-124 | `124-home-overflow-secondary-actions.yaml` | `content-rich` | Android, iOS | Home overflow: force-refresh visibility, Sort & Filter sheet (Oldest First, Show read), Clear week-old articles confirmation. |
| REG-125 | `125-about-support-navigation.yaml` | `content-rich` | Android, iOS | Settings → About & Support → About screen, open-source licenses. |
| REG-126 | `126-deep-link-routing-*.yaml` (Android) / `126-deep-link-routing.yaml` (iOS) | `content-rich` | Android, iOS | Android: article reader, feed-source filter, category filter routes via explicit MainActivity intents. iOS: `feedflow://feed/<id>` reader route. |
| REG-127 | `127-notifications-secondary-settings.yaml` | `notifications` | Android, iOS | Enable-all mutation, per-feed toggle mutation, grouping picker; Android check-period + Wi-Fi/charging restrictions. |
| REG-128 | `128-notifications-empty-state.yaml` | `empty` | Android, iOS | Notifications settings empty/no-feeds state. |
| REG-129 | `129-edit-feed-secondary-options.yaml` | `notifications` | Android, iOS | Article context-menu → feed settings; link-opening-preference mutation, notification toggle mutation, save. |
| REG-130 | `130-add-feed-secondary-options.yaml` | `notifications` | Android, iOS | Add-feed: select existing category, toggle notifications. |
| REG-131 | `131-home-source-filter-edit-entry.yaml` | `content-rich` | Android, iOS | Pick a feed-source filter from the drawer, open that source's Edit screen from the Home overflow menu. |
| REG-132 | `132-about-support-secondary-options.yaml` | `content-rich` | Android, iOS | Crash-reporting toggle mutation, support-link visibility. |
| REG-133 | `133-home-sync-backup-action.yaml` | `sync-upload-required` | Android, iOS | Pending-upload Home overflow action with a linked mock sync account. |
| REG-134 | `134-large-content-pagination-search.yaml` (Android) / `134-large-content-pagination.yaml` (iOS) | `large-content` | Android, iOS | Pagination beyond the first 40-item page (both). Android also covers large-dataset search. |
| REG-135 | `135-feed-list-secondary-persistence.yaml` | `content-rich` | Android, iOS | Description line limit, date format, time format, left/right swipe action dropdowns survive relaunch. |
| REG-136 | `136-sync-storage-secondary-persistence.yaml` | `content-rich` | Android, iOS | Auto-delete period survives relaunch (both); sync period survives relaunch (Android). |
| REG-137 | `137-reading-behavior-browser-selector-persistence.yaml` | `content-rich` | Android, iOS | Browser selector mutation (Chrome / Default) survives relaunch. |
| REG-138 | `138-cloud-provider-disconnect.yaml` | `sync-linked-mock` | iOS | Unlink seeded iCloud account, return to add-account state. |
| REG-139 | `139-article-export-filter.yaml` (+ iOS `139-ios-article-export-filter.yaml`) | `content-rich` | Android, iOS | Bookmarked-articles export filter reaches export-success state. |
| REG-140 | `140-opml-export-success.yaml` | `content-rich` | Android, iOS | Export feeds to OPML, app reaches export-success state. |
| REG-141 | `141-article-export-filter-matrix.yaml` | `content-rich` | Android, iOS | All / Read / Unread article export filters reach export-success state. |
| REG-142 | `142-icloud-provider-backup.yaml` | `sync-linked-mock` | iOS | Seeded iCloud account Backup action returns to linked sync state. |
| REG-143 | `143-article-context-menu-action-set.yaml` | `content-rich` | Android, iOS | App-owned context-menu action set for an article with comments + feed website metadata. |
| REG-144 | `144-notification-permission-denied.yaml` | `notifications` | iOS | Permission-blocked notification settings UI, notification toggles stay hidden. |
| REG-145 | `145-network-provider-linked-states.yaml` | `sync-linked-mock` | Android, iOS | Seeded Miniflux / BazQux / Feedbin linked screens, disabled state of unlinked providers, connected/last-sync/disconnect UI. |
| REG-146 | `146-drawer-feed-source-context-menu.yaml` | `content-rich` | Android, iOS | Drawer long-press: Mark all as read, Open website visibility, Edit feed, Change category sheet, Pin/Unpin toggle, Delete confirmation. |
| REG-147 | `147-no-feeds-empty-state-cta.yaml` | `empty` | Android, iOS | NoFeedsBottomSheet fan-out: Add feed, Import and export, Feed Suggestions, Accounts destinations. |
| REG-148 | `148-pull-to-next-feed.yaml` | `content-rich` | Android, iOS | iOS NextFeedButton tap and Android PullToNextLayout overscroll gesture move filter to the next feed source. |
| REG-149 | `149-drawer-category-mark-all-read.yaml` | `content-rich` | Android, iOS | Drawer category long-press → Mark all as read, category Timeline becomes empty. |
| REG-150 | `150-reader-previous-and-font-menu.yaml` | `reader-mode` | Android, iOS | Reader Previous/Next buttons, Font Size menu opens; Android also exercises the Increase/Decrease slider buttons. |
| REG-151 | `151-feed-source-list-delete-all-in-category.yaml` | `content-rich` | Android, iOS | Settings → Feeds: category-header long-press, Delete all feeds + confirmation. |
| REG-152 | `152-search-result-context-menu.yaml` | `content-rich` | Android, iOS | Android: full long-press menu (Mark all above/below, Open comments, Add to bookmarks, Mark as read) + bookmark-filter follow-up. iOS: result-row visibility only (`.searchable` snapshot budget). |
| REG-153 | `153-drawer-add-import-feed-entries.yaml` | `content-rich` | Android, iOS | Drawer "+" → Add feed / Feed Suggestions / Import feed from OPML entries reach their destinations. |
| REG-154 | `154-empty-bookmarks-back-to-timeline.yaml` | `content-rich` | Android, iOS | Unbookmark every seeded item, empty Bookmarks message + Back to timeline shortcut returns to Timeline. |
| REG-155 | `155-empty-home-open-another-feed.yaml` | `empty` | Android, iOS | EmptyFeedView Open another feed button opens the drawer. |

## Manual-Supported Suite

Run from dedicated wrapper scripts; not part of the release gate. Flow files live in `e2e/maestro/{android,ios}/manual-supported/`.

| ID | Flow / Script | Profile | Platforms | Coverage |
| --- | --- | --- | --- | --- |
| MAN-201 | `201-android-widget-configuration.yaml` (`e2e/scripts/run-android-widget-config.sh`) | `android-widget` | Android | Seeded widget preview, header toggle preview mutation, background-color picker invalid-value validation, text-color mode mutation, Add Widget confirmation in the configuration activity. |
| MAN-202 | _Not implemented_ | `android-widget` | Android | Widget launcher smoke. Needs a launcher / widget-host automation strategy. |
| MAN-203 | covered by iOS `126-deep-link-routing.yaml` | `content-rich` | iOS | `feedflow://feed/<id>` route used by widget links; real SpringBoard widget tap is out of scope. |
| MAN-204 | `204-android-share-intent*.yaml` (`e2e/scripts/run-android-share-intent.sh`) | `empty` | Android | Real `ACTION_SEND` entry with a local RSS fixture served through `10.0.2.2`, share adds the feed/article. iOS Share Extension blocked — see Known Limitations. |
| MAN-205 | `205-android-tablet-split-layout.yaml` / `205-ipad-split-layout.yaml` (`e2e/scripts/run-android-tablet-layout.sh`, `e2e/scripts/run-ios-ipad-layout.sh`) | `content-rich` | Android tablet, iPad | Docked drawer + feed list remain visible while switching library filters at tablet/iPad width. |
| MAN-206 | covered by REG-134 | `large-content` | Android, iOS | Large-dataset pagination — pinned to the same flow as REG-134. |
| MAN-207 | _Not implemented_ | real providers | Android, iOS | Real-provider smoke. Manual / nightly with staging credentials. |
| MAN-208 | _Not implemented_ | TBD | Android, iOS | Background sync timing. Manual / nightly. |
| MAN-209 | `209-ipad-large-screen-surfaces.yaml` (`e2e/scripts/run-ios-ipad-large-screen.sh`) | `reader-mode` | iPad | Large-screen settings sheet presentation, reader detail presentation next to the sidebar, portrait/landscape rotation recovery. |
| MAN-210 | `210-android-tablet-rotation.yaml` (`e2e/scripts/run-android-tablet-rotation.sh`) | `reader-mode` | Android tablet | Docked drawer at tablet width, tablet settings presentation, reader presentation, portrait/landscape reader rotation. |

## Known Limitations

These are features intentionally not covered, with the reason recorded so they aren't re-investigated:

- **iOS Share Extension via OS share sheet** — Maestro can reach `shareCell` from Safari, but the synthesized tap dispatches into MobileSafari's WebView instead of the remote-hosted `SharingUIService` popover, so the extension process never starts (Maestro/XCTest limitation on iOS 26).
- **iOS large-dataset search (REG-134)** — XCTest hierarchy retrieval times out after opening the large search-result screen.
- **iOS search-result context-menu mutations (REG-152)** — `.searchable` view hierarchy exceeds Maestro's 30s main-thread snapshot budget. Android covers the menu; iOS is row-visibility only.
- **iOS SwiftUI text input on the FreshRSS connect form (REG-117)** — Maestro stalls on `setClipboard` / `pasteText` into the form. FreshRSS filled-form coverage stays Android-only.
- **OS browser launches (REG-112 / REG-137 / open-website actions)** — Default / internal / preferred-browser destinations leave the app. Only the in-app preference mutation and per-feed Reader Mode override are asserted.
- **iOS swipe actions for disabled and open-in-browser (REG-107)** — full-width gestures open the row or escape to OS/browser surfaces.
- **OPML partial-failure reporting (REG-119)** — local OPML imports don't produce `feedSourceWithError` / `notValidFeedSources` entries; only invalid-OPML rejection is exercised.
- **Real Cloud provider auth and most cloud mutations** — Mocked success/error auth, Google Drive mock state, Android/Dropbox unlink, Dropbox/Google Drive backup, and real provider sync need live credentials or provider-side mock support. Seeded linked screens, iOS iCloud unlink (REG-138), iOS iCloud backup (REG-142), and the pending-upload action (REG-133) are covered.
- **Android non-FreshRSS denied-notification path** — Revoking permission before the seeded flow didn't produce a stable local runner state. iOS denied-permission UI is covered by REG-144.
- **Android widget launcher smoke (MAN-202) and real iOS SpringBoard widget tap** — Need stable widget-host / SpringBoard automation. The configuration UI (MAN-201) and the widget feed-link deep route (MAN-203 / REG-126) are covered.
- **Real OS notification taps and feed-source/category notification delegate routes** — Need OS notification automation. The app-owned deep-link routing is covered by REG-126.
- **In-app review prompt** — `ReviewViewModel` request flow is OS/timing-driven; not exercised.
- **iPad / macOS menu commands and keyboard shortcuts** — Cmd-R, Cmd-Shift-A, Cmd-N, etc. from `FeedFlowApp+Menu.swift`. Maestro can drive iPad keyboard chords, but no flow added yet.
- **Android in-app Widget Settings screen** — Settings → Widget Settings exposes the same controls as MAN-201 but reached from the main settings tree. Same surfaces, different entry; not exercised separately.
- **Background sync / notification delivery (MAN-208)** — WorkManager scheduling, iOS background refresh, notification delivery, and notification deep-link routing remain manual / nightly candidates.
- **Force Add feed (formerly REG-102)** — dropped: required a DEBUG-only "trigger force-add failure" hook in production to deterministically reach the force-add UI without depending on network errors.
- **Android delete-category from drawer (formerly part of REG-105)** — dropped: the production drawer only lists categories that still own at least one feed source, so an empty category can't be long-pressed. iOS still covers this branch because its drawer lists categories independently.
