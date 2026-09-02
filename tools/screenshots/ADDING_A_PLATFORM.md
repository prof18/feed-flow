# Porting a platform out of Figma

One platform = one file, `src/config/<id>.mjs`, plus its captures under
`assets/captures/<id>/`. Nothing else is edited: `render.mjs`, `serve.mjs` and
`verify.mjs` discover configs automatically. Use `src/config/android_phone.mjs`
as the worked example.

## Figma source

File key `gzweOdjwQnGs6vlvfDt0hl`. Pages:

| Platform | Page | English board |
|---|---|---|
| Android Phone | `5:2` | done — reference implementation |
| iPhone | `6:2` | `iPhone Board` |
| iPad | `8:2` | `iPad Board` |
| Android Tablet | `13:2` | `Android Tablet Board` |
| macOS | `9:2` | `macOS Board` |
| Windows | `10:2` | `Windows Board` |

`get_metadata` on the page to find screen node ids, then `get_design_context`
on each screen for exact values. Use `get_screenshot` sparingly to eyeball.

## The traps — every one of these cost real time on Android Phone

0. **NEVER record your own render's output as a `figmaReference` value.**
   That value exists to check the renderer against the design; filling it in
   from what you produced makes the check verify the renderer against itself
   and it will always pass. If a screen cannot reach Figma's real value, use a
   per-screen override — and if no override exists, STOP and report it. Do not
   quietly adjust the reference to match.

1. **Take geometry from the ENGLISH board, never a `(xx-XX Export)` board.**
   The localised boards carry spacing compressed to fit one language. Building
   on the ru-RU board gave every locale Russian's cramped margins.

2. **Headline and subheadline have DIFFERENT widths.** Figma gives each its own
   box. Reusing the headline's width for the sub made it run to the canvas edge
   and re-wrap. Record `textWidth` and `subWidth` separately.

3. **The headline→sub gap varies per screen** (Android Phone: 22/26/31/26/43).
   So does the sub→device gap. Both are per-screen, not per-platform.

4. **Figma's `h` on a text node is the wrapped box height, not a line count.**
   Poppins' natural line-height is ~1.59, Montserrat's ~1.22. Use
   `lineHeight: 'normal'` for subheadlines and let the browser decide.

5. **A frame with `overflow-clip` in Figma must clip in CSS too.** Android
   Phone screen 05's cards are wider than their frame; without the clip the
   content ran to the canvas edge.

6. **Figma's x positions drift.** Devices that were meant to be centred were
   4–35px off. Set `centreDevice: true` and let the renderer centre them. For a
   clipped frame, what gets centred is the PAINTED content, not the frame box.

7. **Fonts are already vendored** at 400/500/700 for Poppins and Montserrat.
   Do not re-vendor. Headlines 700, subheadlines 400, chips 500. The stack is
   `Poppins, Montserrat` — Poppins covers Latin, Montserrat picks up Cyrillic
   per-character.

8. **Download captures at native resolution** via `get_design_context`, whose
   asset URLs are the originals. `download_assets`' `rawImages` can hand back
   downscaled proxies (135×300 instead of 1080×2400).

## Screen kinds

- `phone` — headline, subheadline, one device mockup stacked below.
- `comparison` — headline, subheadline, two cards with chips. The cards may
  overlap (Android Phone, iPhone) or be separate (macOS); chips centre on the
  visible part of their own card either way. Cards may have per-panel widths
  via `panels[i].w`.
- `split` — text column beside the mockup, mockup centred on the canvas'
  vertical midline, text at its own `top`. macOS 03 is the worked example.
- `plain` — no copy at all: background, blobs, one capture. Used by Windows,
  whose captions live in Microsoft Store listing metadata rather than in the
  image.

## Per-screen overrides

Figma varies these per screen; all are optional and fall back to the platform
value:

- `top` — the headline's y. iPhone 05 sits at 186.53 against 131.15 on 01-04;
  macOS 04 at 150 against 99.
- `textLeft` — the text column's x.
- `headlineToSub`, `subToDevice` — the two gaps.
- `subWidth` — the subheadline's own box width.

A platform whose screens carry no copy sets `localised: false` and renders once
into `build/base/<id>/` instead of per locale, because the output is identical
in every language. Windows is the worked example (`src/config/windows.mjs`).

## What to write

Copy the shape of `android_phone.mjs`:

- `id`, `canvas` ({w, h, radius, bg}), `blobs`, `centreDevice: true`
- `figmaReference` — the English board's `headTop`, `headSize`, `subTop`,
  `devTop` per screen. **This is what `verify.mjs` checks against.** Record it
  from `get_design_context` before writing any layout code.
- `rhythm` — `preferred` from the English board, `min` as compression floors,
  `deviceMinScale`
- `text` — `left`, `maxWidth`, headline/sub families and colours
- `screens[]` — `id`, `key` (matches the yml key prefix), `kind`
  (`phone` | `comparison`), `headlineSize`, `textWidth`, `subWidth`,
  `headlineToSub`, `subToDevice`, and the device geometry
- `featureGraphic` only if the platform has one (Play only)

Screen `key` must match `assets/screenshotcopy/base/screenshot_copy.yml` — e.g.
`screenshot_copy_ipad_03_headline` means `key: 'ipad_03'`.

## Definition of done

```bash
node render.mjs --platform <id> --locale base
node verify.mjs        # must print "All checks passed."
```

`verify.mjs` checks the English render against your `figmaReference` (±1px) and
then checks every translated locale for: device centred (skipped for `split`),
right margin never tighter than the left, bottom margin ≥ `rhythm.minBottom`,
feature-graphic padding.

`minBottom` may be negative when the design deliberately bleeds off the bottom
edge, as macOS does — better than shrinking every mockup to manufacture a
margin the design does not want.

Only `base`, `it` and `ru` have real screenshot copy; the rest are empty in
Weblate and are skipped automatically.

Do **not** edit `render.mjs`, `verify.mjs`, `serve.mjs`, `src/render/page.mjs`
or `src/server.mjs`. If your platform genuinely cannot be expressed without
changing shared code, stop and say so rather than editing them — another agent
is working in the same tree.
