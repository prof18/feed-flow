#!/usr/bin/env python3
"""Update Google Play listing files from storecopy source assets.

Usage:
    update-play-listing.py <project-root> [language|all]

Examples:
    update-play-listing.py /path/to/feed-flow en      # English only
    update-play-listing.py /path/to/feed-flow it      # Italian only
    update-play-listing.py /path/to/feed-flow all     # All languages
"""

import json
import os
import shutil
import sys

# Mapping: storecopy language code -> Google Play listing language code
LANG_MAP = {
    "base": "en-US",
    "bg": "bg",
    "de": "de-DE",
    "es": "es-ES",
    "et": "et",
    "fr": "fr-FR",
    "gl": "gl-ES",
    "hu": "hu-HU",
    "it": "it-IT",
    "iw": "iw-IL",
    "ja": "ja-JP",
    "pt-BR": "pt-BR",
    "ru": "ru-RU",
    "sk": "sk",
    "ta": "ta-IN",
    "uk": "uk",
    "vi": "vi",
    "zh-CN": "zh-CN",
}

# Reverse mapping for resolving listing codes to storecopy codes
REVERSE_MAP = {v: k for k, v in LANG_MAP.items()}


def update_language(project_root, storecopy_lang):
    listing_lang = LANG_MAP.get(storecopy_lang)
    if not listing_lang:
        print(f"  WARNING: No listing mapping for '{storecopy_lang}', skipping.")
        return

    src_dir = os.path.join(project_root, "assets", "storecopy", storecopy_lang)
    dst_dir = os.path.join(
        project_root, "androidApp", "src", "googlePlay", "play", "listings", listing_lang
    )

    if not os.path.isdir(src_dir):
        print(f"  WARNING: Source '{src_dir}' does not exist, skipping.")
        return
    if not os.path.isdir(dst_dir):
        print(f"  WARNING: Listing '{dst_dir}' does not exist, skipping.")
        return

    json_file = os.path.join(src_dir, "store_listing.json")
    desc_file = os.path.join(src_dir, "google_play_description.md")

    if os.path.isfile(json_file):
        with open(json_file, "r", encoding="utf-8") as f:
            data = json.load(f)

        title = data.get("title", "").strip()
        short_desc = data.get("google_play_short_description", "").strip()

        if title:
            with open(os.path.join(dst_dir, "title.txt"), "w", encoding="utf-8") as f:
                f.write(title + "\n")
            print(f"  Updated {listing_lang}/title.txt")
        else:
            print(f"  Skipped {listing_lang}/title.txt (empty)")

        if short_desc:
            with open(
                os.path.join(dst_dir, "short-description.txt"), "w", encoding="utf-8"
            ) as f:
                f.write(short_desc + "\n")
            print(f"  Updated {listing_lang}/short-description.txt")
        else:
            print(f"  Skipped {listing_lang}/short-description.txt (empty)")
    else:
        print(f"  No store_listing.json for {storecopy_lang}")

    if os.path.isfile(desc_file):
        shutil.copy2(desc_file, os.path.join(dst_dir, "full-description.txt"))
        print(f"  Updated {listing_lang}/full-description.txt")
    else:
        print(f"  No google_play_description.md for {storecopy_lang}, skipping full description")


def resolve_storecopy_lang(project_root, lang):
    storecopy_dir = os.path.join(project_root, "assets", "storecopy")

    # Direct match
    if os.path.isdir(os.path.join(storecopy_dir, lang)):
        return lang
    # "en" -> "base"
    if lang == "en":
        return "base"
    # Match by listing code (e.g., "en-US" -> "base", "it-IT" -> "it")
    if lang in REVERSE_MAP:
        return REVERSE_MAP[lang]
    return None


def main():
    if len(sys.argv) < 2:
        print("Usage: update-play-listing.py <project-root> [language|all]")
        sys.exit(1)

    project_root = sys.argv[1]
    lang_arg = sys.argv[2] if len(sys.argv) > 2 else "all"
    storecopy_dir = os.path.join(project_root, "assets", "storecopy")

    if lang_arg == "all":
        print("Updating all available languages...")
        for entry in sorted(os.listdir(storecopy_dir)):
            entry_path = os.path.join(storecopy_dir, entry)
            if os.path.isdir(entry_path):
                print(f"Processing: {entry}")
                update_language(project_root, entry)
        print("Done.")
    else:
        resolved = resolve_storecopy_lang(project_root, lang_arg)
        if not resolved:
            available = [
                e
                for e in os.listdir(storecopy_dir)
                if os.path.isdir(os.path.join(storecopy_dir, e))
            ]
            print(f"ERROR: Unknown language '{lang_arg}'.")
            print(f"Available: {', '.join(sorted(available))}")
            print("Also accepts: en, or listing codes like en-US, it-IT")
            sys.exit(1)

        listing_lang = LANG_MAP.get(resolved, "?")
        print(f"Updating: {resolved} (-> {listing_lang})")
        update_language(project_root, resolved)
        print("Done.")


if __name__ == "__main__":
    main()
