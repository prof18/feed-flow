# Live cloud-sync testing

Read the [cloud sync architecture](CLOUD_SYNC_ARCHITECTURE.md) for the component
map, merge rules, provider behavior, and failure handling behind these scenarios.

This is the optional, real-account complement to the deterministic regression
harness in [CLOUD_SYNC_TESTING.md](CLOUD_SYNC_TESTING.md). It is a manual or
operator-assisted release check, never a CI requirement and never a replacement
for `./gradlew --quiet --console=plain allTests`.

Prefer a disposable development account and fixtures. Use only the account and
fixture subset the user has explicitly placed in scope; this can include a
personal account when explicitly authorized. Existing authorization continues to
apply during the same run. Do not hard-code a destructive "restore" step: record
the fixture's initial state and restore only when it is in the authorized scope.

## Scope and support matrix

| Provider | Android | Desktop | iOS | Notes |
| --- | --- | --- | --- | --- |
| Dropbox | supported | supported | supported | Test real account transfers. |
| Google Drive | supported | supported | supported | Flatpak excludes Drive. |
| iCloud | — | macOS only | supported | Apple devices/accounts and entitlements required. |

The deterministic suite covers the same merge rules through provider doubles.
Live testing proves the installed app, real provider authorization, transfer, and
peer delivery in one supported pairing. It does not prove every interleaving,
provider outage, or background scheduling condition.

## Roles, run directory, and evidence

Name the two installations **device1** and **device2** in every command, note,
and artifact. A desktop may be either role. Work on one UI actor at a time;
serial UI actions avoid creating accidental concurrent writes. Independent,
read-only metadata inspection may run in parallel.

Create a durable local run directory, for example:

```sh
RUN_DIR=".tmp/cloud-sync-live/2026-09-12-dropbox-device1-android-device2-ios"
mkdir -p "$RUN_DIR"/{evidence,logs}
CONTROLLER_DIR="$RUN_DIR/device2-controller" # must not exist before prepare
```

Keep secrets, tokens, database contents, and account identifiers out of the
report and terminal output. Use placeholders in commands and redact copies before
sharing them. Capture UI state and, where available, read-only database copies,
provider metadata, and timestamped app logs under `RUN_DIR/evidence/`. Do not
modify databases, cloud files, or a shipping app installation outside the app UI.

Before each mutation record: provider, app build, device/OS, account alias,
fixture IDs and expected state, pending-queue state when available, and the
current remote file identity/revision when the provider exposes one. Record a
separate checkpoint for each of these points:

1. local mutation;
2. **after upload, before refreshing the sender**;
3. receiver after its download/refresh.

The second checkpoint demonstrates a successful upload attempt. The third is the
only evidence of peer delivery. A successful controller command, a background
gesture, a refresh animation, or a screenshot alone does not establish either.

Wait for a bounded, observable transfer (log, provider metadata, or an explicit
app result), then refresh the receiver and inspect the named data. If delivery is
late, retry *refresh/observation* within the run's stated bound; do not repeat the
mutation. If the bound expires, preserve the checkpoints and mark the scenario
`BLOCKED` or `FAIL` as appropriate.

Use these outcomes:

| Outcome | Meaning |
| --- | --- |
| `PASS` | The specified sender upload and receiver state were both observed. |
| `FAIL` | Required behavior was observed to be wrong with usable evidence. |
| `BLOCKED` | An external prerequisite or test transport prevented a conclusion. |
| `NOT RUN` | The scenario was intentionally not attempted. |

Never turn a controller exit, unavailable device connection, or incomplete peer
check into `PASS`.

## Portable scenario sequence

Start each scenario from the recorded baseline. Force an initial sync and record
the result before creating the intended stale peer. For stale scenarios,
**device2** downloads the baseline, then remains offline from refreshes while
**device1** makes and uploads the remote edit. Do not refresh device2 until the
scenario says so.

| # | Scenario | Exact sequence and expected receiver result |
| --- | --- | --- |
| 1 | Initial discovery | Start with a known backup from device1 and record its contents. Link an empty device2 to the same dev account and sync it. Verify it downloads the existing backup, rather than uploading its empty state, and its subscriptions, categories and known article flags equal the baseline. |
| 2 | Flag matrix | The deterministic suite runs all 16 `(read, bookmarked)` transitions. In a live run, cover representative real transfers: bookmark an unread article, peer marks it read while a stale sender clears the bookmark, and clear both flags. Assert both flags, including explicit `false/false`; do not infer one from a filter alone. |
| 3 | Stale unrelated flag edit | device2 is stale. device1 marks **article1** read and uploads. device2 bookmarks **article1** without a preliminary refresh, uploads, then converges. article1 is read and bookmarked on both peers. |
| 4 | Source/category CRUD | Create a source and category, assign the source, upload and receive. Rename the source and category, reassign or uncategorize the source, then delete the created source. Verify IDs where visible and final absence on the peer. |
| 5 | Stale unrelated source/category fields | device2 holds an old source/category snapshot. device1 renames category **old-title** to **new-title** and uploads. device2 renames source **source-old-title** to **source-new-title**, uploads stale, then both refresh. Both new titles survive and the source keeps its category assignment. |
| 6 | Category-only deletion | Create or choose a disposable category with a source. Delete the category through the app's category action, upload, and receive. The category is absent and the source survives as Uncategorized (`category_id = NULL` where inspectable). |
| 7 | Remote source deletion with stale peer bookmark | device2 is stale and still has disposable source **source1**. device1 deletes source1 and uploads. Without refreshing, device2 bookmarks unrelated **article2** and uploads. The prepared upload and final receiver both omit source1 and retain article2's bookmark. Then refresh device2 and verify source1 is removed locally. |

The 16-state matrix remains automated because read and bookmark are independent
fields; every live scenario in this table is a representative real-provider check.
Keep the fixture IDs and the actual saved titles in the report—input helpers can
alter text.

## Provider preparation

Build and install the intended revision using the platform commands in
[AGENTS.md](../AGENTS.md), then record the build/commit on each device. All
participants must use the same debug/release snapshot namespace. Android can
use the installed Android CLI; desktop can use Compose Hot Reload for ordinary
Dropbox/Drive checks or native UI automation for a packaged app. Use accessibility
targets from a fresh UI tree; coordinates can hit adjacent articles. iCloud
desktop checks require the signed package below.

### Dropbox and Google Drive

Link each device through the shipping provider UI. For Android Google Drive, the
debug OAuth client must be registered for the **current** debug package name and
the SHA-1 of the keystore used for this build. Derive both locally rather than
copying a personal fingerprint:

```sh
./gradlew --quiet --console=plain :androidApp:signingReport
```

Confirm the matching SHA-1 in the OAuth console, then verify the actual package
separately from the built APK or the variant's Gradle configuration before
retrying sign-in. `UNREGISTERED_ON_API_CONSOLE` is a configuration block, not a
sync failure. Do not paste the fingerprint, client ID, or account details into
this document or the run report.

For iOS Drive, configure a registered **iOS** OAuth client for the installed
bundle ID, using the same Google Cloud project as the other test platforms.
The Debug build reads `GID_CLIENT_ID` and `GID_REVERSED_CLIENT_ID` from the ignored
`iosApp/Assets/Config-Debug.xcconfig`; its tracked template contains placeholders.
The reversed client ID must match the app's callback URL scheme. A compile-only
dummy configuration cannot authenticate. Preserve existing local credential
files and complete login through the app UI before measuring sync behavior.

Drive is intentionally unavailable in the shipping Flatpak build. Record that
pairing as `NOT RUN`, not `FAIL`.

### iCloud on macOS and iOS

iCloud needs a signed, sandboxed macOS desktop bundle and an iOS app signed into
the same authorized Apple account/container. Build the desktop distributable with
the App Store entitlement path while retaining development data:

```sh
./gradlew --quiet --console=plain :desktopApp:createDistributable -PmacOsAppStoreRelease=true
```

Before launch, verify that the packaged `props.properties` has `is_release=false`.
Copy the bundle into `RUN_DIR`, then embed a **matching current** Mac development
profile and sign it with the current Apple Development identity and the existing
desktop entitlements. Inspect the selected profile, identity, and entitlements
locally; do not record personal certificate IDs. Verify the resulting bundle with
`codesign --verify --deep --strict <bundle-path>`.

After choosing a profile that includes this Mac and matches the signing
certificate, team, app ID and iCloud container, the local packaging sequence is:

```sh
APP_COPY="$RUN_DIR/FeedFlow iCloud Test.app"
MAC_DEVELOPMENT_PROFILE='<path-to-matching-development.provisionprofile>'
SIGNING_IDENTITY='<current-Apple-Development-identity>'
ditto desktopApp/build/release/main/app/FeedFlow.app "$APP_COPY"
cp "$MAC_DEVELOPMENT_PROFILE" "$APP_COPY/Contents/embedded.provisionprofile"
codesign --force --timestamp --options runtime --sign "$SIGNING_IDENTITY" \
  --entitlements desktopApp/entitlements.plist "$APP_COPY"
codesign --verify --deep --strict "$APP_COPY"
open "$APP_COPY"
```

Use a new copy path. Keep the package's nested native/runtime signatures intact.
An App Store distribution profile is not a substitute for a profile permitting
local development launch. If host security or device services are sandbox-denied,
use the tool's authorized host-access mechanism before diagnosing missing identities
or a broken device; do not recreate certificates merely because a restricted query failed.

This app has sandbox data, normally under its container's Application Support
directory; it is different from the ordinary unsandboxed development database.
Read copies only. The purpose is to preserve the production sandbox/iCloud
entitlements while using debug data and the debug cloud filename. Never replace a
shipping app installation for this check.

For the current debug desktop bundle the databases are in
`~/Library/Containers/com.prof18.feedflow/Data/Library/Application Support/FeedFlow-dev/`
(`FeedFlow.db` and `FeedFlowFeedSyncDB-debug.db`). Confirm the running bundle and
data directory before querying: an ordinary development window can still be
open against a different database. Use SQLite read-only mode; copying a live
database without its WAL can produce stale evidence.

The macOS bridge is `shared/src/jvmMain/kotlin/com/prof18/feedflow/shared/domain/feedsync/ICloudNativeBridge.kt`;
the native implementation is under `feedSync/ikloud-macos/src/macosMain/`.
Rebuild and repackage the native libraries whenever either side of that native
source/interface changes. A passing local helper test does not demonstrate signed
bundle loading or iCloud peer propagation.

## iOS control helper

`tools/cloud-sync-live/controller.py` is the optional standard-library Python
transport for a physical iPhone controller. It changes no app behavior, is outside
`allTests`, and does not replace the deterministic harness. Consult its actual
surface when needed:

```sh
python3 tools/cloud-sync-live/controller.py --help
python3 tools/cloud-sync-live/controller.py send --help
python3 -m unittest discover -s tools/cloud-sync-live/tests
```

Prerequisites: macOS, Xcode with the device's SDK, XcodeGen (`brew install
xcodegen` if missing), Python 3, and the XcodeBuildMCP CLI. The iPhone must be
trusted, unlocked, in Developer Mode, and available for UI automation. Use a
signing team and separate controller bundle ID for which local development
signing is configured. Accept a device automation prompt if it appears.

The FeedFlow dev app must already be installed. If it needs deployment, generate
the app project with `./.scripts/generate-project.sh` from `iosApp/` as
described in AGENTS, configure real provider credentials, then run:

```sh
xcodebuildmcp device build-and-run \
  --project-path iosApp/FeedFlow.xcodeproj --scheme FeedFlow \
  --configuration Debug --device-id '<device-udid>' --extra-args=-quiet
```

The normal Debug bundle ID is `com.prof18.feedflow.dev`; use that for
`--app-bundle-id` unless the installed build is customized. The controller host
has a different ID and does not replace FeedFlow. Regenerate only when the app
project is missing or its generation inputs changed.

`prepare` requires a fresh, nonexistent run directory. It generates only the
isolated XcodeGen project; it does **not** install or run the controller. Start
the device test separately, keep that terminal/job running, and wait for the
actual execution session rather than treating JavaScript or command-launch output
as the controller result.

```sh
python3 tools/cloud-sync-live/controller.py prepare \
  --run-dir "$CONTROLLER_DIR" --device '<device-udid>' \
  --app-bundle-id '<app-bundle-id>' \
  --controller-bundle-id '<controller-bundle-id>' --team-id '<team-id>' \
  --timeout-seconds 5400

xcodebuildmcp device test \
  --project-path "$CONTROLLER_DIR/ios-project/LiveCloudSyncHost.xcodeproj" \
  --scheme LiveCloudSyncHost --device-id '<device-udid>' \
  --derived-data-path "$CONTROLLER_DIR/build" \
  --extra-args=-quiet --extra-args=-parallel-testing-enabled \
  --extra-args=NO \
  --extra-args=-only-testing:LiveCloudSyncTests/LiveCloudSyncTests/testControlSession
```

The generated project is `$CONTROLLER_DIR/ios-project/LiveCloudSyncHost.xcodeproj`;
the configured controller session lifetime is 5400 seconds. The target FeedFlow
app is not automatically activated at startup. Once the test session is live, use `send`
with its default 60-second timeout:

```sh
python3 tools/cloud-sync-live/controller.py send \
  --run-dir "$CONTROLLER_DIR" activate
python3 tools/cloud-sync-live/controller.py send \
  --run-dir "$CONTROLLER_DIR" inspect
```

Supported actions are `inspect`, `tap`, `longPress`, `typeText`, `swipeUp`,
`swipeDown`, `refresh`, `activate`, `background`, `screenshot`, and `stop`.
`--target` is required for `tap` and `longPress`, and optional for `swipeUp` and
`swipeDown`. `typeText` writes at the current focus and does not replace existing
text. Inspect and select existing text through the UI before replacing it;
do not assume tapping a field puts its cursor at the end. Run `refresh` from
the timeline top with no overlay. Use
`screenshot` deliberately; it is optional evidence and never proof of delivery.

Each command saves JSON records and response trees under
`$CONTROLLER_DIR/commands/`, identified by run and command UUID, and returns nonzero
on errors. A timed-out command retains its pending result and blocks new commands:
run `collect --run-dir "$CONTROLLER_DIR"` to obtain that result without resending.
After manual state inspection, `abandon --run-dir "$CONTROLLER_DIR"` explicitly
clears a pending command without replay. Stop the controller when finished. Do
not add provider-specific controller scripts or wait without a stated bound. Run
one agent per controller runner; when automating it, wait on the real execution
session, not only the JavaScript command-launch output.

## Reporting and new-provider onboarding

Write `RUN_DIR/report.md` as the durable handoff. Keep it concise:

```md
# <provider> live cloud-sync run — <date>

Devices: device1=<platform/build>; device2=<platform/build>
Account/fixture scope: <disposable alias and authorized fixture description>
Transfer bound: <duration and observation method>

| Scenario | Result | Sender upload evidence | Receiver evidence | Notes |
| --- | --- | --- | --- | --- |
| Initial discovery | PASS/FAIL/BLOCKED/NOT RUN | <path> | <path> | <facts> |
```

State what was not run and why. Link redacted evidence by relative path. Separate
an app defect from a blocked controller/device transport; an interrupted session
is evidence of neither until the scenario can be completed.

When adding a provider, first extend the `CloudProvider` enum and provider
behavior in
`shared/src/commonTest/kotlin/com/prof18/feedflow/shared/test/cloudsync/CloudStore.kt`,
then wire its device account and transport in the source-set implementations of
the `createCloudDevice` factory declared in `CloudDevice.kt`:

- `shared/src/jvmTest/.../cloudsync/JvmCloudDevice.kt` and `JvmCloudTransports.kt`;
- `shared/src/androidTest/.../cloudsync/AndroidCloudDeviceFactory.kt` and
  `AndroidCloudTransports.kt`, used by the host and optional device actuals;
- `shared/src/iosTest/.../cloudsync/CloudDevice.ios.kt`.

Reuse the portable cases in `CloudSuccessScenarios.kt`. Add a transport double at the provider boundary;
retain the real platform worker, repositories and SQLite snapshots. Run the
existing regression families from every supported platform suite, and add
provider-specific tests for missing-file classification, authorization failures,
revision/conflict semantics, retries and SDK callbacks. Keep live steps provider-neutral; do not add a
controller script per provider. Locate the actual integration points rather than
guessing:

```sh
rg -n "<Provider>|CloudProvider" feedSync shared androidApp iosApp desktopApp
rg -n "runCloudFlagRegressions|runCloudFeedAndCategoryRegressions" shared/src
```

`CloudFlagRegressions.kt` and `CloudFeedAndCategoryRegressions.kt` hold the
shared assertions. The platform suites are `AndroidCloudSyncSuccessTest.kt`,
`DesktopCloudSyncSuccessTest.kt`, and `IosCloudSyncSuccessTest.kt`: add an explicit
provider test entry to each supported suite so the new wiring actually runs.
Adding an enum value alone does not schedule its scenarios. Provider
adapters live below `feedSync/<provider>/`, with app authorization wiring under
the platform app modules. Update the matrix, setup prerequisites, evidence method,
and supported pairings only after those sources and deterministic, real-platform
provider-SDK double coverage support the provider.
