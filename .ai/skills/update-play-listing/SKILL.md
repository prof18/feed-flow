---
name: update-play-listing
description: Update Google Play listing files (title, short description, full description) from storecopy source assets. Use when the user wants to update Play Store listings for a specific language or all languages, sync store copy to listing files, or refresh Play Store translations.
---

# Update Play Listing

Sync Google Play listing files from the storecopy source of truth.

## Source Files

All source data lives in `assets/storecopy/`:
- `{lang}/store_listing.json` — contains `title` and `google_play_short_description`
- `{lang}/google_play_description.md` — full description (plain text, not rendered as markdown)
- English uses `base/` as the language folder

## Target Files

Listing files live in `androidApp/src/googlePlay/play/listings/{listing-lang}/`:
- `title.txt`
- `short-description.txt`
- `full-description.txt`

## How to Run

Run the bundled script from the project root:

```bash
# Single language
python3 .claude/skills/update-play-listing/scripts/update-play-listing.py "$(pwd)" en
python3 .claude/skills/update-play-listing/scripts/update-play-listing.py "$(pwd)" it

# All languages
python3 .claude/skills/update-play-listing/scripts/update-play-listing.py "$(pwd)" all
```

Accepts: storecopy codes (`base`, `it`), short codes (`en`), or listing codes (`en-US`, `it-IT`).

## Language Mapping

The script maps storecopy language codes to Google Play listing codes. When adding a new language, update the `LANG_MAP` dict in `scripts/update-play-listing.py`.

## Notes

- If a storecopy language has no `google_play_description.md`, only title and short description are updated.
- Empty fields in `store_listing.json` are skipped (not written as empty files).
- The script warns and skips if the source or target directory does not exist.
