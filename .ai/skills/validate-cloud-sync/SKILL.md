---
name: validate-cloud-sync
description: Validate FeedFlow cloud-backup sync across supported platforms or onboard a new cloud provider using the shared regression harness and the real-device runbook.
---

# Validate cloud sync

Use the repository's two maintained references:

- [CLOUD_SYNC_TESTING.md](../../../docs/CLOUD_SYNC_TESTING.md) for the deterministic harness, supported source sets, provider boundaries and `allTests`.
- [CLOUD_SYNC_LIVE_TESTING.md](../../../docs/CLOUD_SYNC_LIVE_TESTING.md) for live provider setup, exact device1/device2 scenarios, the optional iPhone controller and evidence/report requirements.

Resolve these paths from this skill's directory; run commands from the repository root.
Choose the requested provider, supported platforms and test layer. A request for
unit/regression coverage does not require live accounts or devices. For a new
cloud provider, follow the onboarding section of the runbook and reuse the
portable scenarios before adding provider-specific cases. Server accounts such
as GReader and Feedbin use their own suites.

For live checks, reuse the user's existing account/fixture authorization. If that
scope is missing, establish it before mutations. Keep one actor per device and
sequence writes according to the scenario; independent read-only checks can run
in parallel. With delegation available, give bounded device work to cheaper
agents and retain coordination and final verification in the parent.

Keep the peer stale deliberately and capture the upload checkpoint before its
local refresh. Distinguish upload completion from receiver delivery. Do not
reissue an uncertain mutation; collect the controller result or inspect state.
Record `PASS`, `FAIL`, `BLOCKED` or `NOT RUN` with evidence, then stop temporary
controllers and link the local report. A controller's passing XCTest result is
not a passing sync scenario. Do not publish artifacts as part of validation.
