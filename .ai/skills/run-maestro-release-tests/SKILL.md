---
name: run-maestro-release-tests
description: Run the full FeedFlow Maestro release validation suite and produce HTML and Markdown pass/fail reports. Use when the user asks to run all Maestro tests, validate E2E coverage before release, check Android and iOS Maestro flows, or summarize failed FeedFlow E2E flows.
---

# Run Maestro Release Tests

Run FeedFlow's full local Maestro release gate across Android and iOS, then report what passed, what failed, and where the logs are.

## Workflow

1. Confirm the repo root is the FeedFlow project containing `e2e/maestro/` and `e2e/scripts/`.
2. Run the bundled script from the repo root:

   ```bash
   python3 .ai/skills/run-maestro-release-tests/scripts/run_maestro_release_tests.py
   ```

3. If the user asks for a narrower pass, pass explicit flags:

   ```bash
   # Android only
   python3 .ai/skills/run-maestro-release-tests/scripts/run_maestro_release_tests.py --platform android

   # iOS only
   python3 .ai/skills/run-maestro-release-tests/scripts/run_maestro_release_tests.py --platform ios

   # Smoke only while debugging
   python3 .ai/skills/run-maestro-release-tests/scripts/run_maestro_release_tests.py --suite smoke
   ```

4. Open or read the generated `report.html` first. Use `report.md` as a terminal-friendly fallback. Summarize:
   - total pass/fail counts by platform and suite
   - failed flow filenames
   - each failed flow's report classification, brief analysis, and evidence line
   - setup failures, if any
   - HTML report path and log directory

## Preconditions

- Maestro, `python3` and `adb` must be installed.
- Android needs a running device or emulator. Pick it with `--android-serial <serial>` (or `ANDROID_SERIAL`); without either, the script uses the only online device and fails setup when several are online. Every install, fixture push, flow, helper and restore is bound to that serial.
- iOS needs a booted simulator. Pick it with `--ios-udid <udid>` (or `SIMULATOR_UDID`); otherwise the script uses the booted `iPhone 18 Pro`. The build targets that UDID.
- iOS needs `iosApp/FeedFlow.xcodeproj`; the script generates it when missing.
- When `atc` is installed, the script reserves both devices before building (`atc claim --serial` / `atc claim --ios --udid`, waiting up to 10 minutes for another holder), renews the leases during the run and frees the ones it created at the end. Leases the calling session already holds are reused and left in place. A device that cannot be reserved fails that platform's setup. Pass `--no-atc` to skip reservation. Claim a physical phone by serial: `atc claim --type phone --api 36` boots an emulator instead.
- Extra Maestro variables (for example `WIDGET_HEADER_TITLE` on a localized device) go through `--maestro-env KEY=VALUE`, repeatable.

## Behavior

When both platforms are selected, the script runs Android and iOS in parallel. Each platform still builds and installs once, pushes fixtures, then runs every YAML flow for that platform in sorted order from:

- `e2e/maestro/android/smoke`
- `e2e/maestro/android/regression`
- `e2e/maestro/ios/smoke`
- `e2e/maestro/ios/regression`

Flows run the same way `e2e/scripts/run-android.sh` and `run-ios.sh` run them: the audio flows (174-176) go through `e2e/scripts/run-audio-playback.sh`, Android SM-007 through `android_reader_tooltip.py`, and Android REG-179 through `android_share_feed.py`, which sends the real share intent. Helper output lands under `helpers/android/` in the report directory.

It continues after failed flows so the final report captures the full failure set. After the flows it restores the development feeds with `feedflow-restore-dev-feeds`. It exits non-zero when any setup step, flow or restore fails. Run it with output going to a file (or with `set -o pipefail`) when you rely on the exit code.

Reports and logs are written under:

```text
.tmp/maestro-release-tests/<timestamp>/
```

The main visual report is `report.html`. It contains run metadata, setup/build steps, suite summaries, failed flows, brief failure classifications, evidence lines from logs, and every selected test with links to per-flow logs. The script also writes `report.md` for quick terminal review.

Use the per-flow logs in that directory together with Maestro artifacts under `~/.maestro/tests/` when debugging failures. Treat the report classification as a first-pass diagnosis, then verify ambiguous cases against screenshots and UI hierarchies before calling a failure a product regression.
