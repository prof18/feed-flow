---
name: verify-flatpak
description: Regenerate and verify FeedFlow's Flatpak offline build in a local UTM arm64 Linux VM. Generates the Maven sources files from feed-flow inside the Flatpak SDK, syncs the Flathub packaging repo into the VM, runs flatpak-builder, collects missing Maven coordinates, checks x64/arm64 native parity, download-checks x86_64-only sources and smoke-launches the app. Use when the user asks to verify, test or check the Flatpak build, update or regenerate the Flatpak sources, or prepare a Flathub release.
---

# Verify the Flatpak build

Background, the sources files, the format for missing entries, pruning and the release steps are
in [references/sources-and-release.md](references/sources-and-release.md). Read it before
regenerating sources, adding entries or preparing a release. Building the VM is in
[references/vm-setup.md](references/vm-setup.md).

## Setup

| Thing | Default | Override |
|---|---|---|
| UTM VM | `feedflow-flatpak` (Ubuntu arm64, Apple Virtualization backend, GNOME, flatpak-builder) | `FEEDFLOW_FLATPAK_VM` |
| SSH | `$USER@feedflow-flatpak.local`, key auth | `FEEDFLOW_FLATPAK_VM_HOST` |
| Packaging repo (source of truth) | `../com.prof18.feedflow` next to the feed-flow checkout ([flathub/com.prof18.feedflow](https://github.com/flathub/com.prof18.feedflow)) | `FEEDFLOW_FLATPAK_REPO` or `--repo` |
| Bench in the VM | `~/flatpak-bench`, overwritten by every `sync`; never edit it | |
| Generation checkout in the VM | `~/flatpak-gen`, recreated by every `generate` | |
| Run logs / screenshots | `.tmp/flatpak-verify/<timestamp>-<step>/` in feed-flow (gitignored) | |

Sources files are edited in the packaging repo on the Mac and copied into the VM; only the
feed-flow git pin differs in the VM copy (`sync --ref`).

The VM is arm64, so the build runs the **aarch64** path for real. x86_64 is checked statically
(`parity` + `x86`); Flathub CI builds both. Bugs that only show on x86_64 or with a real GPU need
x86_64 Linux hardware.

## Workflow

All commands from the feed-flow repo root; `S=.ai/skills/verify-flatpak/scripts/flatpak_vm.py`.

1. **Packaging repo.** Check out or create the release branch there. Ask before switching
   branches if the checkout has uncommitted work.
2. **Start the VM:** `python3 $S up`
3. **Regenerate sources** (whenever feed-flow's dependencies, Gradle or plugins changed):
   `python3 $S generate --ref <feed-flow ref>`. It writes `flatpak-sources.json`,
   `flatpak-sources-root.json` and `flatpak-sources-convention.json` into the packaging repo and
   warns when feed-flow's Gradle version no longer matches the manifest. Review the diff there.
   `flatpak-sources-manual.json` is never touched. Takes about an hour: run it in the background.
4. **Pre-flight:** `python3 $S preflight` — valid JSON, no `/` in `dest-filename`, a single manual
   file, prints the manifest pin. Cross-file repeats are normal generator overlap.
5. **Sync:** `python3 $S sync --ref <ref>`
   - Testing an unreleased app: `--ref main` (or a branch / 40-char commit).
   - Release check: omit `--ref` to keep the manifest's tag.
6. **Build** (run in the background; the first run downloads the 25.08 SDK):
   `python3 $S build --disable-cache`. Without `--disable-cache`, an unchanged feedflow module is a
   cache hit, Gradle never runs and the sources are not exercised; the summary warns about this.
7. **Missing dependencies.** If the build fails with `Could not resolve/find`, the build summary (or
   `python3 $S missing`) lists every coordinate in one pass. Add entries to the packaging repo's
   `flatpak-sources-manual.json` following "Adding a missing entry" in the reference, then `sync`
   and `build` again. Any other failure (compile error, script error, OOM) is not a sources
   problem: stop and report it.
8. **Native parity:** `python3 $S parity` — compose `desktop-jvm-linux` and skiko
   `skiko-awt-runtime-linux` must have the same versions and file types for x64 and arm64, and the
   skiko version each `desktop-jvm-linux-<arch>` pom depends on must have its `.jar`.
9. **x86_64 sources:** `python3 $S x86` — downloads every x86_64-only source (skiko/compose x64,
   x64 JBR) and checks HTTP status and hash. This replaces building on x86_64.
10. **Smoke test:** `python3 $S smoke` — launches the installed app under Xvfb in the VM, screenshots
    it after 25 s, and pulls the log and PNG back. Look at the screenshot. JDK "restricted method"
    / skiko `LibraryLoader`, `GlobalSnapshotManager` and Skiko's `Cannot create Linux GL context`
    (no GPU in the VM, it falls back to software rendering) are expected noise.
11. **Click-around (optional, user):** the UTM window shows the GNOME desktop; `python3 $S gui`
    opens the installed app there. Plain `DISPLAY=:0` over SSH fails with "Authorization required"
    because Xwayland needs the session's cookie, which is why `gui` goes through
    `systemd-run --user`. If GNOME asks to create a login keyring, leave its password empty.
    Rendering is software-only, so judge behaviour, not rendering performance.

## Report

Summarise: the feed-flow ref generated from and built, whether Gradle actually ran, missing
coordinates added, parity result, x86 source check result, smoke result with the screenshot path,
and the log directory. Then remind the user of the release steps that stay manual: set the
manifest pin to the release tag in the packaging repo, commit, and push only when the user asks.

## VM maintenance

- Free disk inside the VM: `ssh "${FEEDFLOW_FLATPAK_VM_HOST:-$USER@feedflow-flatpak.local}" 'rm -rf flatpak-bench/.flatpak-builder flatpak-bench/builddir flatpak-bench/repo flatpak-gen'`.
- Update the VM: `ssh "${FEEDFLOW_FLATPAK_VM_HOST:-$USER@feedflow-flatpak.local}" 'sudo apt-get update && sudo apt-get -y upgrade && flatpak update --user -y'`.
- Rebuild it from scratch: [references/vm-setup.md](references/vm-setup.md).
