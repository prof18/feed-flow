---
name: translation-release-audit
description: Audit FeedFlow localization, store listing, live App Store/Play Store listing state, and screenshot-copy changes. Use when checking whether new app translations were added or completed, whether store copy/localized listings changed, whether live store metadata is stale, or whether screenshots need to be regenerated or uploaded.
---

# Translation Release Audit

Run this audit when the user wants a release-readiness signal for translations, live store listings, or screenshots.

## Scope

Check these FeedFlow paths:

- App strings: `i18n/src/commonMain/resources/locale/values*/strings.xml`
- Generated locale code: `i18n/src/commonMain/kotlin/com/prof18/feedflow/i18n/Locales.kt`, `i18n/src/commonJvmAndroidMain/kotlin/com/prof18/feedflow/i18n/Locales.commonJvmAndroid.kt`, `i18n/src/iosMain/kotlin/com/prof18/feedflow/i18n/Locales.ios.kt`
- Source store copy: `assets/storecopy/**`
- Source screenshot copy: `assets/screenshotcopy/**`
- Google Play listing output: `androidApp/src/googlePlay/play/listings/**`
- Website/store screenshot assets when changed: `assets/linux-screenshots/**`, `website/static/images/screenshots/**`, `website/data/screenshots.yml`

Optional live-store checks:

- Google Play Console listing, only when a reliable read path is available
- App Store Connect metadata and screenshots, using `asc`
- Microsoft Store (Partner Center) listing, using `pcenter`

## Workflow

1. Inspect the relevant git window.
   - For a weekly audit, compare changes from the last 7 days with `git log --since="7 days ago" --name-status -- <paths>`.
   - If there are uncommitted changes, include `git status --short` and `git diff --name-status -- <paths>` separately.
   - For a PR or branch audit, compare against the merge base with the target branch when known.
2. Classify changes into:
   - `App translations`: new locale folders, modified `strings.xml`, removed strings, or regenerated locale code.
   - `Completed translations`: locales whose `strings.xml` now has the same string-key set as `values/strings.xml`, ignoring files outside the app-string path.
   - `Store copy`: changes under `assets/storecopy` or generated Play listing text files.
   - `Completed store copy`: locales under `assets/storecopy/<locale>/` whose store-facing source copy is fully translated and ready in the codebase compared with `assets/storecopy/base/`.
   - `Screenshot copy`: changes under `assets/screenshotcopy`.
   - `Completed screenshot copy`: locales under `assets/screenshotcopy/<locale>/` whose screenshot text is fully translated and ready in the codebase compared with `assets/screenshotcopy/base/`.
   - `Screenshot assets`: changed PNG/WebP screenshot files or screenshot metadata.
3. For app translations, compare every changed locale file against `i18n/src/commonMain/resources/locale/values/strings.xml`.
   - Report missing keys and extra keys by locale.
   - Treat a locale as completed only when no keys are missing.
4. For store copy, flag the operational action:
   - Source copy changed under `assets/storecopy`: Play/App/Microsoft store metadata may need syncing or upload only when the changed locale's store copy is fully translated in the codebase.
   - Generated Play listing text changed under `androidApp/src/googlePlay/play/listings`: Google Play metadata has changed and may need upload.
   - Distinguish these cases before writing follow-up:
     - `source == live store` and `generated output != source`: the live store is current; update the generated local output and do not create a store-upload TODO.
     - `source != live store`: the live store is stale; create a TODO that names the exact store and fields to sync.
     - `source complete` but no generated/local/live locale exists: create a TODO that names the missing locale/output/store setup.
   - Before creating a store sync/upload card, validate source field limits for the target store. If complete source copy exceeds a store limit, do not create a sync/upload card; create a source-copy fix card naming the exact over-limit fields and limits.
5. For screenshot copy/assets, flag the operational action:
   - `assets/screenshotcopy` changed: localized screenshot generation may be needed only when the changed locale's screenshot copy is fully translated in the codebase.
   - Store listing screenshot images changed: store screenshots may need upload.
   - Website screenshot assets changed: website screenshot gallery/hero may need deploy.
6. If live store checking is requested or this is the weekly automation, compare against current store state when credentials are available.
   - Google Play: do not use Gradle Play Publisher bootstrap for the recurring audit until its graphics download hang is fixed. Report Google Play live state as unavailable when no other reliable read-only path exists, and continue with checked-in listing output/source-copy checks.
   - App Store: pull metadata/screenshots into a temp directory and compare against FeedFlow's source store/screenshot copy where mapping is clear.
   - Microsoft Store: read the live listing with `pcenter listing show` and compare against `assets/storecopy/<locale>/`.
7. Add follow-up TODOs to the FeedFlow Obsidian board when the audit finds actionable store release work.
   - Board path: `/Users/mg/Workspace/Notes/projects/feed-flow/feed-flow-board.md`.
   - Use the `## marketing` lane for store listing, ASO, screenshot, and store localization release work.
   - Preserve the kanban Markdown format: add one `- [ ] ...` item per action under the lane.
   - Keep TODO text short and concrete, but include the exact thing to change: store, locale, and field or asset type. Examples:
     - `- [ ] Sync App Store Russian subtitle, keywords, and promotional text`
     - `- [ ] Generate Google Play Latvian listing text output`
     - `- [ ] Upload App Store Latvian metadata`
     - `- [ ] Regenerate App Store Russian iPhone/iPad screenshots`
     - `- [ ] Update checked-in Google Play Russian title, short description, and full description from live/source`
   - Do not add TODOs just because a translation was created or changed. Add TODOs only after verifying the relevant source copy is complete in the repo: store metadata cards require complete `assets/storecopy/<locale>/` content; screenshot cards require complete `assets/screenshotcopy/<locale>/` content.
   - Keep store metadata follow-up and screenshot generation/upload follow-up as separate board cards when both apply.
   - Do not add TODOs for app-string translation gaps, incomplete store/screenshot copy, non-actionable status, credential setup noise, or findings already represented by an existing unchecked board item.
   - If the board is unavailable or cannot be edited, include the TODO text in the final report under `Action needed` instead.

## Live Store Checks

### Google Play

Use the repository's dedicated Publishing API audit script. The API requires a Play
edit for listing/image reads, so it creates one disposable uncommitted edit, reads the
content into a temporary directory, then deletes the edit. It never writes under
`androidApp/src/googlePlay/play/` or publishes metadata:

```bash
audit_dir="$(mktemp -d /private/tmp/feedflow-play-listing.XXXXXX)"
python3 .scripts/pull-google-play-listing.py --output-dir "$audit_dir"
```

The script reads `FEEDFLOW_PLAY_CONFIG_JSON` by default. If it is absent or invalid,
report the live check as unavailable. Compare `listings.json` against the checked-in
listing output and `image-counts.json` against the expected screenshot assets.

Do not use Gradle Play Publisher bootstrap tasks during the recurring audit. They reset
the existing Play directory and can delete/partially replace tracked graphics and
release notes. Never run any `publish*` task during this audit.

- `Google Play live check: unavailable - FEEDFLOW_PLAY_CONFIG_JSON is unavailable or invalid`

Latvian source store copy is currently incomplete, so report it without creating store or screenshot follow-up. Reassess supported App Store locales only after the source is complete.

Still compare local source and checked-in generated Play output:

- If `assets/storecopy/<locale>/` is complete but `androidApp/src/googlePlay/play/listings/<locale>/` is missing/stale, update or create generated local text output when possible, or create a board card naming the exact local output work.
- If checked-in Play screenshots/assets changed, create screenshot upload/regeneration cards only from committed/local evidence. Use the audit script's image counts to identify live screenshot drift.

### App Store

Use the `asc-cli-usage` skill before designing or changing `asc` commands. Discover flags with `--help` if needed.

Resolve the app and version:

```bash
asc apps list --bundle-id "com.prof18.feedflow" --output json --pretty
asc versions list --app "APP_ID" --output json --pretty
```

Then pull live metadata into a temp directory:

```bash
asc metadata pull --app "APP_ID" --version "VERSION" --platform IOS --dir "/tmp/feedflow-asc-metadata" --force --output json --pretty
```

For screenshots, list localizations and screenshot sets first, then download into a temp directory:

```bash
asc localizations list --version "VERSION_ID" --output json --pretty
asc screenshots list --version-localization "LOCALIZATION_ID"
asc screenshots download --version-localization "LOCALIZATION_ID" --output-dir "/tmp/feedflow-asc-screenshots/LOCALE" --overwrite
```

Compare live App Store metadata with `assets/storecopy/**` where the field mapping is obvious. Compare live screenshot presence/counts with generated screenshot expectations when available; do not treat different App Store processing filenames as meaningful by themselves.
The current asc screenshots-list JSON response uses a sets array, with each set's
screenshots nested inside it; it does not expose screenshots in a top-level data array.
Count those nested arrays before declaring screenshots missing, and download only sets
whose screenshot count is nonzero.

For App Store metadata, validate field limits before syncing or creating board cards. Current limits used by `asc metadata validate`: name 30, subtitle 30, keywords 100, description 4000, promotional text 170. If source fields exceed these limits, the actionable card should be about shortening/fixing the source copy, not syncing ASC.

If `asc` is missing, auth is unavailable, or the app/version cannot be resolved, report `App Store live check: unavailable` with the reason.

### Microsoft Store

Use `pcenter` (workflows live in the `update-microsoft-store-listing` skill). Everything below is **read-only**
and mutates nothing; never run `listing push`, `publish`, `submission` or `rollout`
subcommands during an audit.

Check availability first, and report `Microsoft Store live check: unavailable` with the
reason if it fails:

```bash
pcenter auth doctor --output json
```

Then read the live listing. Prefer `listing show` over `listing pull`: it writes nothing, so
the audit needs no temp directory and leaves nothing to clean up.

```bash
pcenter locales list --output json
pcenter listing show --output json          # every locale
pcenter listing show --locale it --output json   # one locale
pcenter listing show --locale it --images --output json
```

Output is JSON whenever stdout is a pipe. `listing show` returns
`{source, submissionId, localeCount, listings}`, keyed by lowercase locale, each carrying
`title`, `description`, `features`, `keywords`, `copyrightAndTrademarkInfo`, `licenseTerms`,
`recommendedHardware`, `minimumHardware` and `imageCount`. Image **binaries cannot be
downloaded** through the Submission API, so screenshot checks are limited to counts,
captions and Store ids — never report Microsoft Store screenshots as "missing" on the basis
that no files came back.

Field mapping to FeedFlow source copy:

| Live listing field | Source |
| --- | --- |
| `description` | `assets/storecopy/<locale>/microsoft_store_description.md` |
| `title` | `title` in `assets/storecopy/base/store_listing.json` — **en-us only**, see below |
| release notes | `assets/storecopy/microsoft-store-release-notes.json` (owned by the `update-store-release-notes` skill; not part of the listing) |

Three things will produce false findings unless you handle them:

1. **Normalize whitespace before comparing descriptions.** The repo hard-wraps
   `microsoft_store_description.md` at about 80 columns; the Store stores the text
   unwrapped. A raw `diff` therefore reports every locale as different. Collapse runs of
   whitespace on both sides (`re.sub(r"\s+", " ", text).strip()`) and compare that.
2. **A source file existing does not mean the locale is translated.** Some
   `assets/storecopy/<locale>/microsoft_store_description.md` files are still the English
   base text, and `diff` against base will call them "different" purely because of the
   wrapping above. Compare each locale's normalized text against normalized
   `assets/storecopy/base/microsoft_store_description.md`: if they are equal, the locale is
   an **untranslated placeholder**. Never create a sync card for one — pushing it would
   replace a correctly translated live listing with English.
3. **Titles are per-locale and optional.** Some locales carry a `title` and some return
   `""` — as of 2026-08-11, 9 of 25 did. An empty title is normal and must not be reported
   as drift, and neither must a locale whose title differs from the repo: the Microsoft Store
   title is a *reserved product name* chosen in Partner Center, not free text, so a
   difference is something to confirm with the user rather than a field to sync. (A locale
   being newly **added** is the exception — it must carry a title or the submission fails
   with `MissingTitle` — but that is the generator's job, not the audit's.)

Locale matching is case-insensitive. Expect the two sets **not** to line up, and say so
plainly rather than treating every difference as drift:

- A Store locale with no `assets/storecopy/<locale>/` directory is not translated in the
  repo. Report it; do not create a card unless the user is adding that language.
- A `assets/storecopy/<locale>/` with no Store locale is copy the Microsoft Store does not
  serve. Adding a Store language is a Partner Center decision, so report only.
- Regional variants (`de` vs `de-de`, `es` vs `es-es`, `gl` vs `gl-es`, `zh-cn` vs
  `zh-hans`) are separate Store locales. Do not silently fold them together.

Validate source field limits before creating any sync card. Microsoft Store limits:
description 10,000 characters; product features up to 20 items of 200 characters each;
minimum/recommended hardware up to 11 items of 200 characters each; "what's new" 1,500
characters. If complete source copy exceeds a limit, the actionable card is about shortening
the source, not syncing the Store.

To act on a finding, generate the listing files from the canonical copy rather than editing
them by hand:

```bash
pcenter listing pull --dir .pcenter
.scripts/generate-microsoft-store-listing.py --dir .pcenter --dry-run
```

The generator skips untranslated placeholders and locales the Store does not serve, so its
own output is a second opinion on this audit. It never writes `title`. Pushing stays a
human step — report the commands, do not run `listing push` during an audit.

## Output

Keep the report short and decision-oriented:

```text
Weekly translation/store audit

Action needed:
- ...

No action:
- ...

Details:
- App translations: ...
- Store copy: ...
- Screenshot copy/assets: ...

Live store state:
- <locale> Google Play — source: current|incomplete; checked-in output: current|stale;
  live store: current|stale|unavailable; action: <exact action or none>.
- <locale> App Store — source: current|incomplete; live metadata: current|stale|unavailable;
  screenshots: present|missing|unavailable; action: <exact action or none>.
- <locale> Microsoft Store — source: current|incomplete|absent; live listing:
  current|stale|unavailable; images: <count> (binaries not downloadable);
  action: <exact action or none>.

Recommended next commands:
- ...
```

For every locale with an actionable store finding, keep source, checked-in generated
output, and live-store state on the same line. Never call a store “stale” when only
the checked-in output is stale: explicitly say that no store upload is needed when
source equals live store but generated output differs from source. Likewise, distinguish
complete screenshot *copy* from uploaded screenshot *assets*; the former makes a
locale ready to generate, while the latter is the live-store upload state.

If nothing changed, say that no translation, store-copy, or screenshot action is needed.

## Obsidian Board Follow-Up

When store action is needed, write a compact board item so the work is not lost after the audit thread ends. Prefer one grouped task per operational outcome. Do not create board items for missing app-string translations; those belong only in the audit report.

- App Store metadata stale: `- [ ] Sync App Store <locale> <fields>`
- App Store source copy over limits: `- [ ] Shorten App Store <locale> <fields> to <limits>`
- Google Play live metadata stale: `- [ ] Sync Google Play <locale> <fields>`
- Microsoft Store live listing stale: `- [ ] Sync Microsoft Store <locale> <fields>`
- Microsoft Store source copy over limits: `- [ ] Shorten Microsoft Store <locale> <fields> to <limits>`
- Google Play generated output stale but live matches source: update the generated files during the audit; if that is blocked, use `- [ ] Update checked-in Google Play <locale> <fields> from live/source`
- Store copy complete in repo but locale not created/shipped: `- [ ] Add <locale> store metadata to <supported stores>` or `- [ ] Generate <store> <locale> listing output`
- Screenshots stale or incomplete: `- [ ] Regenerate/upload <store> <locale> <device/form-factor> screenshots`
- Screenshot copy complete in repo but screenshots not regenerated/uploaded: `- [ ] Create localized screenshots for <locale>`
- Store or screenshot locale scaffold incomplete: report only; do not create a board item until the relevant source copy is complete.

Before editing the board, read the existing `## marketing` lane and avoid duplicates by matching the main noun phrase and locale/platform names. Insert new items near related store-copy or conversion-analysis tasks. Do not create separate Obsidian notes unless the user asks.

## Guardrails

- Do not translate strings.
- Do not generate screenshots unless the user asks.
- Do not upload store metadata or screenshots unless the user asks.
- Microsoft Store reads only: `auth doctor`, `locales list`, `listing show`, `submission status`, `rollout status`, `app info`. Never `listing push`, `publish`, `submission commit/delete-draft`, or any `rollout` mutation during an audit.
- If translation resources changed, remind the user that `.scripts/refresh-translations.sh` should be run before Gradle checks.
