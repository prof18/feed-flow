---
name: update-store-release-notes
description: Update and localize FeedFlow release notes when the user provides platform-specific changelog copy. Use for Android Google Play, iOS App Store, macOS App Store, and Windows Microsoft Store release-note updates, including requests to translate notes for every configured store locale.
---

# Update FeedFlow Store Release Notes

Use the user-provided copy as the source of truth. Preserve platform-specific wording; do not force one platform's copy onto another.

## Workflow

1. Inspect the target artifacts, locale set, and current working-tree status before editing.
2. Replace the English source and translate it for every configured target locale. Do not invent feature claims or omit user-supplied bullets.
3. Keep the writing short, user-facing, and benefit-led. Preserve bullet formatting when supplied.
4. Validate the exact artifact format and limits. Run `git diff --check`. Run the smallest relevant build or validation only when the changed artifact requires it.

Respect a user request to translate store copy even if general project localization guidance normally delegates translations.

## Android: Google Play

- Write production notes to `androidApp/src/googlePlay/play/release-notes/<locale>/production.txt`.
- `en-US/production.txt` is the English source. Every existing locale directory is a target; create `production.txt` in empty locale directories.
- Keep each note at or below 500 characters: `wc -m androidApp/src/googlePlay/play/release-notes/*/production.txt`.
- Do not edit `alpha.txt` unless the user explicitly requests alpha-track notes.

## iOS and macOS: App Store Connect

- Treat iOS and macOS as separate products: accept and maintain separate platform copy.
- Inspect App Store Connect first to discover the version and its actual localization set. Use `asc` to pull the relevant version localizations, update each locale's `whatsNew`, then pull/validate again to confirm the remote result.
- Use `en-US` as the source locale and translate only into locales exposed for that specific product/version. Do not create an App Store task for a locale App Store Connect does not support.
- Use the platform's field limits reported by `asc`; shorten translations as needed while retaining all material points.
- Never upload or submit a build as part of a text-only release-note request unless the user explicitly asks.

## Windows: Microsoft Store

- Update `assets/storecopy/microsoft-store-release-notes.json`.
- Keep the top-level `notes` object. Each locale value is an array of non-empty bullet strings; the Store publishing script joins the array with newlines.
- Match every Partner Center listing locale. The publisher validates missing locales case-insensitively and warns about unused ones. Discover the live set when credentials are available rather than assuming a static list.
- The publishing workflow passes this file to `.github/scripts/publish-msix-to-store.ps1`; validate the JSON before handoff, for example with `jq empty assets/storecopy/microsoft-store-release-notes.json`.
- Do not publish or commit the Microsoft Store submission unless the user explicitly asks; editing the local release-note file is sufficient.

## Final Report

State the platforms and locale count updated, list any unavailable or unsupported locales, and report the validations actually run.
