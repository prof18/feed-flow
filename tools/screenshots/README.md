# FeedFlow store screenshots

Renders the App Store / Play Store screenshots for every locale from the copy
already in this repo, with no design tool in the loop. This replaces the Figma
"FeedFlow Screenshot Blueprint — Master" file.

## Commands

```bash
node render.mjs --locale it            # one locale
node render.mjs --all                  # every locale in assets/screenshotcopy
node render.mjs --platform android_phone --locale ru
node serve.mjs                         # live preview at :4321
node verify.mjs                        # geometry vs Figma + margins, all locales
```

Output lands in `build/<locale>/<platform>/<screen>.png` at exact store
dimensions (Android phone 1080x2400, Play feature graphic 1024x500).

`serve.mjs` renders the same page the exporter screenshots, so the preview is
byte-identical to what ships. Config and template edits apply on reload, so
tweak, reload, re-render — no restart.

The toolbar's first row switches **platform**, the second switches **locale**;
both are also URL parameters (`?platform=ipad&locale=it`). **Export <locale>**
renders the current locale for the current platform and **Export all locales**
renders every translated locale for it, both writing to `build/` and reporting
the file count and any warnings — the CLI without the terminal. To cover every
platform at once use `node render.mjs --all`. Export is preview-only; nothing
the exporter serves exposes it.

## Verifying

`node verify.mjs` checks the English render against the frozen Figma English
board and checks margins on every locale (device centred, right margin never
tighter than the left, bottom margin, feature-graphic padding). It exits
non-zero on failure — run it after any geometry change.

Figma's device x values had drifted off-centre (03 and 04 by 4px, 05 by 35px)
while 01 and 02 were centred to half a pixel, so devices are centred here and
horizontal position is checked against centred rather than against Figma.

Screen 05 is a special case: its cards are wider than the frame that holds
them, so the frame clips on the right (as it does in Figma) and what gets
centred is the **painted** content, not the frame box. The verifier measures
the same way.

## Adding a language

Add `assets/screenshotcopy/<locale>/screenshot_copy.yml` and run
`node render.mjs --locale <locale>`. That is the whole procedure. There is no
mode limit, no font swap, no per-screen spacing to re-derive.

Weblate seeds every key with an empty value, so a locale file can hold all 63
keys and still be untranslated. Locales with empty values are skipped and
listed, rather than rendered with blank headlines.

## How the layout stays locale-proof

The device position is *derived*, never hand-placed: it sits a fixed gap below
however tall the translated text actually rendered. A language with longer
headlines simply pushes the phone further down — Russian moves it up to 86px
lower than English with no intervention.

When the text grows past what the canvas can absorb, relief is taken in this
order, most-preferred first:

1. **Shrink the device** a few percent (invisible in practice).
2. Close the gap above the device, down to its floor.
3. Close the gap under the headline.
4. Give up top margin, down to a protected floor of 88px.
5. Shrink the headline, down to 60%.

The top margin is defended deliberately: a headline crowded against the canvas
edge reads far worse than a marginally smaller phone. Across the nine current
locales nothing gets past step 1 — the largest adjustment is Italian screen 04
at 92%.

Every adjustment is reported as a warning at the end of a render, and a screen
that still does not fit warns loudly rather than silently shipping clipped.

Chips size themselves to their label and centre on the card they annotate, so a
long translation widens the pill instead of being clipped.

## Fonts

Montserrat and Poppins are vendored under `assets/fonts/` at weights **400, 500
and 700**, with their Google Fonts `unicode-range` subsetting intact. All three
weights are needed: 700 headlines, 400 subheadlines, 500 chips. Vendoring only
one weight makes the browser synthesise the others, which looks close but is
not the design. Chips declare `Poppins, Montserrat` —
Poppins has no Cyrillic, so the browser falls back per-character to Montserrat
instead of silently substituting a system face the way Figma did.

For a script neither font covers (CJK, Hebrew, Tamil), add the face to
`assets/fonts/fonts.css` and append it to the stacks in `src/render/page.mjs`.

## Layout

```
render.mjs               CLI: serve -> Chromium -> element screenshots
serve.mjs                live preview
src/copy.mjs             reads assets/screenshotcopy/<locale>/screenshot_copy.yml
src/server.mjs           static assets + generated page
src/render/page.mjs      HTML/CSS generation + the auto-fit script
src/config/<platform>.mjs geometry, lifted from Figma
assets/captures/         real app captures, native resolution
assets/fonts/            vendored woff2 + @font-face css
assets/reference/        Figma renders kept for visual diffing
```

Adding a platform means one `src/config/<platform>.mjs` — nothing else. The
CLI, preview and verifier discover configs automatically. Screens are one of
three kinds: `phone` (headline, subheadline, device), `comparison` (headline,
subheadline, two overlapping cards + chips), or `plain` (no copy — background,
blobs, one capture).

A platform with no on-image copy sets `localised: false` and renders once into
`build/base/<id>/`, since the result is identical in every language. See
`ADDING_A_PLATFORM.md`.

## Platforms

| id | canvas | screens | notes |
|---|---|---|---|
| `android_phone` | 1080x2400 | 5 + feature graphic | Play feature graphic is 1024x500 |
| `android_tablet` | 2560x1440 | 5 | one 16:9 set serves both Play tablet slots |
| `iphone` | 1284x2778 | 5 | flattened device mockups |
| `ipad` | 2732x2048 | 5 | flattened device mockups |
| `macos` | 2880x1800 | 4 | mockups bleed to the bottom edge |
| `windows` | 3840x2160 | 4 | no on-image copy; renders once |

## Provenance

Geometry was extracted from Figma file `gzweOdjwQnGs6vlvfDt0hl`, board
"Android Phone Board (ru-RU Export)" (nodes 683:3, 683:12, 683:21, 683:30,
683:39, 683:55) via the Figma MCP server. Values are the design's real numbers
and are deliberately not rounded to a grid.

Two deliberate departures from the Figma board, both to remove manual per-locale
work:

1. Chips are content-sized and centred on their card. In Figma the right-hand
   chip carried a hand-measured width and was not centred.
2. Type is declared `Poppins, Montserrat`. Poppins is the original English board
   face and covers Latin; Montserrat picks up Cyrillic and Greek per-character,
   since Poppins has no glyphs for them. Figma silently substituted a system
   face instead, which is what made its wrap-height maths unreliable.

Spacing comes from the English board (top 112, device tops 508 / 598 / 588 /
508.8 / 514), which English reproduces exactly. The ru-RU board's tighter values
are used as the compression floors, not as the defaults — building on them was
an early mistake that gave every language Russian's cramped spacing.
