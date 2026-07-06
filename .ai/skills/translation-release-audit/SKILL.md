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

The repo has a Gradle Play Publisher bootstrap task:

```bash
./gradlew --quiet --console=plain :androidApp:bootstrapGooglePlayReleaseListing
```

Do not run this task during the recurring audit for now. It repeatedly hangs during graphics downloads and can leave a temp checkout with deleted/partial graphics, so it is not a reliable live-check source. Until there is a fixed/reliable Play read path, report:

- `Google Play live check: unavailable - Gradle Play Publisher bootstrap currently hangs during graphics download`

Current store-locale exception: Latvian (`assets/storecopy/lv`) has complete source store copy, but App Store Connect currently has no Latvian locale option for this app/version. Exclude Latvian from App Store follow-up until ASC exposes/supports that locale; only consider local/source output and stores that support Latvian, such as Google Play.

Still compare local source and checked-in generated Play output:

- If `assets/storecopy/<locale>/` is complete but `androidApp/src/googlePlay/play/listings/<locale>/` is missing/stale, update or create generated local text output when possible, or create a board card naming the exact local output work.
- If checked-in Play screenshots/assets changed, create screenshot upload/regeneration cards only from committed/local evidence. Do not infer live Play screenshot drift without a reliable live source.
- Do not run any `publish*` task during this audit.

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

For App Store metadata, validate field limits before syncing or creating board cards. Current limits used by `asc metadata validate`: name 30, subtitle 30, keywords 100, description 4000, promotional text 170. If source fields exceed these limits, the actionable card should be about shortening/fixing the source copy, not syncing ASC.

If `asc` is missing, auth is unavailable, or the app/version cannot be resolved, report `App Store live check: unavailable` with the reason.

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
- Live store state: ...
- Screenshot copy/assets: ...

Recommended next commands:
- ...
```

If nothing changed, say that no translation, store-copy, or screenshot action is needed.

## Obsidian Board Follow-Up

When store action is needed, write a compact board item so the work is not lost after the audit thread ends. Prefer one grouped task per operational outcome. Do not create board items for missing app-string translations; those belong only in the audit report.

- App Store metadata stale: `- [ ] Sync App Store <locale> <fields>`
- App Store source copy over limits: `- [ ] Shorten App Store <locale> <fields> to <limits>`
- Google Play live metadata stale: `- [ ] Sync Google Play <locale> <fields>`
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
- If translation resources changed, remind the user that `.scripts/refresh-translations.sh` should be run before Gradle checks.
