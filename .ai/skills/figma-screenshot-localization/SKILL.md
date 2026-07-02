---
name: figma-screenshot-localization
description: Generate a new localized set of FeedFlow App/Play Store screenshots in the Figma screenshot blueprint file by duplicating the existing ru-RU export (not the English baseline) and swapping text. Use when the user asks to localize store screenshots, generate screenshots for a new language, or export screenshots for a language other than English/Russian.
---

# Figma Screenshot Localization

Generates a new-language export set of FeedFlow's store screenshots inside the
"FeedFlow Screenshot Blueprint — Master" Figma file, then exports full-resolution
PNGs for the user without any manual per-screen work in the Figma UI.

**Figma file key:** `gzweOdjwQnGs6vlvfDt0hl`
(URL: `https://www.figma.com/design/gzweOdjwQnGs6vlvfDt0hl/FeedFlow-Screenshot-Blueprint---Master`)

**Prerequisite:** load the `figma-use` skill before any `use_figma` call — it is
mandatory per that skill's own instructions and this workflow is entirely
`use_figma` script driven.

## The core idea: clone the ru-RU export, not the English baseline

The English `<Platform> Board` frames use Poppins and tight, storefront-default
spacing. They are the **frozen reference** — never edit them again.

The `<Platform> Board (ru-RU Export)` frames (added in an earlier session) are
the **reusable template**: they already use Montserrat (a font with full
Cyrillic + Latin coverage, unlike Poppins which has no Cyrillic glyphs) and
already have the corrected breathing-room spacing worked out per platform (see
constants table below). For any new language, duplicate from the **ru-RU
export**, not from the English board — this means you never have to redo the
font swap or re-derive spacing math from scratch, only re-verify it still fits
given the new language's text lengths.

Pages and their frames (page IDs are stable Figma node IDs, verify with
`figma.root.children` if anything looks off):

| Platform | Page ID | English board | ru-RU template to duplicate from |
|---|---|---|---|
| Android Phone | `5:2` | `Android Phone Board` | `Android Phone Board (ru-RU Export)` |
| iPhone | `6:2` | `iPhone Board` | `iPhone Board (ru-RU Export)` |
| iPad | `8:2` | `iPad Board` | `iPad Board (ru-RU Export)` |
| Android Tablet | `13:2` | `Android Tablet Board` | `Android Tablet Board (ru-RU Export)` |
| macOS | `9:2` | `macOS Board` | `macOS Board (ru-RU Export)` |
| Windows | `10:2` | `Windows Board` | *(none — see Windows note below)* |

Inside each `ru-RU Export` wrapper, the screen components keep their original
English names, e.g. `android_phone_01_just_a_list`, `iphone_03_stay_organized`,
`macos_04_make_the_list_yours`. The Android Phone wrapper also contains a
`play_graphic` component (the Play Store feature graphic).

## Translation source

Screenshot copy lives in the repo at
`assets/screenshotcopy/<locale>/screenshot_copy.yml`, keyed identically to
`assets/screenshotcopy/base/screenshot_copy.yml` (the English source of truth).
Locales already translated and ready as of this writing: `da`, `et`, `it`,
`lv`, `ru`. Before starting, diff the target locale's key set against `base`
to confirm it's complete:

```bash
diff <(grep -oE '^screenshot_copy_[a-z0-9_]+:' assets/screenshotcopy/base/screenshot_copy.yml | sort) \
     <(grep -oE '^screenshot_copy_[a-z0-9_]+:' assets/screenshotcopy/<locale>/screenshot_copy.yml | sort)
```

If the locale folder doesn't exist yet or the diff shows missing keys, stop
and tell the user — do not invent translations yourself (the project's
i18n/translation rules forbid translating on your own).

Each yml key maps directly to a Figma variable name by replacing `_` with `/`
after the `screenshot_copy` prefix, e.g. `screenshot_copy_ipad_03_headline`
→ Figma variable `screenshot_copy/ipad/03/headline`. `compact_label`,
`rich_cards_label`, and the feature graphic headline are **not** in the
variable collection — those are set as plain text directly (see below).

## Workflow

### 1. Check the variable mode budget

The `Screenshot Copy` variable collection is capped at **10 editable modes**
by this file's Figma plan (a hard API limit, not a UI setting — confirmed by
testing `setValueForMode` on modes beyond the 10th, which throws
`cannot modify modes beyond limit of 10`). Currently only `en-US` and `ru-RU`
exist, so there's room for 8 more languages. If you're about to add the 11th
mode, stop and tell the user — either the plan needs upgrading or an unused
mode needs deleting first (deleting is safe only if you've verified via
`valuesByMode` that the mode holds no real, non-placeholder translation).

```js
const collections = await figma.variables.getLocalVariableCollectionsAsync();
const c = collections.find(c => c.name === 'Screenshot Copy');
return c.modes.map(m => m.name); // current modes
```

### 2. Add the new mode and populate variable values

```js
const collections = await figma.variables.getLocalVariableCollectionsAsync();
const c = collections.find(c => c.name === 'Screenshot Copy');
const newModeId = c.addMode('<locale>'); // e.g. 'it-IT', match the yml's locale convention

// values = { "screenshot_copy/android_phone/01/headline": "...", ... }
// built from the target locale's screenshot_copy.yml, mapped as described above
const vars = await Promise.all(c.variableIds.map(id => figma.variables.getVariableByIdAsync(id)));
for (const v of vars) {
  if (values[v.name] !== undefined) v.setValueForMode(newModeId, values[v.name]);
}
```

Sanity-check afterward that every variable you expect to touch actually got a
value (missing keys silently leave the mode falling back to the default mode's
value, which would ship English text disguised as the new language).

### 3. Clone the ru-RU export per platform, into the same page

Do **one platform per `use_figma` call**, following the pattern already
proven for Russian:

```js
const page = figma.root.children.find(p => p.id === '<platform page id>');
await figma.setCurrentPageAsync(page);

const ruWrapper = page.children.find(c => c.name === '<Platform> Board (ru-RU Export)');
const board = page.children.find(c => c.name === '<Platform> Board'); // English baseline, read-only reference

const newWrapper = figma.createFrame();
newWrapper.name = '<Platform> Board (<locale> Export)';
newWrapper.x = ruWrapper.x;
newWrapper.y = ruWrapper.y + ruWrapper.height + 200; // stack below, never touch existing wrappers
newWrapper.clipsContent = false;
newWrapper.fills = board.fills;
page.appendChild(newWrapper);

let cursorX = 0;
const clones = [];
for (const src of ruWrapper.children) { // each screen component + feature graphic
  const clone = src.clone();
  newWrapper.appendChild(clone);
  clone.x = cursorX;
  clone.y = 0;
  cursorX += clone.width + 200;
  clones.push({ sourceId: src.id, cloneId: clone.id, name: clone.name });
}
newWrapper.resize(cursorX - 200, Math.max(...ruWrapper.children.map(c => c.height)));
```

**Always explicitly write every text value after cloning — never trust that a
clone carries the correct content.** This session hit a real caching quirk
where a cloned node's `.characters` read-back reflected a value written to the
*source* node in a later, unrelated script call, even though clone() should be
an independent copy. The fix that reliably worked: don't rely on inherited
text at all.

- **Headline/subheadline text is bound to the `Screenshot Copy` variables**
  (`setBoundVariable('characters', variable)` was applied when the RU export
  was built). Bindings survive `.clone()`, so you generally don't need to
  touch these nodes' text directly — setting the new wrapper's explicit
  variable mode (step 4) is enough. Verify by reading `.characters` on a
  couple of cloned nodes after setting the mode and confirming they resolve
  to the new language's translated string, not English or Russian leftovers.
- **Compact/rich-cards labels, the "01"–"05" page-number markers (iPad/macOS),
  and the feature graphic headline are plain, unbound text.** Set these
  explicitly from the locale's yml (or leave numbers alone — they're just
  digits) right after cloning, in the same script that does the cloning.

### 4. Bind the new wrapper to the new mode

```js
const collections = await figma.variables.getLocalVariableCollectionsAsync();
const c = collections.find(c => c.name === 'Screenshot Copy');
const newMode = c.modes.find(m => m.name === '<locale>');
newWrapper.setExplicitVariableModeForCollection(c, newMode.modeId);
```

### 5. Re-verify spacing — don't assume RU's exact pixel positions transfer

The new language's text will wrap differently than Russian's. Reuse the same
budget-conserving recompute from the RU work: pick a `TOP_MARGIN` +
`HEADLINE_TO_SUB_GAP` + `SUB_TO_MOCKUP_GAP` split whose sum (`K`) matches the
platform's already-tuned constant below, measure the *true* wrapped headline
height with `textAutoResize = 'HEIGHT'` (reliable here because Montserrat
actually has the right glyphs — this measurement was **unreliable with
Poppins** on Cyrillic because Figma silently substitutes a fallback font for
glyphs Poppins lacks, and the substitute's metrics don't match what
`loadFontAsync` measures), then position subheadline and mockup off of that.

Per-platform constants established this session (canvas size, then the
`TOP_MARGIN / GAP1 / GAP2` split and `K = TOP_MARGIN + GAP1 + GAP2`, kept
constant so mockup position — and therefore fit within canvas — never changes
regardless of how the three numbers are split):

| Platform | Canvas | Layout | TOP / GAP1 / GAP2 | K |
|---|---|---|---|---|
| Android Phone | 1080×2400 | top-stack, 01–05 | 32 / 20 / 60 | 112 |
| iPhone | 1284×2778 | top-stack, 01–05 | 36 / 24 / 66 | 126 |
| iPad | 2732×2048 | top-stack w/ number, 01–04 | 44 / 24 / 78 | 146 |
| iPad | 2732×2048 | 05 (headline/sub + panels+labels below) | 44 / 24 / 90 (downstream gap) | 158 |
| Android Tablet | 2560×1440 | top-stack, 01–03 | 28 / 22 / 62 | 112 |
| Android Tablet | 2560×1440 | 04 (side-by-side text+mockup) | headline vertically centered on original block center, GAP1 = 26 | — |
| Android Tablet | 2560×1440 | 05 (top-stack + comparison wrapper) | unchanged — already had a 151px gap, generous | — |
| macOS | 2880×1800 | 01 top-stack | 36 / 24 / 90 | 150 |
| macOS | 2880×1800 | 02 top-stack (headline font reduced ~78% to fit — mockup is unusually tall relative to canvas) | 30 / 18 / 40 | 88 |
| macOS | 2880×1800 | 03 (side-by-side) | headline vertically centered on original block center, GAP1 = 28 | — |
| macOS | 2880×1800 | 04 (top-stack + panels+labels below) | 30 / 20 / 66 (downstream gap) | 116 |

If a screen's translated headline is long enough that even at `K`'s current
split the mockup would overflow the canvas, shrink `TOP_MARGIN` further before
touching `GAP2` (breathing room before the device is the whole point of this
exercise — don't sacrifice it first), and only reduce headline font size as a
last resort (as was needed for macOS 02).

Font weights: **Bold** for headlines, **Regular** for subheadlines and body
copy. Don't assume — check the *other* platforms' already-correct nodes with
`getStyledTextSegments(['fontName'])` if unsure for a given label.

### 6. Fix pill labels (compact list / rich cards)

Two failure modes hit every platform this session, both worth checking again
per language since translated label length varies:

- **Width clipping**: the pill frame's fixed width doesn't fit the new text.
  Fix: measure `text.width` after setting `textAutoResize = 'WIDTH_AND_HEIGHT'`,
  resize the frame to `textWidth + text.x * 2 + ~10px` margin, and recenter the
  frame over its associated panel (`panel.x + panel.width / 2 - newWidth / 2`).
- **Z-order clipping**: on Android Phone and similar side-by-side comparison
  layouts, the second phone mockup can be a *later sibling* than the pill,
  so it draws on top and visually clips it — independent of the pill's own
  width. Fix: `parent.appendChild(chipFrame)` to move the pill to the end of
  its parent's children (front of z-order). Check both symptoms; a pill can
  have either or both problems.

### 7. Verify visually before exporting

Screenshot every new screen with `get_screenshot` at the node's native
resolution (`maxDimension` = the node's actual max(width, height) — using a
smaller value can hide real wrapping issues, and a value that's too small
relative to actual size sometimes even produces misleading renders, so always
request native size for verification, not just for final export). Check
specifically for: headline/subheadline overlap, subheadline running into the
device mockup, pill label clipping, and content exceeding the canvas bottom
edge.

### 8. Windows note

Windows screenshots have no in-image headline text by design (captions live
in the Microsoft Store listing metadata, not the screenshot). Nothing to
duplicate in Figma for Windows — translate `screenshot_copy_windows_0N_caption`
separately wherever Windows store captions are actually authored/uploaded.

### 9. Export

Don't ask the user to export one-by-one from the Figma UI. For each new
screen component, call `get_screenshot` with `maxDimension` set to the
component's native max dimension, download the PNG via `curl` into a folder
tree organized by platform (mirroring the structure used for the `ru-RU`
delivery: `<locale>-export/android_phone/`, `/iphone/`, `/ipad/`,
`/android_tablet/`, `/macos/`, `/feature_graphic/`), zip it, and deliver via
the file-send tool. Every component already carries the original's
`exportSettings` (PNG/JPG, scale 1x) via `.clone()`, so if the user later
wants to export natively from inside Figma instead, selecting a platform's
`<locale> Export` wrapper and using Figma's "Export N layers" batch action
works too — no extra setup needed on that front.

## Adding a language whose script Montserrat doesn't cover

Montserrat covers Latin (incl. extended) and Cyrillic, which handles every
locale currently in `assets/screenshotcopy/` (`da`, `et`, `it`, `lv`, `ru`)
plus the base English. If a future locale needs Hebrew, CJK, Tamil, or another
script outside that coverage, don't assume Montserrat will silently work —
Figma will fall back to an uncontrolled substitute font exactly like it did
with Poppins/Cyrillic, and the same wrap-height measurement unreliability will
resurface. In that case: pick a font with real coverage for that script
(e.g. a "Noto Sans <Script>" family is usually available in Figma's font
list), verify wrap predictions in the JS layout math actually match a
rendered `get_screenshot` before trusting the numbers, and expect to
re-derive the `K` constants for that font's metrics rather than reusing the
table above verbatim.
