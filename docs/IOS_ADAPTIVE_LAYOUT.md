# Adaptive iOS layout

FeedFlow uses one `NavigationSplitView` and one detail `NavigationStack` across compact and regular widths. Folding, rotation, and window resizing must adapt the existing hierarchy instead of replacing it: replacing the root discards search, sheets, reader scroll position, and deeper navigation history. `FeedFlowScene` owns each window's `AppState`; playback, browser preferences, and `AppAppearance` theme state remain app services.

Compact Home returns to the sidebar with the native dismissal action. Filter selections and navigation pushes request the detail column. When a regular-width layout becomes compact, it also requests the detail column so a visible timeline or reader survives folding instead of exposing the sidebar. This request applies only at that transition; the drawer remains accessible afterward. Wide column visibility remains a writable binding for native sidebar controls and persistence.

Layout decisions use the local horizontal and vertical size classes. On iOS 26 and later, compact toolbars use a native navigation title and separate labeled actions so the system can arrange them horizontally or vertically. Compact Home removes the automatic split-view sidebar item because it supplies its own drawer action. Earlier iOS versions retain the existing compact header. Reader actions use native toolbar spacing. Scroll content adds only its centering margin; SwiftUI already handles the safe-area insets.

The compact sidebar includes the now-playing accessory. In wide layouts the accessory belongs to the detail column, avoiding two simultaneous players. Reader destinations continue to use their own playback accessory.

This follows Apple's [iPhone Duo preparation guide](https://developer.apple.com/iphone-duo/prepare/). Continuous feed and article content can cross the fold; FeedFlow does not split individual articles at the hinge.

## Validation

Use the deterministic `content-rich` and `audio-episode` profiles documented in the [Maestro guide](../e2e/maestro/maestro-e2e-guide.md). REG-181 covers search state through rotation. Folding and live resizing require Device Hub; see the [manual matrix](../e2e/maestro/maestro-e2e-tests.md#known-limitations).

The 2026-10-15 simulator checks used Xcode 27.1, iPhone Duo on iOS 27.1, and conventional iPhone 18 Pro / iPad Pro 13-inch on iOS 27.0:

| Check | Result |
| --- | --- |
| Duo closed, open portrait/landscape, partially open | Feed/sidebar and reader layouts inspected; native vertical actions remain visible. |
| Search while folding and rotating | Query, result, and Back navigation retained. |
| Compact drawer after reader Back | Native dismissal returns to the sidebar after popping the reader. |
| Fold from sidebar + timeline | User recording and Device Hub reproduced the sidebar replacing the timeline on closing. After the transition fix, open → closed retained Timeline; opening the compact drawer and selecting Read returned to its article list. |
| Reader Back, then fold | On the transition-fix build, opening a Read article in compact width, unfolding, then returning with Back showed the Read list; closing retained that list. Folding with the reader still open and compact drawer → unfold → close were not repeated in this follow-up. |
| Long reader while folding and rotating | Reading position retained with text reflow. |
| Add Feed sheet while folding | Unsaved URL retained; form and dismissal controls remain reachable. |
| Audio across fold poses | Local fixture played and paused; compact drawer retained its mini-player, while the wide layout showed one player in the detail column. |
| Live resizing | Narrow/wide feed and reader inspected; compact sidebar selection opens the detail. |
| Conventional iPhone/iPad | Reader controls inspected; iPhone search → Back → drawer → Read → timeline passed on the final build. iPad sidebar/filter navigation and restored sidebar visibility checked. |
| REG-181 on iPhone/iPad | All recorded assertions and final screenshots completed, but Maestro hung during teardown. The flow result is inconclusive. |

The Duo Maestro attempt could not handle the system deep-link confirmation reliably. A separate iPad timeline smoke attempt timed out opening its seed deep link, and a final iPhone library-filter smoke attempt stopped during driver initialization before running any assertions. None is recorded as a passing flow. The full release suite, live OAuth, physical-device performance, and multiwindow OAuth presentation are not validated by these checks. Existing OAuth presentation code still selects an application scene globally and needs a separate authenticated multiwindow check.

Local run logs and screenshots are under `.tmp/duo-readiness/` and are not committed. Run the complete release gate before shipping; these checks are a focused adaptation pass.

## Separate navigation exploration

The requested next step keeps two visible panes: sidebar + article list while browsing, then article list + reader after opening an article. Back would restore sidebar + article list, preserving the selected filter and list scroll position. The same behavior needs consideration on iPad; the request does not add a third visible pane.

Today the reader is pushed inside the list's detail stack, while the sidebar stays in the leading column. Replacing that leading column with the existing list needs navigation ownership to change: one list state and selection must survive moving between roles, without duplicate collectors or losing scroll position. Before implementation, settle list width/readability on Duo and iPad, how an article selection updates the reader, and how search results, the in-app browser, audio destinations, and compact Back history participate. Folding from either wide arrangement should retain the current list or article. This exploration is intentionally separate from the fold-column fix.
