# Flatpak sources and release reference

## Why the sources files exist

The manifest's `feedflow` module runs `.scripts/flatpak-build-setup.sh`,
`.scripts/disable-android-for-flatpak.sh`, then Gradle with `--offline`, because Flathub builds have
no network. Every Maven/Gradle artifact is pre-declared as a `type: file` source. flatpak-builder
downloads each one, checks its hash, and places it under
`./offline-repository/<group path>/<artifact>/<version>/`, which Gradle uses as a local repository.
A missing entry fails the build with `Could not resolve` / `Could not find`.

The manifest also pins the Gradle distribution (`gradle-<v>-bin.zip`, sha256) and the JBR runtime
tarballs (one per arch, `only-arches`). Bump those together with FeedFlow when its Gradle or JBR
version changes; `generate` warns about a Gradle mismatch.

## The four sources files

| File | Produced by | Covers |
|---|---|---|
| `flatpak-sources.json` | `generate`: the generator on every feed-flow subproject (`scripts/module-sources.init.gradle`), merged, plus jar completion and x64 mirroring | Each module's JVM compile/runtime, KMP metadata, KSP and compiler-plugin classpaths, and the Linux natives for both arches |
| `flatpak-sources-root.json` | `generate`: feed-flow's root `flatpakGradleGenerator` task | Plugins on the root build classpath |
| `flatpak-sources-convention.json` | `generate`: `build-logic/convention`'s `flatpakGradleGenerator` task (test configurations excluded) | FeedFlow's convention plugins |
| `flatpak-sources-manual.json` | **Hand-maintained** | Everything the generator cannot see: settings-level plugins (Foojay), task-created KSP compiler dependencies, imported BOM/parent POMs, and anything the build later reports missing |

Rules:
- Only `flatpak-sources-manual.json` is edited by hand, and there must be exactly one.
- Generation happens inside the Flatpak SDK on arm64 Linux after the same two setup scripts the
  manifest runs, so it sees the Flathub build's plugins and the Linux natives. Generating on macOS
  would list macOS Skiko natives instead.
- Generation covers every subproject because each module resolves its own classpath; versions can
  differ from `:desktopApp`'s. Configurations for Apple/native/JS/Wasm/Android/test targets are
  excluded: the Linux JVM build never resolves them, and many cannot resolve on Linux.
- `generate` post-processes the merged file in two ways:
  - **Jar completion.** flatpak-gradle-generator 1.8.0 processes a task's configurations
    concurrently and records each component once, so when a configuration that needs only
    POM/module metadata reaches a component first, the jar is silently dropped (it hit compose
    `desktop-jvm` and `navigation3-ui-desktop`). For every component listed without
    `<artifact>-<version>.jar`, `generate` fetches that jar from the same repository path and adds
    it when it exists. Splitting configurations into separate tasks does not avoid the race and
    makes generation far slower.
  - **x64 mirroring.** Generation runs on arm64, so only `*-linux-arm64` natives are resolved;
    each one is mirrored as `*-linux-x64` with the same file types for Flathub's x86_64 builder.
- `generate` takes about an hour (repository probing across every module); run it in the
  background.
- Entries repeated across files are normal overlap. The generated file also carries some entries
  the build never reads (macOS/Windows Skiko runtimes, KMP root jars); they cost download size
  only.
- A generator exit code of 0 does not prove coverage; only a clean offline build does
  (`build --disable-cache`).

## Adding a missing entry

For each `group:artifact:version` the build reports, add the `.jar`, `.pom`, `.module` (if
published) and any platform variants (`-jvm`, …), each as:

```json
{
  "type": "file",
  "url": "<full maven url>",
  "sha512": "<real hash>",
  "dest": "./offline-repository/<group/as/path>/<artifact>/<version>",
  "dest-filename": "<artifact>-<version>.<ext>"
}
```

- Hash with `curl -sL '<url>' | shasum -a 512` **only after** confirming HTTP 200
  (`curl -sI -o /dev/null -w '%{http_code}' '<url>'`): a 404 page hashes fine and gives a bogus entry.
- Repository: Maven Central (`https://repo.maven.apache.org/maven2`) by default;
  `https://dl.google.com/dl/android/maven2` for `androidx.*`, `com.android.*`,
  `com.google.android.*`; `https://plugins.gradle.org/m2` for Gradle plugins and plugin marker
  artifacts; `https://jitpack.io` for `com.github.*`. Other custom repositories are declared in
  FeedFlow's `settings.gradle.kts`.
- A POM with a `<parent>` or an imported BOM needs those POMs too.
- If the same coordinate keeps coming back after being added, the URL or `dest` path is wrong.
- If a missing coordinate is a normal module dependency, prefer fixing the generation (for example
  a configuration wrongly excluded in `module-sources.init.gradle`) over a manual entry.
- Linux natives (compose `desktop-jvm-linux-*`, skiko `skiko-awt-runtime-linux-*`) need both
  arches with the same file types: compose is `.pom` only, skiko is `.jar` + `.pom`. `generate`
  handles this; check with `parity` after hand edits.

## Pruning manual entries (optional)

Build with `python3 $S build --disable-cache --keep-build-dirs`, then in the VM look at
`~/flatpak-bench/.flatpak-builder/build/feedflow-<N>/offline-repository/`: a file was read iff
`atime > ctime`. Drop untouched `flatpak-sources-manual.json` entries **except** x64 natives for
current versions (the arm64 VM never reads them), then rebuild with `--disable-cache` to confirm.
Many manual entries are now also produced by `generate`; this is the way to find which ones the
manual file can drop.

## Releasing

1. In the packaging repo, create the release branch (`release-x.y.z`) from an up-to-date `master`.
2. `generate --ref <release tag>` if dependencies changed since the last release.
3. Make sure the manifest pins the release tag, not a branch:
   ```yaml
   - type: git
     url: https://github.com/prof18/feed-flow
     tag: 1.19.0-all
   ```
   (`sync --ref` only changes the VM copy, so the repo file is what ships.)
4. Run the full verification with no `--ref`, so the VM builds exactly what will ship.
5. Commit (`Release x.y.z` / `Update deps`); push and open the PR against
   `flathub/com.prof18.feedflow` only when the user asks. Flathub CI builds x86_64 and aarch64.
