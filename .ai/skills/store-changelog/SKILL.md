---
name: store-changelog
description: Generate, translate, and write FeedFlow release notes for every store (Google Play, App Store iOS, App Store macOS, Microsoft Store, Flatpak) from git history since a tag. Use when the user asks for changelogs, release notes, "what's new", or store descriptions for a release.
---

# Store Changelog Generator

Generate release notes for every FeedFlow store from the git history since a tag, get the user's approval, then translate them and write them into the repo files each store reads.

## Workflow

### 1. Pick the tag

- Use the tag the user gives (for example `1.18.0-all`).
- Otherwise run `git describe --tags --abbrev=0` and tell the user which tag you used.
- Tags are per platform (`-all`, `-android`, `-ios`). If the latest tag only covers one platform, check whether it holds anything the other platforms haven't shipped. For example, run `git log <previous -all tag>..<latest tag>`. Then say which range each platform's notes cover.

### 2. Read the changes

```bash
git log <tag>..HEAD --pretty=format:"%h %s%n%b" --no-merges
```

Keep only changes users would notice. Follow `resources/release-notes-guidelines.md`: leave out dependency bumps, translations, tests, docs, CI and tooling. Decide which platforms each change reaches. Shared code reaches every platform. Swift or `iosApp/` reaches iOS and macOS. `androidApp/` is Android only. `desktopApp/` reaches Windows, Linux, and the macOS desktop build. Search `~/regesto-kb` for recorded FeedFlow release-note decisions, for example `release-note-format`, and follow them.

### 3. Propose, then STOP

Show the user the full English notes for every platform, in the exact final format of each section below. Include:
- the tag and commit range used
- the character count of the Android notes
- the changes you left out on purpose, and why

**Do not translate or edit any file yet.** Wait for the user to confirm or change the notes. Treat the text the user approves as final, and don't reword it in the next steps.

### 4. Translate and write the files (after confirmation only)

Translate the approved notes into every configured locale for each platform. Write each platform's notes to the files listed below. Keep the author's first-person singular voice in every language ("I'd love your feedback", never "we" or "us").

### 5. Validate and report

- Android: count each file with Python `len()` on the UTF-8 text. `wc -m` counts bytes in the C locale. If a locale is over 500 characters, tighten that translation, keeping every bullet.
- Microsoft Store and App Store: `jq empty <file>`, and check that every locale has the same number of bullets as English.
- Flatpak: `xmllint --noout desktopApp/packaging/flatpak/com.prof18.feedflow.metainfo.xml`.
- Run `git diff --check`.
- Report which files changed, the locale count for each platform, and any translation you shortened. Say that the translations are machine-made and need a native review.

Do not commit, upload, or publish anything unless the user asks. Pushing notes to a store is a separate step: see `update-store-release-notes` for App Store Connect, and `pcenter` and the release workflow for the Microsoft Store.

## Platforms: formats and files

All platforms use the same bullet format: one `• ` bullet per line, one sentence each, starting with the benefit. No section headers.

### Android (Google Play)

- Limit: **500 characters per locale**, including bullets and newlines. Pick the 4–5 changes with the most impact.
- Files: `androidApp/src/googlePlay/play/release-notes/<locale>/production.txt`. `en-US` is the source. Every other locale folder is a target. Never touch `alpha.txt`.
- After writing, also give the Play Console block: `en-US` first, then the other locales in alphabetical order. Save it to the scratchpad and send it to the user as well:
  ```text
  <en-US>
  • ...
  </en-US>
  <bg>
  • ...
  </bg>
  ```

### iOS and macOS (App Store)

- iOS and macOS are separate apps with separate copy. Highlight the changes each platform actually gets: scrolling and iPad on iOS, windows and keyboard on macOS.
- Same simple bullet list as the other platforms, with no headers. Example:
  ```
  • Reader Mode has a brand-new engine that gives cleaner, more reliable articles. If a page doesn't look right, I'd love to hear your feedback.
  • Smoother scrolling in the article list, especially on iPad.
  ```
- File: `assets/storecopy/app-store-release-notes.json`, shaped as `{ "ios": { "<locale>": [bullets] }, "macos": { "<locale>": [bullets] } }`. Each bullet string includes the leading `• `.
- The keys are App Store Connect locale codes: `en-US`, `de-DE`, `es-ES`, `fr-FR`, `he`, `hu`, `it`, `ja`, `pt-BR`, `ru`, `sk`, `uk`, `vi`, `zh-Hans`. If App Store Connect may have gained or lost a language, confirm the list with a read-only `asc` query.

### Windows (Microsoft Store)

- File: `assets/storecopy/microsoft-store-release-notes.json`. It keeps the top-level `notes` object. Each locale value is an array of bullet strings that include the leading `• `.
- Update every existing locale key. Alias keys (`de`/`de-de`, `es`/`es-es`, `gl`/`gl-es`, `zh-cn`/`zh-hans`) get identical text. A missing locale fails the publish.
- Include Windows-only fixes (for example Google Drive sign-in). Mention only the sync providers the Windows app has.

### Linux (Flatpak)

- File: `desktopApp/packaging/flatpak/com.prof18.feedflow.metainfo.xml`. Add a new `<release>` as the first child of `<releases>`, using the version from the `com.feedflow.versioning` convention plugin and the release date. English only.
  ```xml
    <release version="X.X.X" date="YYYY-MM-DD">
        <description>
            <p>Feature or fix description</p>
        </description>
    </release>
  ```
- Leave out Google Drive: Flatpak builds disable it.

## Resources

- `resources/release-notes-guidelines.md`: rules for language, filtering and QA.
