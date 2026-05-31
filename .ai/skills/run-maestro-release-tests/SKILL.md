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
   - setup failures, if any
   - HTML report path and log directory

## Preconditions

- Maestro must be installed as `maestro`.
- Android needs a running device or emulator. The repo default is `Resizable_Experimental`.
- iOS needs a booted `iPhone 17 Pro` simulator. The script uses `SIMULATOR_UDID` when present, otherwise it detects the booted simulator.
- iOS needs `iosApp/FeedFlow.xcodeproj`; the script generates it when missing.

## Behavior

The script builds and installs each selected platform once, pushes fixtures, then runs every YAML flow in sorted order from:

- `e2e/maestro/android/smoke`
- `e2e/maestro/android/regression`
- `e2e/maestro/ios/smoke`
- `e2e/maestro/ios/regression`

It continues after failed flows so the final report captures the full failure set. It exits non-zero when any setup step or flow fails.

Reports and logs are written under:

```text
.tmp/maestro-release-tests/<timestamp>/
```

The main visual report is `report.html`. It contains run metadata, setup/build steps, suite summaries, failed flows, and every selected test with links to per-flow logs. The script also writes `report.md` for quick terminal review.

Use the per-flow logs in that directory together with Maestro artifacts under `~/.maestro/tests/` when debugging failures.
