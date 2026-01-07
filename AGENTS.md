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

- `./gradlew check` -> Run all checks including tests and linting for Shared code, Android and Desktop
- `./gradlew detekt` -> Run static analysis with Detekt for Shared code, Android and Desktop
- `.scripts/ios-format.sh` -> Format iOS code through swiftformat and swiftlint
- `./gradlew test` -> Run all tests for Shared code, Android and Desktop
- `.scripts/refresh-translations.sh` -> Regenerate i18n translation code after adding new translations
- `./gradlew :androidApp:assembleDebug` -> Build Android debug
- `./gradlew desktopApp:run` -> Run Desktop app
-
### Building for iOS Simulator
To build ReaderFlow for iPhone 17 Pro simulator:
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


### Build Verification Process

IMPORTANT: When editing code, you MUST:
1. Build the project after making changes
2. Fix any compilation errors before proceeding
Be sure to build ONLY for the platform you are working on to save time.

## Handing off

Before handing off you must:
1. Run `./gradlew detekt` to ensure all checks pass
2. Run `.scripts/ios-format.sh` to format iOS code
3. Fix any issues found during the above steps

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

## General rules:

- DO NOT write comments for every function or class. Only write comments when the code is not self-explanatory.
- DO NOT write tests unless specifically told to do so.
- DO NOT excessively use try/catch blocks for every function. Use them only for the top caller or the bottom callers, depending on the cases.
- ALWAYS run gradle tasks with the following flag: `--quiet --console=plain`

### Git Commit Messages
When creating commits:
- Use simple, one-liner commit messages
- DO NOT include phase numbers (e.g., "Phase 1", "Phase 2")
- DO NOT add "Generated with Claude Code" attribution
- DO NOT add "Co-Authored-By: Claude" attribution
- Example: `git commit -m "Add foundation for unified article parsing system"`

### iOS Development
- ALWAYS build with xcodebuild with -quiet flag when building for iOS. If the command returns errors you may run xcodebuild again without the -quiet flag.
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

