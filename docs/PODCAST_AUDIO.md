# Podcast audio enclosures

FeedFlow shows a headphones badge for items with a validated audio enclosure. In the built-in reader,
including stored feed content and extracted web content, a card below the heading displays the item
title, a **Play audio** control on mobile, and **Open in another app**. The card scrolls with the
article. Missing titles use the localized
**Audio episode** fallback; filenames and signed URL parameters are not displayed.

## Detection and ingestion

Only enclosure metadata identifies audio. Ordinary article URLs and HTML links to audio do not
create badges. HTTP(S) URLs must have a valid host. Audio MIME types are accepted; missing or generic
binary MIME types may use a supported extension: MP3, M4A, M4B, AAC, OGG, OGA, Opus, WAV, or FLAC.
Explicit image and video MIME types are rejected. Signed queries retain their original representation.

Local RSS uses the parser's raw enclosure. GReader-compatible providers use the first valid audio
enclosure; Feedbin requests enclosure data on both entry-fetch paths. The pinned local RSS parser
currently discards Atom enclosure links, so local Atom audio discovery is not supported by this change.

## Storage and upgrades

Audio URLs are local metadata in the nullable `feed_item.audio_url` column. Existing rows start with
no audio. Fetching the same item again from the same source can enrich or rotate its audio URL;
a fetch without valid audio preserves the known value. No historical replay or migration-time
network request is performed. Source collisions and deleted-item suppression keep their existing
semantics, and enrichment does not rewrite article fields, read state, or bookmarks.

CSV and cloud-backup formats are unchanged. CSV reimport of an existing same-source item preserves
its local audio URL while replacing the CSV fields. A fresh import, or replacement from a different
source, has no audio URL. Audio is not transferred by CSV or cloud backup.

## External opening

- **Android:** sends an unpinned `ACTION_VIEW` intent with `audio/*`, normalizing only the URL
  scheme for Android intent matching. The signed path and query are preserved. Normal system
  resolution honors a compatible saved handler; FeedFlow does not force a chooser.
  If the typed intent cannot launch, an untyped external URL intent is attempted. If both fail,
  FeedFlow shows a localized error. Handler support for remote audio varies.
- **iOS:** opens the original URL through `UIApplication`. Ordinary HTTPS audio normally opens in
  the system browser; FeedFlow's internal or preferred article browser is bypassed. This does not
  promise routing to Apple Podcasts or another podcast app. Failed opening presents a localized alert.
- **Desktop:** uses the existing safe external URL opener and error snackbar.

The reader's internal audio action token is consumed in FeedFlow and never sent to the OS. On mobile,
**Play audio** starts an in-app player, with playback controls kept in the Android reader dock and the
iOS compact player above the native reader bar. Playback position is retained when the player is closed
and reopened, and the active episode remains available while navigating to another article. Moving
to a plain article clears that article's audio card. Audio playback adds no read/bookmark writes;
normal detail navigation can still mark articles read under the existing settings.

The card belongs to the built-in reader. External pages and the in-app browser are unchanged, as
are article taps and reader eligibility. An enclosure-only item whose ordinary article URL is itself
a media URL can continue to open through the configured article browser rather than the built-in reader.

External opening hands the original audio URL to the system browser or compatible app. In-app mobile
playback streams the enclosure URL and offers play/pause, seeking, elapsed position, playback speed, and close/reopen
resume. FeedFlow does not download or import episodes. The receiving app owns playback when users
choose **Open in another app**.

One episode plays at a time. The mobile engines support background playback and system media
controls, and pause when headphones disconnect. Resume positions stay on the device, keyed by
article identity rather than a potentially changing audio URL. The most recent 200 positions are
retained; completing an episode clears its position. The mobile speed selector offers 0.5×, 1×,
1.5× and 2×, preserving pitch and the selected rate through pause/resume and episode changes while
the app's player remains alive. Changing speed while paused does not start playback. There is no
queue, download management, or sleep timer. Desktop continues to use external audio opening.

iOS associates its AVPlayer with a Now Playing session and publishes episode title, feed, artwork,
elapsed time, duration and playback rate for the Lock Screen and Control Center. Duration changes and
the end of preparation refresh the system metadata and scrubbing availability even while paused.
Seeking updates system progress immediately, and closing playback clears that session's metadata.

Outside the reader, mobile browsing screens show a compact active-episode indicator with its title
and play/pause control. Android uses a full-width bottom bar with a top divider and a background
extending behind system navigation; iOS keeps its glass accessory above native bottom controls.
It remains visible while paused. Tapping the title returns directly to the
episode using its per-feed or global article-opening preference, while preserving playback and position.
Reader preferences choose feed content or the full article; browser preferences open the article in
the configured browser. URL-less episodes still use their stored feed content. When another
article is open, the existing reader dock's episode title performs the same return action. The compact
indicator is hidden in the reader to avoid duplicating its controls, and closing playback removes it.
Android Back exits rendered reader articles through FeedFlow navigation instead of restoring an
older HTML document; website fallback pages retain their browser history.

Resume positions use a bounded, device-local Settings map rather than a database table: the player
only needs a position lookup by article ID, and retains at most 200 entries. There are no history,
queue or cross-device queries. A table becomes useful if that scope expands. On iOS,
`RepositoryAudioPlaybackPositionStore` adapts the Kotlin repository to the Swift player's small
storage protocol, allowing native player tests to inject an in-memory store without Koin or the
shared framework. The app composition root obtains the repository through `Deps`, matching the
existing Settings and sync repository bridges; the player does not resolve dependencies itself.

## Verification

Classifier, database migration/enrichment/CSV, provider ingestion, reader-state, HTML rendering,
and Android typed/fallback intent behavior have deterministic unit coverage. REG-173 covers the
audio badge, **Play audio** action, external opening action, and clearing the card on a plain article.
REG-174 uses the local 60-second low-amplitude WAV fixture to cover play/pause, seeking, closing and
reopening at the saved position, and keeping the active episode while moving to a plain article.
REG-175 covers the compact indicator on timeline and search, mini play/pause, direct return from another
article and from search, position retention, no duplicate indicator in the reader, and removal after
closing playback. It passed on Pixel and the dedicated iPhone 17 Pro Simulator on 2026-10-08; screenshots
confirm the indicator stays above the keyboard and the iOS native search field.
Both platforms exercise the speed selector. Android also checks the native media notification, its pause button, and the reader overflow menu while playback controls are open; on iOS the flow returns to the playing episode and taps the reader card's **Pause audio** action.
Native engine tests cover pausing before preparation, retained Android service reattachment,
iOS media-session configuration on first play and retry, media-services reset recovery without
automatic playback, and playback-speed changes without losing position or starting paused playback.
Native iOS tests also verify system progress and scrubbing after paused preparation, timing and rate
updates on seek/play/pause, metadata clearing on close, and session replacement after a media reset.
Playback identity uses item ID and audio URL, so changed titles, feed names or artwork do not restart
the same stream when users tap Pause; a changed audio URL still replaces the stream.
Android service tests verify session registration
before any controller connects so the native notification can be created.
The focused runner starts a byte-range-capable local server and supplies its URL to the debug seed;
Android uses `adb reverse` to expose the host server on the selected device's localhost, and iOS
Simulator connects through `127.0.0.1`. REG-173 and REG-174 passed on the connected Pixel and the
dedicated iPhone 17 Pro simulator, with the speed and native-notification extensions passing on 2026-10-06. The full `detekt allTests` gate and Android debug
build passed, including the native iOS playback tests; the iOS app build and formatting checks also
passed. The existing iOS REG-150 article-navigation and text-settings flow passed after the reader
toolbar integration. Status is recorded in the
[Maestro catalog](../e2e/maestro/maestro-e2e-tests.md).

Human acceptance remains necessary for all list layouts, long titles, RTL, light/dark appearance,
larger text, narrow windows, accessible focus, card scrolling, and font-change scroll retention.
Real external-handoff checks must cover Android compatible handlers with no default and a saved
default, external-browser fallback, iOS system opening with FeedFlow's internal browser selected,
Desktop opening, failure presentation, and unchanged read/bookmark state.
