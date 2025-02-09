# FeedFlow Developer Guidelines

## Project Overview
FeedFlow is a cross-platform RSS Reader built with Kotlin Multiplatform,
supporting Android, iOS, and macOS platforms. 
The project uses modern technologies and shared code architecture to maintain
consistency across platforms.

## Tech Stack
- **Core**: Kotlin Multiplatform
- **UI Frameworks**: 
  - Jetpack Compose (Android, Desktop)
  - Compose Multiplatform (Desktop)
  - SwiftUI (iOS)
- **Database**: SQLDelight
- **Serialization**: Kotlin Serialization
- **Analytics**: Firebase Crashlytics
- **Code Quality**: Detekt
- **iOS Interop**: SKIE

## Project Structure
```
feed-flow/
├── androidApp/     # Android-specific implementation
├── iosApp/         # iOS-specific implementation
├── desktopApp/     # macOS-specific implementation
├── shared/         # Shared business logic and models
├── sharedUI/       # Shared UI components
├── core/          # Core utilities and extensions
├── database/      # Database layer (SQLDelight)
├── feedSync/      # Feed synchronization logic
├── i18n/          # Internationalization resources
└── build-logic/   # Custom build configuration
```

## Setup Instructions
1. **Basic Setup**:
   - Clone the repository
   - Use Android Studio or IntelliJ IDEA with Kotlin Multiplatform support

2. **Android Setup**:
   - Copy dummy Google Services file:
     ```bash
     cp config/dummy-google-services.json androidApp/src/debug/google-services.json
     cp config/dummy-google-services.json androidApp/src/release/google-services.json
     ```
   - Create keystore.properties for Dropbox integration

3. **iOS Setup**:
   - Copy and configure FeedFlow.xcconfig
   - Set up GoogleService-Info.plist
   - Configure Config.xcconfig for Dropbox integration

4. **Desktop Setup**:
   - Configure props.properties for Dropbox integration
   - Run using: `./gradlew desktopApp:run`

## Development Workflow
1. **Code Organization**:
   - Place shared logic in `shared` module
   - Platform-specific code goes in respective app modules
   - Use `expect/actual` for platform-specific implementations

2. **Testing**:
   - Write tests for shared code in `shared` module
   - Use Maestro for UI testing
   - Run platform-specific tests before submitting PRs

3. **Best Practices**:
   - Follow Kotlin coding conventions
   - Use dependency injection
   - Keep UI logic in presentation layer
   - Maintain clear separation of concerns
   - Write comprehensive tests for new features

## Running Tests
- **Shared Tests**: `./gradlew :shared:test`
- **Android Tests**: `./gradlew :androidApp:testDebugUnitTest`
- **UI Tests**: Use Maestro test suite in .maestro directory

## Common Tasks
- **Clean Build**: `./gradlew clean`
- **Run Desktop App**: `./gradlew desktopApp:run`
- **Generate Localization**: Check i18n module
- **Code Analysis**: Run Detekt using `./gradlew detekt`

## Things to consider:
- Every new string should be added to the `i18n` module and not hardcoded. The translated strings are not used with the xml key but with the Kotlin generated value 
- When adding new strings, the StringsVersion number needs to be bumped.
- The database migrations are placed in the `sqldelight/com/prof18/feedflow/migrations` folder