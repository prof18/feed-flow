# Maestro E2E Tests

A catalog of every Maestro flow currently in the suite. For how to author, run, and debug flows see [`maestro-e2e-guide.md`](./maestro-e2e-guide.md). For a browser-friendly physical flow inventory, open [`maestro-e2e-tests.html`](./maestro-e2e-tests.html).

- **Smoke** — 13 logical coverage flows, both platforms, useful as a fast confidence subset (`e2e/scripts/run-android-smoke.sh` and `e2e/scripts/run-ios-smoke.sh`). iOS has one extra physical YAML for the bookmark-filter search variant.
- **Regression Suite** — 66 logical coverage flows for broader local/CI validation. Some IDs split into platform-specific variants or seed helper YAML files.
- **Release Validation** — run smoke plus regression with `e2e/scripts/run-android.sh` and `e2e/scripts/run-ios.sh`.
- **Known Limitations** — what is intentionally not covered and why

## Running

```bash
# Full automated suite for release validation
e2e/scripts/run-android.sh
e2e/scripts/run-ios.sh

# Fast smoke subset
e2e/scripts/run-android-smoke.sh
e2e/scripts/run-ios-smoke.sh

# A single Android flow
maestro --platform android test e2e/maestro/android/smoke/<flow>.yaml

# A single iOS flow
SIMULATOR_UDID=$(xcrun simctl list devices booted | awk -F '[()]' '/iPhone 17 Pro/ {print $2; exit}')
maestro --platform ios --device "$SIMULATOR_UDID" test e2e/maestro/ios/smoke/<flow>.yaml
```

## Smoke

Google Drive disconnect with an unavailable credential provider is covered by Android Robolectric tests
(`GoogleDriveAuthHelperTest`). Maestro cannot deterministically force credential-provider failures
through the current seed tooling; seeded account state does not control the system credential provider.

Fast confidence subset. Flow files live in `e2e/maestro/{android,ios}/smoke/`.

| ID | Flow | Profile | Coverage |
| --- | --- | --- | --- |
| SM-001 | `001-first-launch-empty.yaml` | `empty` | App launches, empty timeline message visible, seed marker stable. |
| SM-002 | `002-seeded-timeline-loads.yaml` | `content-rich` | Seeded timeline renders newest article, unread count, hidden feed excluded from Timeline. |
| SM-003 | `003-library-filters.yaml` | `content-rich` | Drawer filters: Timeline, Read, Bookmarks, source filter, category filter, uncategorized. Bookmarks has no drawer count even with unread saved articles. |
| SM-004 | `004-article-read-bookmark-state.yaml` | `content-rich` | Open article, bookmark via reader toolbar, article appears under Read/Bookmarks filters. Android also verifies cancellation and confirmation of reader bookmark removal. |
| SM-005 | `005-mark-all-read.yaml` | `content-rich` | Home overflow → Mark all as read confirmation, articles move to Read filter. |
| SM-006 | `006-search-core.yaml` (+ iOS `006-search-bookmark-filter.yaml`) | `content-rich` | Search query, filter chips (All / Read / Bookmarks). iOS uses seeded query/filter deeplink. |
| SM-007 | `007-reader-mode-core.yaml` | `reader-mode` | Open article in reader, long-press toolbar tooltip (Android), next-article button, more menu, Text Settings sheet opens. |
| SM-008 | `008-feed-edit-core.yaml` | `content-rich` | Drawer feed-source long-press → Feed settings, toggle hide and pin, change category, save. Android also exercises the inline rename via `inputText`; iOS skips the rename (SwiftUI text input is flaky in Maestro) and only verifies the feed lands in the new category. |
| SM-009 | `009-feed-list-settings-persist.yaml` | `content-rich` | Feed list settings: layout, image visibility, order — mutated and persisted across relaunch. |
| SM-010 | `010-reading-behavior-settings-persist.yaml` | `content-rich` | Reading behaviour: article open mode, show-read, and auto-hide read persist across relaunch; scroll marking disables auto-hide with an explanation and restores the saved preference when turned off. |
| SM-011 | `011-import-export-smoke.yaml` | `empty` + fixtures | OPML import (Android Downloads / iOS Files), CSV import, end-to-end success. |
| SM-012 | `012-blocked-words.yaml` | `content-rich` | Add blocked word, blocked article disappears from Timeline; iOS covers the deterministic settings add/delete path. |
| SM-013 | `013-relaunch-persistence.yaml` | `content-rich` | Reader-mode + bookmark mutations survive app relaunch. |

## Regression Suite

Run for broader functional coverage. Flow files live in `e2e/maestro/{android,ios}/regression/`.

| ID | Flow | Profile | Platforms | Coverage |
| --- | --- | --- | --- | --- |
| REG-101 | `101-add-feed-form-validation.yaml` | `empty` | Android, iOS | Add Feed URL field validation, Save button starts disabled, category selector opens the Categories sheet. Android additionally types a URL via `inputText` and asserts the Save button becomes enabled. |
| REG-103 | `103-feed-suggestions.yaml` | `empty` | Android, iOS | Drawer → Feed Suggestions screen, Business category, add suggestion → "Added" confirmation. |
| REG-104 | `104-feed-source-list-management.yaml` | `content-rich` | Android, iOS | Settings → Feeds: expand/collapse category, inline rename (Android), delete confirmation, fetch-failed warning. |
| REG-105 | `105-category-management.yaml` (+ iOS `105-category-add-validation.yaml`) | `content-rich` | Android, iOS | Add Feed category sheet: create new category, duplicate-name validation. Drawer category menu: rename, delete all feeds. The drawer "Delete category" path is dropped on both platforms — neither drawer renders categories that no longer own a feed source. iOS now drives the category name inputs through `inputText` rather than DEBUG hooks. |
| REG-106 | `106-article-context-menu.yaml` | `content-rich` | Android, iOS | Article long-press: Mark as read/unread, Add/Remove bookmark mutations. |
| REG-107 | `107-swipe-actions.yaml` | `swipe-actions`, `swipe-left-only`, `swipe-disabled` | Android, iOS | Left = toggle read, right = toggle bookmark swipes. Android confirms right-swipe removal from Bookmarks, including cancel and confirm; it also verifies direction-specific arbitration: a configured row direction keeps priority, while a disabled right direction opens the drawer; with both row directions disabled, a rightward content swipe also opens the drawer. |
| REG-108 | `108-feed-layout-matrix-card.yaml`, `108-feed-layout-matrix-big-image.yaml`, `108-feed-layout-matrix-grid.yaml`, `108-feed-layout-matrix-compact.yaml` | `card-layout`, `big-image-layout`, `grid-layout`, `compact-list` | Android, iOS | Card, Big Image, Grid, and Compact feed-list layouts render the seeded items. |
| REG-109 | `109-feed-order-mark-above-below.yaml` | `oldest-first` | Android, iOS | Oldest-first ordering profile, Mark all above / below as read article context-menu actions. |
| REG-110 | `110-reader-fallback.yaml` | `reader-mode` | Android, iOS | Reader fallback path: article fails extraction, fallback web view + Open in browser button visible. |
| REG-111 | `111-reader-image-viewer.yaml` | `reader-mode` | Android, iOS | Reader image viewer: open image, share button visible, close. |
| REG-112 | `112-link-opening-preferences.yaml` | `external-browser` | Android, iOS | Per-feed Reader Mode override forces the article into reader mode despite the global external-browser preference. |
| REG-113 | `113-sync-storage-settings.yaml` | `content-rich` | Android, iOS | Refresh-on-launch, auto-delete picker, Android sync-period, clear-downloaded cancel path. |
| REG-114 | `114-appearance-settings.yaml` | `content-rich` | Android, iOS | Theme picker (Dark), hide unread count, Android Black theme + reduce motion. |
| REG-115 | `115-notifications-profile.yaml` | `notifications` | Android, iOS | Seeded notifications settings: per-feed toggle and grouping picker visibility. |
| REG-116 | `116-account-list-one-account-constraint.yaml` | `sync-linked-mock` | Android, iOS | One-account constraint: with FreshRSS linked, other providers are disabled; disconnect unlocks them. |
| REG-117 | `117-greader-provider-form-validation.yaml` | `empty`, `content-rich` | Android, iOS | FreshRSS/Miniflux/BazQux/Feedbin provider forms: required-field state, warning hidden with no local subscriptions and shown when subscriptions exist, plus FreshRSS replacement confirmation + password visibility (Android). |
| REG-118 | `118-cloud-provider-mock-states.yaml` | `sync-linked-mock` | Android, iOS | Seeded Dropbox linked state (both platforms) and iCloud linked state (iOS). |
| REG-119 | `119-opml-import-error-states.yaml` | `empty` + fixtures | Android, iOS | Invalid OPML import → error screen + Choose another file recovery. |
| REG-120 | `120-csv-import-article-states.yaml` | `empty` + fixtures | Android, iOS | CSV article import → unread/read/bookmarked-unread/bookmarked-read state assertions. |
| REG-121 | `121-reading-behavior-secondary-settings.yaml` | `content-rich` | Android, iOS | Browser row visibility, save-reader-content toggle, prefetch confirmation, mark-read-when-scrolling toggle. |
| REG-122 | `122-feed-list-settings-detail-controls.yaml` | `content-rich` | Android, iOS | Font scale visibility, secondary hide toggles, description line limit, date/time format, swipe-action pickers. |
| REG-123 | `123-sync-storage-advanced-settings.yaml` | `content-rich` | Android, iOS | Clear-downloaded confirmation (both); Android Wi-Fi-only, charging-only, clear image cache confirmation. |
| REG-124 | `124-home-overflow-secondary-actions.yaml` | `content-rich` | Android, iOS | Home overflow: force-refresh visibility, Sort & Filter sheet (Oldest First, Show read), Clear week-old articles confirmation. |
| REG-125 | `125-about-support-navigation.yaml` | `content-rich` | Android, iOS | Settings → About & Support → About screen, open-source licenses. |
| REG-126 | `126-deep-link-routing.yaml` | `content-rich` | Android, iOS | Android: article reader, feed-source filter, category filter routes. iOS: `feedflow://feed/<id>` reader route. |
| REG-127 | `127-notifications-secondary-settings.yaml` | `notifications` | Android, iOS | Enable-all mutation, per-feed toggle mutation, grouping picker; Android check-period + Wi-Fi/charging restrictions. |
| REG-128 | `128-notifications-empty-state.yaml` | `empty` | Android, iOS | Notifications settings empty/no-feeds state. |
| REG-129 | `129-edit-feed-secondary-options.yaml` | `notifications` | Android, iOS | Article context-menu → feed settings; article-open-mode mutation, notification toggle mutation, save. |
| REG-130 | `130-add-feed-secondary-options.yaml` | `notifications` | Android, iOS | Add-feed: select existing category, toggle notifications. |
| REG-131 | `131-home-source-filter-edit-entry.yaml` | `content-rich` | Android, iOS | Pick a feed-source filter from the drawer and open that source's Edit screen from the Home overflow menu. Android also changes the per-feed article-open mode, saves, reopens from the same overflow menu, and verifies the persisted setting is loaded. |
| REG-132 | `132-about-support-secondary-options.yaml` | `content-rich` | Android, iOS | Crash-reporting toggle mutation, support-link visibility. |
| REG-133 | `133-home-sync-backup-action.yaml` | `sync-upload-required` | Android, iOS | Pending-upload Home overflow action with a linked mock cloud-backup account (Dropbox on Android, iCloud on iOS; GReader accounts never show it). |
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
| REG-145 | `145-network-provider-linked-states.yaml` | `sync-linked-mock` | Android, iOS | Seeded Miniflux / BazQux / Feedbin linked screens, disabled state of unlinked providers, connected/last-sync/disconnect UI. |
| REG-146 | `146-drawer-feed-source-context-menu.yaml` | `content-rich` | Android, iOS | Drawer long-press: Mark all as read, Open website visibility, Feed settings, Change category sheet, Pin/Unpin toggle, Delete confirmation. |
| REG-147 | `147-no-feeds-empty-state-cta.yaml` | `empty` | Android, iOS | NoFeedsBottomSheet fan-out: Add feed, Import and export, Feed Suggestions, Accounts destinations. |
| REG-148 | `148-pull-to-next-feed.yaml` | `content-rich` | Android, iOS | iOS NextFeedButton tap and Android PullToNextLayout overscroll gesture move filter to the next feed source. |
| REG-149 | `149-drawer-category-mark-all-read.yaml` | `content-rich` | Android, iOS | Drawer category long-press → Mark all as read, category Timeline becomes empty. |
| REG-150 | `150-reader-previous-and-font-menu.yaml` | `reader-mode` | Android, iOS | Reader Previous/Next buttons, Text Settings sheet opens with Line Height, Reset, and current value labels. |
| REG-151 | `151-feed-source-list-delete-all-in-category.yaml` | `content-rich` | Android, iOS | Settings → Feeds: category-header long-press, Delete all feeds + confirmation. |
| REG-152 | `152-search-result-context-menu.yaml` | `content-rich` | Android, iOS | Android: full long-press menu (Mark all above/below, Open comments, Add to bookmarks, Mark as read), bookmark-filter follow-up, and cancel/confirm removal. iOS: result-row visibility only (`.searchable` snapshot budget). |
| REG-153 | `153-drawer-add-import-feed-entries.yaml` | `content-rich` | Android, iOS | Drawer "+" → Add feed / Feed Suggestions / Import feed from OPML entries reach their destinations. |
| REG-154 | `154-empty-bookmarks-back-to-timeline.yaml` | `content-rich` | Android, iOS | Android verifies cancel and confirm for bookmark removal before emptying the list; both platforms verify the empty Bookmarks message + Back to timeline shortcut returns to Timeline. |
| REG-155 | `155-empty-home-open-another-feed.yaml` | `empty` | Android, iOS | EmptyFeedView Open another feed button opens the drawer. |
| REG-156 | `156-feed-source-reorder-smoke.yaml` | `content-rich` | Android | Settings → Feeds: drag uncategorized feed sources, categories (including the Uncategorized group), and Technology feed sources, then verify the list remains usable. Shared/database tests assert the exact ordering semantics. |
| REG-157 | `157-drawer-reorder-smoke.yaml` | `reorder-drag` | Android | Drawer: drag pinned feed sources, categories (including the Uncategorized group), and feed sources inside Technology, then verify the drawer remains usable. Shared/database tests assert the exact ordering semantics. |
| REG-158 | `158-empty-feed-next-feed-navigation.yaml` | `content-rich` | Android, iOS | Mark the last unread article in a single-source filter as read; EmptyFeedView renders the next-feed affordance and Android's overscroll gesture / iOS's NextFeedButton move the filter to the next unread source. |
| REG-159 | `159-reader-feed-content-no-url.yaml` | `feed-content` | Android, iOS | URL-less item opens into the fully styled feed-content reader with its title and feed source, and omits unavailable browser and content-source actions. |
| REG-160 | `160-reader-content-source-navigation.yaml` | `reader-mode` | Android, iOS | Switching from cached web content to RSS content does not consume the reader back action; Back returns to the feed list. |
| REG-161 | `161-reader-no-url-no-content.yaml` | `feed-content` | Android, iOS | An item with neither a url nor feed content shows the unavailable-content message instead of an empty web view. |
| REG-162 | `162-pagination-scroll-read.yaml` | `pagination-scroll-read` | Android | Guards issue #1319: with mark-as-read-on-scroll enabled, scrolling through 90 unread items keeps loading every following page. Offset pagination over the unread-only query skipped ~40 articles per page once scrolled-past items were flushed as read, so articles 041-080 (asserted through the 050 "Skip Needle" row) were unreachable and the list stopped early. iOS is intentionally skipped: the pagination logic lives in shared code and the Android flow covers the regression wiring. |
| REG-163 | `163-rtl-content-direction.yaml` | `rtl-content` | Android, iOS | Timeline items follow their own content direction, not the app locale: a Persian item mirrors (image left of the title) while a Latin item in the same list does not; a Latin-prefixed mixed title stays left-to-right, and a direction-neutral title falls back to the description. |
| REG-164 | `164-drawer-swipe-gesture.yaml` | `swipe-disabled` | Android | A mostly vertical drag over the timeline scrolls without opening the drawer or falling through to the article, a deliberate horizontal content swipe opens the drawer in every system navigation mode, and drag-to-close still works once it is open. FeedFlow keeps Material3's outer full-screen drag handle disabled while the drawer is closed and handles unclaimed horizontal drags inside the content, after the feed list and configured row swipe actions get priority. iOS uses a separate SwiftUI drawer and is unaffected. |
| REG-165 | `165-ios-reader-scroll-retention.yaml` | `reader-mode` | iOS | Reader mode keeps its scroll position across a background/foreground cycle. Backgrounding makes iOS render the app-switcher snapshot in both appearances, flipping the SwiftUI color scheme twice; the reader must restyle the loaded document with JS instead of regenerating its HTML, because any HTML change reloads the web view and sends the article back to the top. Android is not covered: its reader web view is driven by separate Compose code and never regenerated from the color scheme. |
| REG-166 | `166-reader-rotation-retention.yaml` | `reader-mode` | Android | Guards issue #1401: rotating while reading keeps the reading position, and an open full-screen image viewer survives the rotation instead of being dismissed. `MainActivity` declares the size and orientation config changes, so the activity is not recreated and the reader's `loadDataWithBaseURL` document is never reloaded; the image URL is held in `rememberSaveable`. The post-rotation assertions check that the head anchor is gone and a late paragraph is on screen rather than re-asserting the tail anchor, because the text reflows at the new width and the anchoring can shift by a paragraph. The flow sets `PORTRAIT` before seeding so it does not inherit an orientation from an earlier flow. iOS is not covered: its reader is separate SwiftUI code, and REG-165 covers its own scroll-retention path. |
| REG-167 | `167-scroll-read-auto-hide.yaml` | `pagination-scroll-read` | Android, iOS | With both preferences saved on, scrolling marks articles read while keeping earlier rows reachable in the current list. Scrolling onward still loads the next page and reaches Article 050. |
| REG-168 | `168-mark-read-filter-boundaries.yaml` | `oldest-first` | Android, iOS | Mark all above/below from Bookmarks: the bookmarked unread article moves into Read, while unbookmarked old/new articles stay unread and visible on Timeline with Show read articles disabled. |
| REG-169 | `169-bookmarks-mark-all-read-confirmation.yaml` | `content-rich` | Android | Toolbar and footer each show the Bookmarks-specific bulk-read confirmation; cancel preserves unread state, confirm marks saved articles read and retains their bookmarks, and an unbookmarked Timeline article remains unread. |
