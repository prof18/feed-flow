# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FeedFlow is a cross-platform RSS reader built with Kotlin Multiplatform, Compose Multiplatform and SwiftUI. It targets Android, iOS, macOS, Windows, and Linux. The app uses SwiftUI for iOS-specific UI components and Compose for all other platforms.

## Architecture

### Module Structure
- **core/**: Core domain models and utilities shared across all platforms
- **shared/**: Main business logic, repositories, view models, and data layer
- **sharedUI/**: Compose UI components shared across platforms
- **database/**: SQLDelight database implementation
- **i18n/**: Internationalization resources
- **feedSync/**: Feed synchronization modules (Dropbox, FreshRSS, iCloud)
- **androidApp/**: Android-specific app implementation
- **iosApp/**: iOS/macOS app with SwiftUI
- **desktopApp/**: Desktop app for Windows, Linux, and macOS
- **website/**: Hugo-based website

### Key Technologies
- **Kotlin Multiplatform**: Core logic sharing
- **Compose Multiplatform**: UI framework for Android/Desktop
- **SwiftUI**: iOS native UI
- **SQLDelight**: Database abstraction
- **Koin**: Dependency injection
- **Ktor**: HTTP client
- **RSS Parser**: Custom RSS parsing library

## Common Development Commands

### Build Commands
```bash
# Build the entire project
./gradlew build

# Build Android app
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:assembleRelease

# Build shared framework for iOS
./gradlew :shared:linkDebugFrameworkIosArm64
```

### Code Quality Commands
```bash
# Run Detekt linting
./gradlew detekt

# Format iOS/macOS code
.scripts/io-format.sh
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
The iOS app uses Xcode and requires iOS-specific setup:
- Open `iosApp/FeedFlow.xcodeproj` in Xcode
- Build and run from Xcode

- ALWAYS build with xcodebuild with -quiet flag when building for iOS. If the command returns errors you may run xcodebuild again without the -quiet flag.

## Code Structure Notes

### Shared Logic
- ViewModels in `shared/src/commonMain/kotlin/.../presentation/`
- Repositories in `shared/src/commonMain/kotlin/.../domain/`
- Platform-specific implementations use `expect`/`actual` pattern

### Platform-Specific Code
- Android: `shared/src/androidMain/kotlin/`
- iOS: `shared/src/iosMain/kotlin/`
- Desktop: `shared/src/jvmMain/kotlin/`

### Database
- SQLDelight schema in `database/src/commonMain/sqldelight/`
- Database drivers are platform-specific

### Dependency Injection
- Koin modules in `shared/src/commonMain/kotlin/.../di/`
- Platform-specific modules in respective platform folders

## Testing
- Common tests in `shared/src/commonTest/kotlin/`
- Platform-specific tests in respective test folders
- Uses Kotlin Test framework with JUnit on JVM platforms

## Internationalization
- String resources in `i18n/src/commonMain/resources/locale/values-[language]/`
- Refresh
- Uses Lyricist for multiplatform string handling
- Translation management via Weblate
- Run .scripts/refresh-translations.sh after adding a new translation, to re-generate the kotlin code

## Key Features to Understand
- **Feed Sync**: Supports Dropbox, FreshRSS, and iCloud synchronization
- **Reader Mode**: HTML content extraction and formatting
- **OPML Import/Export**: Standard RSS subscription format support
- **Widgets**: Android App Widget and iOS Widget support
- **Notifications**: Background feed update notifications