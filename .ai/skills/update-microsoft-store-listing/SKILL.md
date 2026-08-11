---
name: update-microsoft-store-listing
description: Read or change FeedFlow's Microsoft Store listing with pcenter — listing text, screenshots, locales, submissions and rollouts. Use when inspecting live Store copy, syncing listing text from assets/storecopy, replacing Store screenshots, adding or removing a Store listing language, or rescuing a stuck submission or package rollout.
---

# Update the Microsoft Store listing

The Store is driven by [`pcenter`](https://github.com/prof18/pcenter-cli), which replaced the
PowerShell scripts that used to live in `.github/scripts/`. Install locally with
`brew install prof18/tap/pcenter`.

## Before anything else

- **Reading is free and changes nothing.** Start here: `pcenter listing show --locale en-us`,
  `pcenter locales list`, `pcenter submission status`, `pcenter rollout status`,
  `pcenter app info`, `pcenter reviews list --from 2026-01-01 --all`.
- **Credentials** live in `~/.config/pcenter/credentials.env` locally — set up with
  `pcenter auth login`, checked with `pcenter auth doctor`. Never commit them. CI passes
  `MS_STORE_*` from repository secrets instead.
- **Only one pending submission can exist per app.** If a command reports one, inspect it
  with `pcenter submission status` before doing anything. `--replace-pending` deletes
  whatever draft is there, including one a human left for inspection.

## Where the copy lives

`assets/storecopy/<locale>/` — `microsoft_store_description.md` plus the `microsoft_store_*`
keys of `store_listing.json` — is the **source of truth**, maintained through the translation
pipeline. Nothing pcenter writes is authoritative.

`.pcenter/` is a **gitignored scratch directory**, the same idea as `.asc`. It is a snapshot
of what the Store currently holds: regenerate it with `pcenter listing pull --dir .pcenter`,
never commit it, and never hand-edit it expecting the edit to survive.

Release notes are separate and committed:
`assets/storecopy/microsoft-store-release-notes.json`, owned by the
`update-store-release-notes` skill. A Store locale missing from that file fails the publish
rather than shipping an empty changelog.

## Changing listing text

Generate the listing files rather than editing them:

```bash
pcenter listing pull --dir .pcenter
.scripts/generate-microsoft-store-listing.py --dir .pcenter --dry-run
.scripts/generate-microsoft-store-listing.py --dir .pcenter
pcenter listing push --dir .pcenter --dry-run
```

The generator skips locales the Store does not serve, and locales whose source is still the
English base text — it measures word overlap against `base`, because several
`microsoft_store_description.md` files are English with only a heading or two translated and
an equality check misses them. It never writes `title` for an existing locale.

`listing push` requires exactly one mode:

| Mode | Effect |
| --- | --- |
| `--dry-run` | Prints the diff, creates nothing. Safe even with a draft pending. |
| `--skip-commit` | Creates an inspectable draft in Partner Center. |
| `--yes` | Creates **and commits** — goes live after certification. |

**Always run `--dry-run` first, and never `--yes` unless the user explicitly asks.**

## Screenshots

One English set is shared by every language; the Windows Store does not need localized ones.

```bash
.scripts/generate-microsoft-store-listing.py --dir .pcenter --screenshots ~/path/to/shots
```

It deletes each locale's existing screenshots and uploads the new set, preserving the
`StoreLogoSquare` and the en-US captions. Image binaries **cannot be downloaded** from the
API, so the source PNGs must come from outside the repo — keep them somewhere findable.
Requirements: PNG, ≥1366×768, ≤50 MB, ≤10 per locale.

## Adding or removing a listing language

`--add <locale>` creates a listing file for a language the Store does not serve yet. It
copies the reserved product name into `title`, which is **required**: a listing language the
packages do not include has no package to draw its name from, and omitting it fails the whole
submission with `MissingTitle`.

A language is "additional" in Partner Center when the MSIX does not declare it — the package
languages come from `.github/msix-resources-template.xml`. Add a language there to make it
package-supported on the next Windows release.

Removing a locale needs `--allow-locale-removal` on top of the mode flag, so a deleted file
cannot silently drop a Store language.

## Store limits pcenter checks before creating a submission

- `shortDescription` ≤ **500** characters — Microsoft's published docs wrongly say 1,000.
- At most **21 locales may carry keywords** at all. Not per locale, not a keyword total: a
  22nd locale with keywords is rejected outright, so one must be cleared to make room.
- `description` ≤ 10,000; `features` ≤ 20 items.

## When something is stuck

- **Rollout** (the 2026-07-08 class of failure): `pcenter rollout status`, then
  `pcenter rollout finalize`. Every mutation verifies the resulting state, because the Store
  API returns 504 for operations that in fact succeeded.
- **A failed submission** (`CommitFailed`, `CertificationFailed`) still occupies the single
  pending slot. `pcenter submission delete-draft --yes` clears it.
- **Watching**: `pcenter submission watch`. Running out of poll attempts is not a failure —
  certification takes hours — it reports `in-progress` and exits 0.

## Exit codes

`2` fix the invocation or config · `3` credentials rejected · `4` invalid for the current
state, never retry unchanged · `5` throttled. Output is JSON when piped.

Full reference: [pcenter docs](https://github.com/prof18/pcenter-cli/tree/main/docs).

## Guardrails

- Do not push listing changes or commit a submission unless the user explicitly asks.
- Never `--yes` without an explicit request; `--skip-commit` leaves a draft they can review.
- Do not translate store copy. If a locale's source is still English, report it — the
  generator will refuse it anyway.
