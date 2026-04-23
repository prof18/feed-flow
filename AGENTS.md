# AGENTS.md

## Project Overview

FeedFlow is a multi-platform RSS reader built with Kotlin Multiplatform, Compose Multiplatform and SwiftUI.
It targets Android, iOS, macOS, Windows, and Linux.
The app uses SwiftUI for iOS-specific UI components and Compose for all other platforms.
All the business logic is shared via Kotlin Multiplatform.

## Project Structure & Module Organization

### Module Structure
- **core/**: Core domain models and utilities shared across all platforms
- **shared/**: Main business logic, repositories, view models, and data layer
- **sharedUI/**: Compose UI components shared across Android and Desktop
- **database/**: SQLDelight database implementation
- **i18n/**: Internationalization resources
- **feedSync/**: Feed synchronization modules (Dropbox, FreshRSS, iCloud, etc)
- **androidApp/**: Android-specific app implementation
- **iosApp/**: iOS app with SwiftUI
- **desktopApp/**: Desktop app for Windows, Linux, and macOS
- **website/**: Hugo-based website

## Build, Test, and Development Commands

### Build Commands
All Gradle commands in this section should be run with `--quiet --console=plain`.

- `./gradlew --quiet --console=plain detekt allTests` -> Run all checks including tests and linting for Shared code, Android and Desktop
- `./gradlew --quiet --console=plain detekt` -> Run static analysis with Detekt for Shared code, Android and Desktop
- `.scripts/ios-format.sh` -> Format iOS code through swiftformat and swiftlint
- `./gradlew --quiet --console=plain test` -> Run all tests for Shared code, Android and Desktop
- `.scripts/refresh-translations.sh` -> Regenerate i18n translation code after adding new translations
- `./gradlew --quiet --console=plain :androidApp:assembleGooglePlayDebug` -> Build Android debug
- `./gradlew --quiet --console=plain :androidApp:compileGooglePlayDebugKotlin` -> Quick compile check for Android (no APK assembly)
- `.scripts/run-android.sh` -> Install and launch Android Google Play debug (wraps `:androidApp:installGooglePlayDebug`)
- `./gradlew --quiet --console=plain desktopApp:run` -> Run Desktop app
- `./gradlew --quiet --console=plain :desktopApp:compileKotlinJvm` -> Quick compile check for Desktop
- `./gradlew --quiet --console=plain :desktopApp:jvmMainClasses` -> Compile Desktop main classes (faster than full build)
- `.scripts/delete-desktop-debug-db.sh` -> Delete local Desktop debug database and prefs
- `./gradlew --quiet --console=plain :shared:compileKotlinJvm` -> Quick compile check for shared module only (fastest iteration)
- `./gradlew --quiet --console=plain :feedSync:feedbin:build` -> Build a specific feedSync sub-module (pattern: `:feedSync:<module>:build`)
- `./gradlew --quiet --console=plain :desktopApp:packageDistributionForCurrentOS` -> Package desktop app distribution for the current OS

### Running Android App
Ensure an emulator or device is connected via `adb`, then run:
- `.scripts/run-android.sh` -> Install and launch the debug app on device/emulator

### iOS Project Generation

The iOS Xcode project (`iosApp/FeedFlow.xcodeproj`) is generated from `iosApp/project.yml` by [XcodeGen](https://github.com/yonaskolb/XcodeGen) and is NOT committed to git, except for `iosApp/FeedFlow.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/Package.resolved`. Regenerate it with:

```bash
cd iosApp && ./.scripts/generate-project.sh
```

This requires XcodeGen installed (`brew install xcodegen`, or `mint bootstrap` using `iosApp/Mintfile` which pins the version). Regenerate whenever `project.yml`, iOS source folder structure, xcconfigs, entitlements, or SPM dependencies change. The wrapper script also creates `Assets/Config-Debug.xcconfig` from the tracked template when it is missing, post-fixes `lastKnownFileType` for iOS 26 `.icon` bundles, injects `wasCreatedForAppExtension="YES"` into the Widget/Share extension schemes (XcodeGen drops this when the scheme also launches the host app — yonaskolb/XcodeGen#1523), and preserves the tracked `Package.resolved` file across project regeneration. Direct package requirements stay declared in `project.yml`, while the committed `Package.resolved` locks the fully resolved SwiftPM graph used by CI and release builds.

### Building for iOS Simulator

- If using XcodeBuildMCP, use the installed XcodeBuildMCP skill before calling XcodeBuildMCP tools.
- The xcodeproj must exist locally. Run `cd iosApp && ./.scripts/generate-project.sh` first if it's missing.

To build FeedFlow for iPhone 17 Pro simulator:
```bash
mcp__XcodeBuildMCP__build_sim_name_proj projectPath: "/Users/mg/Workspace/feedflow/feed-flow/iosApp/FeedFlow.xcodeproj" scheme: "FeedFlow" simulatorName: "iPhone 17 Pro"
```
There could be different project path son your machine. Always use the first one. The alternative paths will be:
```bash
mcp__XcodeBuildMCP__build_sim_name_proj projectPath: "/Users/mg/Workspace/feedflow/feed-flow-2/iosApp/FeedFlow.xcodeproj" scheme: "FeedFlow" simulatorName: "iPhone 17 Pro"
```

```bash
mcp__XcodeBuildMCP__build_sim_name_proj projectPath: "/Users/marco.gomiero/Workspace/tmp/feed-flow/iosApp/FeedFlow.xcodeproj" scheme: "FeedFlow" simulatorName: "iPhone 17 Pro"
```


### Running Specific Tests
- `./gradlew --quiet --console=plain :shared:allTests` -> Run all shared module tests across supported targets
- `./gradlew --quiet --console=plain :shared:jvmTest --tests "com.prof18.feedflow.shared.presentation.SomeTest"` -> Run a specific test class on JVM
- `./gradlew --quiet --console=plain :shared:iosSimulatorArm64Test` -> Run shared tests on iOS simulator

### Build Verification Process

IMPORTANT: When editing code, you MUST:
1. Build the project after making changes
2. Fix any compilation errors before proceeding
   Be sure to build ONLY for the platform you are working on to save time.

## Handing off

Before handing off you must:
1. Run `.scripts/refresh-translations.sh` before the Gradle checks if you changed translation resources
2. Run `./gradlew --quiet --console=plain detekt allTests` to ensure Kotlin/shared/Android/Desktop checks pass - don't run it if you modified only swift files
3. Run `.scripts/ios-format.sh` to format iOS code - only run if you made changes on the iOS app
4. If you changed iOS code, run `xcodebuild -project iosApp/FeedFlow.xcodeproj -scheme FeedFlow -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -onlyUsePackageVersionsFromResolvedFile build -quiet` before handoff; rerun without `-quiet` only if you need the full diagnostics
5. Fix any issues found during the above steps

### Initial Setup (for building from scratch)
```bash
# Android: Copy dummy google-services.json
cp config/dummy-google-services.json androidApp/src/debug/google-services.json
cp config/dummy-google-services.json androidApp/src/release/google-services.json

# iOS: Copy dummy GoogleService-Info.plist
cp config/dummy-google-service.plist iosApp/GoogleService-Info-dev.plist
cp config/dummy-google-service.plist iosApp/GoogleService-Info.plist

# iOS: Create Config.xcconfig
cp iosApp/Assets/Config.xcconfig.template iosApp/Assets/Config.xcconfig
```

For compile-only local/CI iOS builds without real sync credentials, you can use `cp config/dummy-config.xcconfig iosApp/Assets/Config.xcconfig` instead.

## Testing

When writing tests, follow the comprehensive testing guide at **`.ai/TESTING.md`**.

Key points:
- All tests extend `KoinTestBase` for dependency injection
- Use Turbine for Flow testing
- Prefer fakes over mocking libraries
- Use data generators from `shared/src/commonTest/.../test/generators/`
- For sync service tests, use the `feedSync/test-utils` module (provides mock HTTP engines and Koin modules for GReader/Feedbin)
- Run specific test classes with `--tests "fully.qualified.ClassName"` to iterate quickly

## General rules:

- DO NOT write comments for every function or class. Only write comments when the code is not self-explanatory.
- If you touch or create any business logic, ensure it's thoroughly tested with unit tests.
- DO NOT excessively use try/catch blocks for every function. Use them only for the top caller or the bottom callers, depending on the cases.
- ALWAYS run gradle tasks with the following flag: `--quiet --console=plain`

### Desktop screens/windows
- For desktop settings/details pages opened from the main screen, prefer a dedicated `DialogWindow` instead of in-window navigation.
- Reuse `desktopApp/src/jvmMain/kotlin/com/prof18/feedflow/desktop/ui/components/DesktopDialogWindow.kt` for new desktop windows instead of duplicating window setup.
- Use `desktopApp/src/jvmMain/kotlin/com/prof18/feedflow/desktop/main/DesktopDialogWindowNavigator.kt` to open/close windows from `MainWindow` (add destinations to the enum and keep visibility state there).
- Keep the screen body content as a composable content block and avoid duplicating content calls; apply platform conditionals only to wrapper chrome (for example, toolbar/title handling).
- On macOS, keep transparent title bar handling inside the reusable desktop window wrapper; on other desktop platforms avoid adding duplicate custom title UI.

### Git Commit Messages
When creating commits:
- Use simple, one-liner commit messages
- DO NOT include phase numbers (e.g., "Phase 1", "Phase 2")
- DO NOT add "Generated with Claude Code" attribution
- DO NOT add "Co-Authored-By: Claude" attribution
- Example: `git commit -m "Add foundation for unified article parsing system"`

### macOS Desktop Code Signing
- Native libraries in `desktopApp/resources-sandbox/macos-arm64/` must be signed with the Mac App Store certificate when it is renewed.
- Sign with: `codesign --force --timestamp --options runtime --sign "3rd Party Mac Developer Application: Marco Gomiero (Q7CUB3RNAK)" <path>`
- Verify with: `codesign -dvvv <path>`

### iOS Development
- ALWAYS build with xcodebuild with -quiet flag when building for iOS. If the command returns errors you may run xcodebuild again without the -quiet flag.
- Direct xcodebuild alternative: `xcodebuild -project iosApp/FeedFlow.xcodeproj -scheme FeedFlow -destination 'platform=iOS Simulator,name=iPhone 17 Pro' -onlyUsePackageVersionsFromResolvedFile build -quiet`
- IMPORTANT: The project now supports iOS 26 SDK (June 2025) while maintaining iOS 18 as the minimum deployment target. Use #available checks when adopting iOS 26+ APIs.
- Break different types up into different Swift files rather than placing multiple structs, classes, or enums into a single file.
- Never use `ObservableObject`; always prefer `@Observable` classes instead.
- Never use `Task.sleep(nanoseconds:)`; always use `Task.sleep(for:)` instead.
- Avoid `AnyView` unless it is absolutely required.
- Avoid force unwraps and force `try` unless it is unrecoverable.

### Internationalization
- String resources are located in `i18n/src/commonMain/resources/locale/values-[language]/`
- Run .scripts/refresh-translations.sh after adding a new translation, to re-generate the kotlin code
- NEVER add hardcoded strings in the code. Always use the i18n resources.
- NEVER try to translate other languages by yourself. Add only the English strings. The translations will be handled by professionals later.

### Flatpak / Linux Desktop
- `.scripts/flatpak-build-setup.sh` -> Prepare the project for Flatpak builds (sets release props, disables JetBrains JDK vendor, disables toolchain auto-provisioning, comments out Android-only Gradle plugins)
- `.scripts/disable-android-for-flatpak.sh` -> Comments out all Android-related Gradle config across all `build.gradle.kts` and build-logic files; excludes `androidApp` from `settings.gradle.kts`
- Flatpak packaging files live in `desktopApp/packaging/flatpak/` (manifest, launch script, desktop entry, AppStream metadata, icon)
- The `flatpak=true` property in `desktopApp/src/jvmMain/resources/props.properties` is used to disable platform-specific features (e.g., Google Drive sync) in Flatpak builds
- HiDPI scaling for the JVM uses `sun.java2d.uiScale` JVM argument. For local testing: `./gradlew --quiet --console=plain desktopApp:run -PjvmArgs="-Dsun.java2d.uiScale=2.0"`

### CI/CD
- CI config: `.github/workflows/code-checks.yaml`
- Pipeline: `checks` (macos-26, detekt + allTests + swiftlint) -> `build-android-app` + `build-desktop-app` + `build-ios-app` (in parallel)
- iOS CI build forces arm64 simulator architecture: `xcodebuild -project iosApp/FeedFlow.xcodeproj -configuration Debug -scheme FeedFlow -sdk iphonesimulator -destination "generic/platform=iOS Simulator" ARCHS=arm64 ONLY_ACTIVE_ARCH=YES -onlyUsePackageVersionsFromResolvedFile build | xcbeautify --renderer github-actions`
- CI runs `.scripts/refresh-translations.sh` before checks; do this locally before pushing if translations changed
- Debugging CI failures: `gh run list --limit=10`, then `gh run view <run-id> --log`
- Red CI recovery loop: `gh run rerun <run-id>` (or `gh run rerun <run-id> --failed`), then fix and push until green
