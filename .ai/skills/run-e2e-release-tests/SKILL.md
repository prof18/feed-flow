---
name: run-e2e-release-tests
description: Run the full FeedFlow E2E release validation suites (Android and iOS through Maestro, desktop through Spectre) and produce HTML and Markdown pass/fail reports. Use when the user asks to run all Maestro or E2E tests, validate E2E coverage before release, check the Android, iOS or desktop flows, or summarize failed FeedFlow E2E flows.
---

# Run E2E Release Tests

Run FeedFlow's full local E2E release gate across Android, iOS and desktop, then report what passed, what failed, and where the logs are.

## Workflow

1. Confirm the repo root is the FeedFlow project containing `e2e/maestro/` and `e2e/scripts/`.
2. Run the bundled script from the repo root:

   ```bash
   python3 .ai/skills/run-e2e-release-tests/scripts/run_e2e_release_tests.py
   ```

3. If the user asks for a narrower pass, pass explicit flags:

   ```bash
   # Android only
   python3 .ai/skills/run-e2e-release-tests/scripts/run_e2e_release_tests.py --platform android

   # iOS only
   python3 .ai/skills/run-e2e-release-tests/scripts/run_e2e_release_tests.py --platform ios

   # Desktop only (macOS)
   python3 .ai/skills/run-e2e-release-tests/scripts/run_e2e_release_tests.py --platform desktop

   # Smoke only while debugging
   python3 .ai/skills/run-e2e-release-tests/scripts/run_e2e_release_tests.py --suite smoke
   ```

4. Open or read the generated `report.html` first. Use `report.md` as a terminal-friendly fallback. Summarize:
   - total pass/fail counts by platform and suite
   - failed flow filenames
   - each failed flow's report classification, brief analysis, and evidence line
   - setup failures, if any
   - HTML report path and log directory

## Preconditions

- Maestro, `python3` and `adb` must be installed for Android and iOS; desktop needs only `python3` and the Gradle wrapper.
- Desktop runs only on macOS. It opens real FeedFlow windows, needs no device and no atc lease, and must not overlap another desktop run: `/tmp/ffe2e/runner.lock` refuses a second runner, and the test JVM's `/tmp/ffe2e/run.lock` refuses a second Gradle run. See `e2e/desktop/README.md`.
- Android needs a running device or emulator. Pick it with `--android-serial <serial>` (or `ANDROID_SERIAL`); without either, the script uses the only online device and fails setup when several are online. Every install, fixture push, flow, helper and restore is bound to that serial.
- iOS needs a booted simulator. Pick it with `--ios-udid <udid>` (or `SIMULATOR_UDID`); otherwise the script uses the booted `iPhone 18 Pro`. The build targets that UDID.
- iOS needs `iosApp/FeedFlow.xcodeproj`; the script generates it when missing.
- When `atc` is installed, the script reserves both devices before building (`atc claim --serial` / `atc claim --ios --udid`, waiting up to 10 minutes for another holder), renews the leases during the run and frees the ones it created at the end. Leases the calling session already holds are reused and left in place. A device that cannot be reserved fails that platform's setup. Pass `--no-atc` to skip reservation. Claim a physical phone by serial: `atc claim --type phone --api 36` boots an emulator instead.
- Extra Maestro variables (for example `WIDGET_HEADER_TITLE` on a localized device) go through `--maestro-env KEY=VALUE`, repeatable.

## Behavior

When Android and iOS are both selected, the script runs them in parallel; desktop runs after them, so its real app windows do not compete with the mobile builds for CPU and memory. The `desktoponly` suite runs on desktop only: with `--suite desktoponly` the mobile platforms are skipped with a note, and a run that leaves nothing to do (for example `--platform android --suite desktoponly`) exits with code 2. A requested Maestro suite folder with no YAML flows is a setup failure, not an empty pass. Each mobile platform still builds and installs once, pushes fixtures, then runs every YAML flow for that platform in sorted order from:

- `e2e/maestro/android/smoke`
- `e2e/maestro/android/regression`
- `e2e/maestro/ios/smoke`
- `e2e/maestro/ios/regression`

Desktop runs one Gradle invocation per suite (`:desktopApp:desktopE2eTest -Pfeedflow.e2eSuite=<smoke|regression|desktoponly>`). Its rows come from the JUnit XML in `desktopApp/build/test-results/desktopE2eTest`, named `desktop/<suite>/<TestClass>/<method>`; skipped tests show as SKIP. The JUnit files and the failure evidence (`tree.txt`, window captures, app logs) are copied to `desktop/<suite>/junit` and `desktop/<suite>/evidence` in the report directory. A Gradle run that fails without failing tests (a compile error or a crashed test worker) is a desktop setup failure.

Flows run the same way `e2e/scripts/run-android.sh` and `run-ios.sh` run them: the audio flows (174-176) go through `e2e/scripts/run-audio-playback.sh`, Android SM-007 through `android_reader_tooltip.py`, and Android REG-179 through `android_share_feed.py`, which sends the real share intent. Helper output lands under `helpers/android/` in the report directory.

It continues after failed flows so the final report captures the full failure set. After the Maestro flows it restores the development feeds with `feedflow-restore-dev-feeds`; desktop needs no restore because each desktop test uses an isolated home and preferences node. It exits non-zero when any setup step, flow or restore fails. Run it with output going to a file (or with `set -o pipefail`) when you rely on the exit code.

Reports and logs are written under:

```text
.tmp/e2e-release-tests/<timestamp>/
```

The main visual report is `report.html`. It contains run metadata, setup/build steps, suite summaries, failed flows, brief failure classifications, evidence lines from logs, and every selected test with links to per-flow logs. The script also writes `report.md` for quick terminal review.

Use the per-flow logs in that directory together with Maestro artifacts under `~/.maestro/tests/` when debugging failures. Treat the report classification as a first-pass diagnosis, then verify ambiguous cases against screenshots and UI hierarchies before calling a failure a product regression.
