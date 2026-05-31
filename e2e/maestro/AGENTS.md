# AGENTS.md

## Maestro E2E

Follow `e2e/maestro/maestro-e2e-guide.md` when writing, changing, or running Maestro flows.

Keep `e2e/maestro/maestro-e2e-tests.md` updated when adding, removing, renaming, or materially reshaping a flow. If the physical YAML inventory changes, update `e2e/maestro/maestro-e2e-tests.html` too.

Use platform-specific flows under:

- `android/smoke/` or `android/regression/`
- `ios/smoke/` or `ios/regression/`

Smoke and regression flows must be deterministic: use debug seed deep links and fixtures, never live feeds, OAuth, real provider auth, or previous app state.

When changing a user-visible feature, add or update Maestro coverage if the behavior can be exercised with existing app UI, debug seed deep links, fixtures, or test tooling without adding production-only code paths. If coverage is not feasible, record the reason in the Known Limitations section of `e2e/maestro/maestro-e2e-tests.md`.

Run the relevant wrapper or single flow before calling an E2E change done:

- Android all automated: `e2e/scripts/run-android.sh`
- iOS all automated: `e2e/scripts/run-ios.sh`
- Android smoke only: `e2e/scripts/run-android-smoke.sh`
- iOS smoke only: `e2e/scripts/run-ios-smoke.sh`
- Single Android flow: `maestro --platform android test e2e/maestro/android/<suite>/<flow>.yaml`
- Single iOS flow: `maestro --platform ios --device "$SIMULATOR_UDID" test e2e/maestro/ios/<suite>/<flow>.yaml`
