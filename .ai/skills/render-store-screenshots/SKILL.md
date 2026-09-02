---
name: render-store-screenshots
description: Render FeedFlow store screenshots for one or more locales from repo copy, and verify every screen keeps its breathing room — top padding, gaps, and a full-size headline. Use when new translations land in assets/screenshotcopy, when screenshot copy changes, or when checking that a language still fits before a store upload.
---

# Render store screenshots

Generates the App Store / Play Store screenshots from the copy in this repo.
No design tool is involved — `tools/screenshots/` is the source of truth.

**All six platforms are here**: `android_phone`, `android_tablet`, `iphone`,
`ipad`, `macos`, `windows`. The Figma blueprint is no longer authored — do not
edit its boards. (The old `figma-screenshot-localization` skill was removed;
`git log -- .ai/skills/figma-screenshot-localization` recovers it if ever
needed.)

## The rule this skill exists to enforce

A screenshot must never be cramped. Breathing room is protected in this order,
and the renderer gives ground from the top of the list first:

1. Shrink the device a few percent.
2. Close the gap above the device (floor 60px).
3. Close the gap under the headline (floor 20px).
4. Give up top margin — **floor 88px, never less**.
5. Shrink the headline (floor 60% of base).

Top padding is defended near the end deliberately: a headline crowded against
the canvas edge looks worse than a slightly smaller phone. The Play feature
graphic follows the same principle — its headline is vertically centred with a
40px minimum above and below, shrinking the type rather than filling the canvas
edge to edge.

If you are tempted to fix a cramped screen by lowering a floor, that is the
wrong lever. Shorten the copy, or let the device shrink further.

## Workflow

### 1. Confirm the copy is complete

Weblate seeds every key into a new locale file with an **empty value**, so a
file with all 63 keys can still be entirely untranslated. Key presence is not
completeness — check the values:

```bash
for d in assets/screenshotcopy/*/; do
  l=${d%/}
  printf '%-8s %s empty\n' "$(basename "$l")" \
    "$(grep -cE '^screenshot_copy_[a-z0-9_]+: *""$' "$l/screenshot_copy.yml")"
done
```

A locale with any empty value is **skipped** by the renderer and listed under
"Skipped — copy not translated yet". That is correct behaviour, not a failure:
rendering it would ship a screenshot with no words on it. A locale with keys
genuinely *missing* (not just empty) is a hard error naming the keys.

Do **not** invent translations — the project's i18n rules forbid it. Report
which locales are waiting on copy and stop.

Feature-graphic copy carries hand-authored line breaks (a YAML `|-` block). Keep
the line count close to the English original; more lines means smaller type.

### 2. Render

```bash
cd tools/screenshots
node render.mjs --locale <locale>     # one locale
node render.mjs --all                 # every locale
```

Output: `tools/screenshots/build/<locale>/<platform>/<screen>.png`, at exact
store dimensions — PNG, which is what both the App Store and Play require.

The preview (step 5) also has **Export** buttons that run the same render, for
when the user would rather click than type.

### 3. Read the warnings — this is the actual check

The run ends with a warning list. Interpret it as follows:

| Warning | Meaning | Action |
|---|---|---|
| *(none)* | Full preferred spacing | Ship it |
| `device scaled to 92–99%` | Normal, invisible | Ship it |
| `device scaled to 80–91%` | Device at or near its floor | Eyeball it |
| `top margin reduced` | Every earlier lever exhausted | Inspect; consider shorter copy |
| `headline shrunk to Npx` | Last resort before overflow | Inspect |
| `OVERFLOWS canvas` | Does not fit | **Never ship.** Copy must get shorter |

A locale that renders with no warnings, or device-scale warnings only, needs no
visual check. Anything further down the table does.

### 4. Verify the geometry

```bash
node verify.mjs
```

Checks the rendered English layout against the frozen Figma English board
(headline top, headline size, subheadline top, device top) and then checks
margins on **every** locale: device centred, right margin never tighter than
the left, bottom margin, and feature-graphic padding. Exits non-zero on any
failure. Run it after touching any geometry in `src/config/`.

Figma's own device positions drifted off-centre (screens 03 and 04 by 4px,
screen 05 by 35px) while 01 and 02 were centred to half a pixel. We centre all
of them, so horizontal position is checked against *centred*, not against
Figma. Do not "fix" this by restoring Figma's x values.

### 5. Look at what the warnings flagged

```bash
node serve.mjs      # http://127.0.0.1:4321/?platform=<id>&locale=<locale>
```

The toolbar switches platform (first row) and locale (second row), and has
Export buttons for the current locale or every translated locale.

The preview renders the same page the exporter screenshots, so what you see is
what ships. Config and template edits apply on reload — no restart needed.

Check the flagged screens for: headline crowding the top edge, subheadline
touching the device, a pill overlapping a card edge, and anything past the
canvas bottom.

### 6. Report

State per locale: screens rendered, the worst adjustment applied, and whether
anything needs shorter copy. Name the output path. Do not upload to any store —
that is a separate, explicit request.

## When a language genuinely does not fit

In order of preference:

1. **Shorten the translation.** Usually correct — store screenshot copy should
   be short in every language.
2. **Lower `deviceMinScale`** for that platform (default `0.8`) if the phone can
   afford to be smaller on every locale.
3. **Add a per-locale override.** Not built yet — build it only when a real
   language needs it, rather than pre-emptively.

Never hand-edit a PNG in `build/`; it is regenerated on every run.

## Adding a platform

One `src/config/<platform>.mjs` plus captures under
`assets/captures/<platform>/`, registered in `render.mjs`. Geometry comes from
the Figma English board (the frozen reference), never the ru-RU export — the
ru-RU values are compression floors, not defaults. See
`tools/screenshots/README.md`.
