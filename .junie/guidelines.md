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

## Development Workflow
1. **Code Organization**:
   - Place shared logic in `shared` module
   - Platform-specific code goes in respective app modules
   - Use `expect/actual` for platform-specific implementations

2. **Best Practices**:
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
- Avoid putting default values in the functions
- do not write any tests unless told you to do so
- Never put default values on data classes
- if you are developing for Android and desktop, DO NOT link and compile the shared module for iOS.