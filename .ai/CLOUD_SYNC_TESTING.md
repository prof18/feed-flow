# Cloud sync regression harness

This harness covers successful Dropbox, Google Drive and iCloud workflows and
regressions added with the cloud sync fixes. Server accounts (GReader, Feedbin and
others) are outside this harness.

## Structure

`shared/src/commonTest/.../test/cloudsync` holds the scenarios, fixture device API
and deterministic cloud byte store. Each device has its own Koin application,
settings, main database and sync database. Actions go through the real repositories
and platform worker. RSS content is seeded locally between source and item sync.
Provider doubles read and write the actual SQLite snapshot bytes; they do not
replace the database or worker with a map of article flags.

The fake store separates providers and accounts, copies bytes at its boundaries,
and gives Drive files stable identities. iCloud saves remain staged until explicit
propagation. This models successful file transfer only: it does not claim to
implement the providers' complete consistency, authorization or conflict behavior.

| Source set / target | What it runs |
| --- | --- |
| `commonTest` | Shared store self-tests and portable success scenarios |
| `jvmTest` | Desktop worker, JDBC SQLite files and persisted properties |
| `androidTest` | Generic shared Android fixtures and byte transports |
| `androidHostTest` | **Robolectric, no emulator**; Android worker, JDBC SQLite files, preferences and WorkManager queue |
| `androidDeviceTest` | Optional instrumentation lane using the Android SQLite driver |
| `iosTest` | iOS worker, native SQLite files and isolated persisted defaults on the simulator |
| `feedSync/icloud/iosTest` | Real iOS iCloud file adapter with local folder URLs |
| `feedSync/ikloud-macos/ikloudTest` | Real macOS Foundation upload/download helper with local URLs |
| `iosApp/Tests/CloudSync` | Swift provider adapter callbacks and bytes with SDK boundary doubles |

The macOS native helper tests run separately from the JVM bridge double. Neither
requires a real iCloud account. The shipping JNI library and Apple background
delivery require separate integration validation; a passing double is not proof of
those mechanisms.

## Cloud sync notifications

`FeedSyncMessageQueue.messageQueue` retains every engine result for diagnostics and
the harness. UI consumers use `userMessages`: transfer, discovery, snapshot and
reconciliation failures stay quiet, including when refresh or Backup triggers the
attempt. Pending changes remain available for the next sync opportunity, and the
last successful sync timestamps do not advance on failure.

Google Drive reauthorization prompts are emitted once per active UI collector until
a successful sync result resets suppression. Explicit iCloud connection attempts
still report an unavailable service; background iCloud URL failures remain silent.
Provider authentication feedback and the recovery-choice UI are separate and remain
available. `FeedSyncMessageQueueTest` tests this policy, and
`CloudTransferRegressions` asserts that real worker failures and retries emit no UI
notifications across the supported platforms/providers.

## Successful baseline

Every supported provider runs the same independent scenarios:

- Initial upload and second-device download of subscriptions, categories and flags.
- Sequential edits in both directions.
- Supported unread/unbookmark transitions while the other flag remains true.
- Repeated sync with stable state and one remote backup.
- Normal close/reopen using the existing database files and saved credentials.
- Bulk mark-as-read with bookmarks preserved.
- Adding a subscription and category after an initial sync.

`CloudFlagRegressions` checks all 16 transitions between the four read/bookmark
states, including explicit false/false values, sender refresh after upload, peer
refresh and clearing a bulk-read result. Its stale-device refresh test verifies
that refresh never uploads an old snapshot over a peer's newer bookmark and
subscription. It does not yet prove preservation of the stale device's pending
edit during download or competing backup writes; those are separate C2/C3 fixes.

`CloudTransferRegressions` checks confirmed-missing bootstrap, failed upload
retaining pending work and remote bytes, failed first download performing no
upload, stopped reconciliation, and a successful retry. iCloud local file absence
is an unavailable download, not proof of remote absence: automatic first-upload
requires future metadata discovery; the existing explicit backup action remains
available. Apple's [iCloud document guide](https://developer.apple.com/documentation/uikit/synchronizing-documents-in-the-icloud-environment)
explains why local filesystem lookup cannot discover unmaterialized documents.

SDK transport tests cover Drive discovery errors, duplicate identities, stale IDs
and pagination, plus Dropbox missing-path classification. Swift callbacks carry
Drive's resolved ID through the local write before caching it. Native iCloud tests
cover replacement and preserving the last backup when an upload copy fails.

`CloudSnapshotRegressions` pauses the upload before reading its file, changes a
local article through the real repository, and verifies that the peer receives
the captured snapshot while the sender keeps the newer edit. It runs for desktop
and Android Dropbox/Drive and iOS Dropbox/iCloud. The iOS Drive adapter already
passes materialized NSData to its SDK boundary; the synchronous desktop iCloud
JNI transfer instead holds the database lifetime lock through its local copy.
These tests prove snapshot isolation, not exact pending-edit acknowledgment.

The sync helper serializes complete SQL operations with closing and file work.
JVM lifetime tests use a real driver with deterministic barriers to prove that
close waits for active queries, file work prevents reopening, and close failures
stop work until retry succeeds. Platform success suites also exercise WAL-backed
files and close/reopen. Cross-process ownership, staged snapshot validation and
non-destructive factory recovery remain separate C7 work.

Ambiguous writes, crashes, cancellation, corruption, empty remote collections and
pending-change acknowledgment races are added with their corresponding fixes.
These tests do not prove the absence of all unseen corner cases.

## Local and CI execution

Run the ordinary Gradle gate from the repository root:

```sh
./gradlew --quiet --console=plain allTests
```

`allTests` keeps the normal source-set tests and includes the cloud-sync suites.
The suites exchange real SQLite snapshot bytes between producer and consumer
devices for desktop and Android hosts, and include the iOS shared, macOS native
helper and Swift adapter tests on macOS arm64. Android cloud-sync tests use
Robolectric and do not start an emulator or require a device. iCloud is skipped
on non-Mac JVM hosts; Drive is unavailable in the shipping Flatpak build.

The optional Android instrumentation source set remains outside the normal gate.
Run it only when a device or emulator is deliberately available:

```sh
./gradlew --quiet --console=plain :shared:connectedAndroidDeviceTest
```

On macOS arm64, the Swift task requires Xcode with an iPhone 17 Pro simulator
and XcodeGen (`brew install xcodegen`). It generates the Xcode project and builds
its Kotlin framework through Gradle dependencies; Xcode does not invoke Gradle
recursively. CI prepares dummy iOS configuration files. For a fresh local checkout,
follow the repository's initial setup instructions.

The Gradle test reports remain in the normal `build/test-results` and
`build/reports/tests` directories. Snapshot files exchanged by producer and
consumer tests are written under `shared/build/cloud-sync-artifacts` and can be
uploaded as CI artifacts when a job fails. CI uses normal Gradle output without
`--quiet` or `--console=plain`. The Swift test result bundle is written under
`shared/build/reports/tests/swiftCloudSyncTest` on macOS.

The harness uses deterministic provider doubles and does not exercise live
provider accounts or SDK credentials; Gradle/Xcode may still download build
dependencies. A passing double is not proof of shipping JNI loading or Apple's
background iCloud delivery.

Stage B preserves PR #1358's contributor attribution through cherry-pick
`a38006731`, followed by the separate corrective commit `9fbe29e50`. The unsafe
upload-before-download ordering is removed; regression coverage builds on the
Stage A success suite.

## Verification and remaining evidence

The macOS arm64 gate exercises the Android/Robolectric, iOS, desktop, native
helper, Swift adapter and directed provider/platform snapshot exchange suites.
Windows and Linux `allTests` jobs are configured in the main code-check workflow,
but their execution is not yet proven locally. Live iCloud/JNI behavior, Apple's
background propagation, and optional Android instrumentation remain separate
validation work.

When adding a scenario, add its enum case and assertions to
`CloudSuccessScenarios.kt`. Platform bindings should supply storage and transport
details only. Run `allTests` on the affected hosts when a portable scenario
changes, and inspect the normal Gradle reports together with
`shared/build/cloud-sync-artifacts`.

Regenerate the Android baseline profiles before a performance-sensitive release:
some provider constructors gained optional test dependencies, so their recorded
constructor signatures have changed. Do not hand-edit the generated profiles.
