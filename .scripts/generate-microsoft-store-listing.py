#!/usr/bin/env python3
"""Generate pcenter listing files from FeedFlow's canonical store copy.

`assets/storecopy/<locale>/` is the source of truth for Store text; pcenter reads
a different shape (one JSON file per locale under `<dir>/listings/`). This script
is the bridge, so a Microsoft Store sync is a command rather than transcription.

    .scripts/generate-microsoft-store-listing.py --dir .pcenter

It writes only locales that are (a) present in the pulled directory, meaning the
Store actually serves them, and (b) genuinely translated. Everything else is
reported and skipped. Run `pcenter listing pull --dir .pcenter` first: the
existing files are what the generated ones are diffed against, and the locale set
comes from there.

Then review and push yourself:

    pcenter listing push --dir .pcenter --dry-run
    pcenter listing push --dir .pcenter --skip-commit

Sources per locale:

    microsoft_store_description.md          -> description
    store_listing.json
      microsoft_store_short_description     -> shortDescription
      microsoft_store_keywords              -> keywords   (comma-separated)
      microsoft_store_product_features      -> features   (already a list)

`title` is never written for an existing locale: it is a *reserved product name*
chosen in Partner Center, not free text, so overwriting it would rename the
listing. A locale being **added** is the exception — the Store rejects the whole
submission with `MissingTitle` if a new listing has none — so `--add` copies the
reserved name already in use by the primary locale.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import re
import shutil
import sys

STORECOPY = pathlib.Path("assets/storecopy")
BASE = "base"

# The English source lives in `base`, not in an `en-us` directory. Mapping it is
# also the pipeline's self-check: the live en-us listing is byte-identical to
# base today, so generating en-us must report "already matches the source". If it
# ever reports a change, either the Store drifted or this script broke.
LOCALE_SOURCES = {"en-us": BASE}

# Exceeding these is a copy problem to fix at the source, not something to
# discover from a 400 at push time.
#
# shortDescription is 500, NOT the 1000 the public "Add and edit Store listing
# info" page states: the Ingestion API rejects 501+ with "The length of
# ShortDescription must be 500 or less" (observed 2026-08-09). Trust the API.
LIMITS = {
    "description": 10_000,
    "shortDescription": 500,
}
MAX_FEATURES = 20
MAX_FEATURE_CHARS = 200

# The API caps the number of *locales carrying keywords* at 21 —
# "The size of KeywordsTotalCount must be 21 or less". It is not a per-locale
# limit and not the total number of keywords (which is far higher in practice).
# Undocumented; found by hitting it on 2026-08-09 when adding a 22nd.
MAX_KEYWORD_LOCALES = 21


def unwrap(text: str) -> str:
    """Undo the hard wrapping in the per-locale markdown.

    The Store stores description text unwrapped; the per-locale files in this
    repo are wrapped at ~80 columns, with continuation lines for long bullets.
    `base` happens to be unwrapped already, so this is a no-op there — which is
    what makes it testable: unwrapping base must reproduce the live text exactly.

    Paragraphs (blank-line separated) are preserved. Inside a paragraph, a line
    starting with "- " begins a new bullet; anything else continues the line
    before it.
    """
    paragraphs = re.split(r"\n\s*\n", text.strip())
    out = []
    for paragraph in paragraphs:
        lines: list[str] = []
        for raw in paragraph.split("\n"):
            stripped = raw.strip()
            if not stripped:
                continue
            if stripped.startswith("- ") or not lines:
                lines.append(stripped)
            else:
                lines[-1] += " " + stripped
        out.append("\n".join(lines))
    return "\n\n".join(out)


def normalize(text: str) -> str:
    """Whitespace-insensitive form, for deciding whether two texts are the same."""
    return re.sub(r"\s+", " ", text or "").strip()


def english_word_share(text: str, base: str) -> float:
    """How much of `text` is still the English source.

    Exact equality with base is not enough to spot an untranslated file: both the
    Greek and Latvian descriptions had a couple of headings translated and the
    rest left in English, which slipped straight past an equality check and put
    two English listings live under other languages.

    Word overlap separates them cleanly. Measured 2026-08-11: genuinely
    translated Russian shares 13.5% of its words with the English base, while
    da/el/fi/it/lv sit between 85% and 100%. Anything above the threshold is the
    source text wearing a locale's name.

    Word-level rather than character-level on purpose: character similarity is
    inflated for Latin-script languages by shared punctuation, structure and
    brand names, which is exactly where the false negatives were.
    """
    pattern = r"[A-Za-zÀ-ÿĀ-ſА-яͰ-Ͽ]{4,}"
    words = set(re.findall(pattern, text.lower()))
    base_words = set(re.findall(pattern, base.lower()))
    if not words:
        return 1.0
    return len(words & base_words) / len(words)


# Above this share of English words, treat the file as untranslated. Well clear
# of both observed clusters (13.5% translated vs 85%+ untranslated).
UNTRANSLATED_WORD_SHARE = 0.8


def split_keywords(value: str) -> list[str]:
    return [part.strip() for part in (value or "").split(",") if part.strip()]


def stage_screenshots(metadata_dir: pathlib.Path, source: pathlib.Path, locales: list[str], dry_run: bool) -> list[str]:
    """Point every locale at one shared screenshot set.

    The Windows Store listing uses the same English screenshots for every
    language, so this deletes each locale's current screenshots and uploads the
    given set in their place. Two things are deliberately preserved:

    - Non-screenshot images (the StoreLogoSquare), which are not screenshots and
      must not be swept up by a screenshot replacement.
    - en-US captions, which are the only captions the listing has; a
      delete-and-reupload would otherwise drop them silently.

    Store images uploaded through Partner Center carry server-side filenames and
    cannot be updated in place, only replaced — so even an identical file shows
    up as a delete plus an upload the first time it comes under management.
    """
    shots = sorted(p for p in source.glob("*.png"))
    if not shots:
        raise SystemExit(f"error: no PNGs in {source}")

    manifest_path = metadata_dir / "images-manifest.json"
    manifest = json.loads(manifest_path.read_text())
    images = manifest["images"]

    captions = [e.get("description", "") for e in images.get("en-us", []) if e.get("imageType") == "Screenshot"]

    report = []
    for locale in locales:
        entries = images.get(locale, [])
        kept = [e for e in entries if e.get("imageType") != "Screenshot"]
        deletions = [
            {"imageType": e["imageType"], "storeId": e["storeId"], "delete": True}
            for e in entries
            if e.get("imageType") == "Screenshot" and e.get("storeId")
        ]

        uploads = []
        destination = metadata_dir / "images" / locale
        for index, shot in enumerate(shots):
            entry = {"localPath": f"{locale}/{shot.name}", "imageType": "Screenshot"}
            if locale == "en-us" and index < len(captions) and captions[index]:
                entry["description"] = captions[index]
            if not dry_run:
                destination.mkdir(parents=True, exist_ok=True)
                target = destination / shot.name
                shutil.copyfile(shot, target)
                entry["sha256"] = hashlib.sha256(target.read_bytes()).hexdigest()
            uploads.append(entry)

        if not dry_run:
            images[locale] = kept + deletions + uploads
        report.append(f"{locale}: {len(deletions)} deleted, {len(uploads)} uploaded, {len(kept)} kept")

    if not dry_run:
        manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n")
    return report


def read_source(locale: str) -> tuple[str | None, dict]:
    directory = STORECOPY / LOCALE_SOURCES.get(locale, locale)
    description_path = directory / "microsoft_store_description.md"
    listing_path = directory / "store_listing.json"
    description = description_path.read_text() if description_path.exists() else None
    listing = json.loads(listing_path.read_text()) if listing_path.exists() else {}
    return description, listing


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--dir", required=True, help="pcenter metadata directory, already populated by `listing pull`")
    parser.add_argument("--locale", action="append", help="limit to one locale; repeatable")
    parser.add_argument(
        "--add",
        action="append",
        metavar="LOCALE",
        help="create a listing file for a locale the Store does not serve yet, adding it as a "
        "new Store language on the next push; repeatable",
    )
    parser.add_argument(
        "--screenshots",
        metavar="DIR",
        help="replace every locale's screenshots with the PNGs in DIR (the Windows Store uses "
        "one English set for all languages); non-screenshot images and en-US captions are kept",
    )
    parser.add_argument("--dry-run", action="store_true", help="report what would change without writing")
    args = parser.parse_args()

    listings_dir = pathlib.Path(args.dir) / "listings"
    if not listings_dir.is_dir():
        print(f"error: {listings_dir} does not exist - run `pcenter listing pull --dir {args.dir}` first", file=sys.stderr)
        return 2
    if not STORECOPY.is_dir():
        print(f"error: {STORECOPY} does not exist - run this from the repository root", file=sys.stderr)
        return 2

    base_description, _ = read_source(BASE)
    if base_description is None:
        print(f"error: {STORECOPY / BASE / 'microsoft_store_description.md'} is missing", file=sys.stderr)
        return 2
    base_normalized = normalize(base_description)  # kept for the en-us self-check

    # The Store's locale set, not the repo's: writing a file for a locale the
    # Store does not serve would silently add a language. --add is how you say
    # you meant to.
    store_locales = sorted(p.stem for p in listings_dir.glob("*.json"))
    adding = [locale.lower() for locale in (args.add or [])]
    for locale in adding:
        if locale in store_locales:
            print(f"error: {locale} is already a Store locale - drop --add {locale}", file=sys.stderr)
            return 2
        if not (STORECOPY / LOCALE_SOURCES.get(locale, locale)).is_dir():
            print(f"error: no assets/storecopy/{locale}/ to add {locale} from", file=sys.stderr)
            return 2
    store_locales = sorted(set(store_locales) | set(adding))
    wanted = {locale.lower() for locale in args.locale} if args.locale else None

    # The reserved product name, taken from whichever locales already carry one.
    # Every locale that has a title uses the same value; a new locale must reuse
    # it rather than inventing a name that was never reserved.
    titles = {
        json.loads(p.read_text()).get("title", "")
        for p in listings_dir.glob("*.json")
        if json.loads(p.read_text()).get("title")
    }
    reserved_title = sorted(titles)[0] if titles else ""
    if adding and not reserved_title:
        print("error: no existing locale carries a title, so the reserved product name is unknown", file=sys.stderr)
        return 2

    written, skipped, problems, warnings = [], [], [], []

    for locale in store_locales:
        if wanted and locale not in wanted:
            continue
        description, listing = read_source(locale)
        if description is None:
            skipped.append((locale, "no microsoft_store_description.md in assets/storecopy"))
            continue
        # A locale whose source *is* base (en-us) is legitimately English; every
        # other locale matching base is an untranslated placeholder, and writing
        # it would replace a translated live listing with English.
        if locale not in LOCALE_SOURCES:
            share = english_word_share(description, base_description)
            if share > UNTRANSLATED_WORD_SHARE:
                skipped.append((locale, f"source is still English ({share:.0%} of its words are the English source) - not translated"))
                continue

        target = listings_dir / f"{locale}.json"
        if target.exists():
            current = json.loads(target.read_text())
        else:
            # A brand new Store language. It must carry a title or the Store
            # rejects the submission with `MissingTitle`, so it takes the
            # reserved product name the existing locales use. Everything else
            # starts empty rather than being copied from another locale, so
            # nothing is inherited by accident.
            current = {
                "title": reserved_title,
                "description": "",
                "shortDescription": "",
                "features": [],
                "keywords": [],
                "copyrightAndTrademarkInfo": "",
                "licenseTerms": "",
                "recommendedHardware": [],
                "minimumHardware": [],
            }

        generated = dict(current)
        generated["description"] = unwrap(description)
        if "microsoft_store_short_description" in listing:
            generated["shortDescription"] = normalize(listing["microsoft_store_short_description"])
        if "microsoft_store_keywords" in listing:
            generated["keywords"] = split_keywords(listing["microsoft_store_keywords"])
        if "microsoft_store_product_features" in listing:
            # Blank entries are untranslated scaffolding, not features. Sending
            # them produces an empty list on the Store anyway; dropping them here
            # makes the omission visible instead of silent.
            features = [normalize(f) for f in listing["microsoft_store_product_features"]]
            kept = [f for f in features if f]
            if len(kept) != len(features):
                warnings.append(
                    f"{locale}: {len(features) - len(kept)} of {len(features)} product features are blank in the source - not translated"
                )
            generated["features"] = kept

        # A keyword containing a colon is almost always a translation artifact:
        # the translator rendered the field label ("Keywords:") into the value.
        # It is not fatal, but it silently wastes one of seven keyword slots.
        for keyword in generated.get("keywords") or []:
            if ":" in keyword:
                warnings.append(f"{locale}: keyword {keyword!r} contains a colon - looks like a translated field label, check the source")

        for field, limit in LIMITS.items():
            value = generated.get(field) or ""
            if len(value) > limit:
                problems.append(f"{locale}: {field} is {len(value)} characters, over the {limit} limit")
        if len(generated.get("features") or []) > MAX_FEATURES:
            problems.append(f"{locale}: {len(generated['features'])} features, maximum is {MAX_FEATURES}")
        for feature in generated.get("features") or []:
            if len(feature) > MAX_FEATURE_CHARS:
                problems.append(f"{locale}: a feature is {len(feature)} characters, maximum is {MAX_FEATURE_CHARS}")

        changed = [k for k in ("description", "shortDescription", "keywords", "features") if current.get(k) != generated.get(k)]
        if not changed:
            skipped.append((locale, "already matches the source"))
            continue
        written.append((locale, changed))
        if not args.dry_run:
            target.write_text(json.dumps(generated, ensure_ascii=False, indent=2) + "\n")

    # Checked across the whole directory, not per locale: the cap is on how many
    # locales carry keywords at all, so adding one to a 21st locale fails even
    # though that locale is individually fine.
    keyword_locales = sorted(
        p.stem for p in listings_dir.glob("*.json") if json.loads(p.read_text()).get("keywords")
    )
    if len(keyword_locales) > MAX_KEYWORD_LOCALES:
        problems.append(
            f"{len(keyword_locales)} locales carry keywords, and the Store allows "
            f"{MAX_KEYWORD_LOCALES}. Clear keywords on a locale to make room: {', '.join(keyword_locales)}"
        )

    if args.screenshots:
        for line in stage_screenshots(pathlib.Path(args.dir), pathlib.Path(args.screenshots), store_locales, args.dry_run):
            print(("would stage " if args.dry_run else "staged ") + line)

    verb = "would update" if args.dry_run else "updated"
    for locale, changed in written:
        print(f"{verb} {locale}: {', '.join(changed)}")
    for locale, reason in skipped:
        print(f"skipped {locale}: {reason}")

    if warnings:
        print("\nwarnings:", file=sys.stderr)
        for warning in warnings:
            print(f"  {warning}", file=sys.stderr)

    if problems:
        print("\nsource copy is over Store limits - fix the source, do not push:", file=sys.stderr)
        for problem in problems:
            print(f"  {problem}", file=sys.stderr)
        return 1

    if not written:
        print("\nnothing to change.")
        return 0

    print(f"\n{len(written)} locale(s) {verb}. Review before pushing:")
    print(f"  pcenter listing push --dir {args.dir} --dry-run")
    return 0


if __name__ == "__main__":
    sys.exit(main())
