# Baseline Profile Module

This module generates baseline profiles for the FeedFlow Android app to improve app startup performance.

## What are Baseline Profiles?

Baseline profiles tell the Android Runtime (ART) which parts of the app to ahead-of-time (AOT) compile, improving app startup time and reducing jank during initial use.

## Generating the Baseline Profile

To generate the baseline profile for app startup:

1. Connect an Android device or start an emulator (API 28+)
2. Make sure you have a **release build** of the app variant you want to profile
3. Run the baseline profile generation task:

```bash
./gradlew :baselineprofile:generateBaselineProfile
```

This will:
- Install the app on the device
- Run the startup benchmark test
- Generate a baseline profile
- Save the profile to `androidApp/src/main/baseline-prof.txt`

## Configuration

The baseline profile is configured to capture:
- **App startup** - The critical path from app launch to first render

The generator uses UI Automator to:
1. Press home
2. Start the app
3. Wait for the app to be fully loaded

## Build Integration

The baseline profile is automatically included in release builds through the `androidx.baselineprofile` Gradle plugin.

## References

- [Baseline Profiles Overview](https://developer.android.com/topic/performance/baselineprofiles/overview)
- [Create Baseline Profiles](https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile)
