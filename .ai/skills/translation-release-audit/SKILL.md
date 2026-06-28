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

- Google Play Console listing, using Gradle Play Publisher and a local service-account JSON
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
   - `Screenshot copy`: changes under `assets/screenshotcopy`.
   - `Screenshot assets`: changed PNG/WebP screenshot files or screenshot metadata.
3. For app translations, compare every changed locale file against `i18n/src/commonMain/resources/locale/values/strings.xml`.
   - Report missing keys and extra keys by locale.
   - Treat a locale as completed only when no keys are missing.
4. For store copy, flag the operational action:
   - Source copy changed under `assets/storecopy`: Play/App/Microsoft store metadata may need syncing or upload.
   - Generated Play listing text changed under `androidApp/src/googlePlay/play/listings`: Google Play metadata has changed and may need upload.
5. For screenshot copy/assets, flag the operational action:
   - `assets/screenshotcopy` changed: localized screenshot generation may be needed.
   - Store listing screenshot images changed: store screenshots may need upload.
   - Website screenshot assets changed: website screenshot gallery/hero may need deploy.
6. If live store checking is requested or this is the weekly automation, compare against current store state when credentials are available.
   - Google Play: only run bootstrap in a disposable worktree or otherwise clean throwaway checkout because it writes downloaded listing files into `androidApp/src/googlePlay/play/listings`.
   - App Store: pull metadata/screenshots into a temp directory and compare against FeedFlow's source store/screenshot copy where mapping is clear.
7. Add follow-up TODOs to the FeedFlow Obsidian board when the audit finds actionable store release work.
   - Board path: `/Users/mg/Workspace/Notes/projects/feed-flow/feed-flow-board.md`.
   - Use the `## marketing` lane for store listing, ASO, screenshot, and store localization release work.
   - Preserve the kanban Markdown format: add one `- [ ] ...` item per action under the lane.
   - Keep TODO text short and concrete, for example `- [ ] Sync App Store metadata for Italian, Russian, and French`.
   - Do not add TODOs for app-string translation gaps, non-actionable status, credential setup noise, or findings already represented by an existing unchecked board item.
   - If the board is unavailable or cannot be edited, include the TODO text in the final report under `Action needed` instead.

## Live Store Checks

### Google Play

The repo uses Gradle Play Publisher:

```bash
./gradlew --quiet --console=plain :androidApp:bootstrapGooglePlayReleaseListing
```

Use it only when credentials are available and the current checkout is disposable or clean. The preferred credential setup for automation is a machine-local secret file outside the repo, referenced by an environment variable such as `FEEDFLOW_PLAY_CONFIG_JSON=/Users/mg/.config/feedflow/play_config.json`. A repo-root `play_config.json` may exist in the primary checkout, but do not assume it exists in automation worktrees because it is intentionally uncommitted.

If the Gradle config only points at `../play_config.json`, either:

- update `androidApp/build.gradle.kts` to read a credential path env var with `../play_config.json` as a fallback, or
- configure the automation worktree setup to copy/symlink the machine-local secret into the worktree root as `play_config.json`.

After bootstrap, compare downloaded live files under `androidApp/src/googlePlay/play/listings/**` against the repo state:

- Text metadata changes mean Google Play's current listing differs from the checked-in listing output.
- Screenshot/image changes mean Google Play screenshot assets differ from the checked-in listing output.
- Do not run any `publish*` task during this audit.

If credentials are missing or bootstrap fails, report `Google Play live check: unavailable` with the reason.

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

- Store metadata stale: `- [ ] Sync stale App Store metadata for <locales>`
- Play metadata stale: `- [ ] Sync stale Google Play metadata for <locales>`
- Screenshots stale or incomplete: `- [ ] Regenerate/upload localized screenshots for <locales/platforms>`
- Store locale scaffold incomplete: `- [ ] Complete store and screenshot copy for <locale>`

Before editing the board, read the existing `## marketing` lane and avoid duplicates by matching the main noun phrase and locale/platform names. Insert new items near related store-copy or conversion-analysis tasks. Do not create separate Obsidian notes unless the user asks.

## Guardrails

- Do not translate strings.
- Do not generate screenshots unless the user asks.
- Do not upload store metadata or screenshots unless the user asks.
- If translation resources changed, remind the user that `.scripts/refresh-translations.sh` should be run before Gradle checks.
