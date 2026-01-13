# FeedFlow Desktop: Voyager → Navigation 3 Migration Plan

## Overview

Migrate the FeedFlow desktop app from Voyager navigation to JetBrains Navigation 3 for Compose Multiplatform. The Android app has already completed this migration successfully and serves as our reference implementation.

**Key Points:**
- Desktop currently has 15+ screens using Voyager's `Screen` interface
- Will convert to `@Serializable` `NavKey` classes (type-safe navigation)
- Must use **JetBrains artifacts** (`org.jetbrains.androidx` namespace) for multiplatform support
- SharedUI is already navigation-agnostic (uses callbacks) - no changes needed there

---

## 1. Dependencies

### Add to `gradle/libs.versions.toml`

**Versions section:**
```toml
[versions]
nav3-multiplatform = "1.0.0"  # JetBrains multiplatform version
```

**Libraries section:**
```toml
[libraries]
# JetBrains Navigation 3 (multiplatform namespace - NOT androidx!)
jetbrains-navigation3-runtime = { module = "org.jetbrains.androidx.navigation:navigation-runtime", version.ref = "nav3-multiplatform" }
jetbrains-lifecycle-viewmodel-navigation3 = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "lifecycle-viewmodel-nav3" }
```

**Remove:**
- `voyager = "1.0.1"` version
- `voyager-navigator` and `voyager-transition` libraries

### Update `desktopApp/build.gradle.kts`

**Remove (lines 66-67):**
```kotlin
implementation(libs.voyager.navigator)
implementation(libs.voyager.transition)
```

**Add:**
```kotlin
implementation(libs.jetbrains.navigation3.runtime)
implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)
```

**Keep:** `kotlinx-serialization-json` (already present, needed for `@Serializable`)

---

## 2. Create Route Definitions

### New file: `desktopApp/src/jvmMain/kotlin/com/prof18/feedflow/desktop/Screen.kt`

All navigation destinations as `@Serializable` data classes/objects implementing `NavKey`:

```kotlin
package com.prof18.feedflow.desktop

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Home : NavKey
@Serializable data object Search : NavKey
@Serializable data object Accounts : NavKey
@Serializable data object FeedSuggestions : NavKey
@Serializable data object AddFeed : NavKey
@Serializable data object ImportExport : NavKey

@Serializable
data class ReaderMode(
    val id: String,
    val url: String,
    val title: String,
) : NavKey

@Serializable
data class EditFeed(
    val id: String,
    val url: String,
    val title: String,
    val categoryId: String?,
    val categoryTitle: String?,
    val lastSyncTimestamp: Long?,
    val logoUrl: String?,
    val websiteUrl: String?,
    val linkOpeningPreference: String,
    val isHidden: Boolean,
    val isPinned: Boolean,
    val isNotificationEnabled: Boolean,
    val fetchFailed: Boolean,
) : NavKey

// Sync screens
@Serializable data object DropboxSync : NavKey
@Serializable data object GoogleDriveSync : NavKey
@Serializable data object ICloudSync : NavKey
@Serializable data object FreshRssSync : NavKey
@Serializable data object MinifluxSync : NavKey
@Serializable data object BazquxSync : NavKey
@Serializable data object FeedbinSync : NavKey

// Settings
@Serializable data object BlockedWords : NavKey
@Serializable data object FeedSourceList : NavKey
```

### Conversion helpers (add to Screen.kt)

```kotlin
// Convert domain models to/from routes
fun FeedSource.toEditFeed(): EditFeed = EditFeed(
    id = id,
    url = url,
    title = title,
    categoryId = category?.id,
    categoryTitle = category?.title,
    lastSyncTimestamp = lastSyncTimestamp,
    logoUrl = logoUrl,
    websiteUrl = websiteUrl,
    linkOpeningPreference = linkOpeningPreference.name,
    isHidden = isHidden,
    isPinned = isPinned,
    isNotificationEnabled = isNotificationEnabled,
    fetchFailed = fetchFailed,
)

fun EditFeed.toFeedSource(): FeedSource = FeedSource(
    id = id,
    url = url,
    title = title,
    category = if (categoryId != null && categoryTitle != null) {
        FeedSourceCategory(categoryId, categoryTitle)
    } else null,
    lastSyncTimestamp = lastSyncTimestamp,
    logoUrl = logoUrl,
    websiteUrl = websiteUrl,
    linkOpeningPreference = LinkOpeningPreference.valueOf(linkOpeningPreference),
    isHidden = isHidden,
    isPinned = isPinned,
    isNotificationEnabled = isNotificationEnabled,
    fetchFailed = fetchFailed,
)

fun FeedItemUrlInfo.toReaderMode(): ReaderMode = ReaderMode(
    id = id,
    url = url,
    title = title,
)
```

---

## 3. ComposeWindow Access Pattern

### New file: `desktopApp/src/jvmMain/kotlin/com/prof18/feedflow/desktop/utils/CompositionLocals.kt`

```kotlin
package com.prof18.feedflow.desktop.utils

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.awt.ComposeWindow

val LocalComposeWindow = compositionLocalOf<ComposeWindow> {
    error("ComposeWindow not provided")
}
```

**Usage:** ImportExportScreen needs ComposeWindow for file dialogs - provide via CompositionLocal instead of constructor parameter.

---

## 4. ViewModel Management

### New file: `desktopApp/src/jvmMain/kotlin/com/prof18/feedflow/desktop/utils/ViewModelUtils.kt`

```kotlin
package com.prof18.feedflow.desktop.utils

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.viewModelStoreOwner
import org.koin.core.parameter.ParametersDefinition
import org.koin.mp.KoinPlatform

@Composable
inline fun <reified T : ViewModel> koinNavViewModel(
    noinline parameters: ParametersDefinition? = null,
): T {
    val koin = KoinPlatform.getKoin()
    return viewModel(
        viewModelStoreOwner = viewModelStoreOwner(),
        factory = { koin.get<T>(parameters = parameters) },
    )
}
```

**Key changes:**
- Replace `screenViewModel()` (Voyager-specific) with `koinNavViewModel()` (Navigation 3)
- ViewModels automatically scoped to navigation entries and disposed when entry removed
- Keep `desktopViewModel()` for non-navigation ViewModels (like HomeViewModel created outside navigation)

**Delete:** `desktopApp/src/jvmMain/kotlin/com/prof18/feedflow/desktop/DesktopViewModel.kt` (Voyager-specific)

---

## 5. Update MainWindow.kt

### Remove Voyager imports:
```kotlin
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.ScaleTransition
```

### Add Navigation 3 imports:
```kotlin
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.compose.animation.*
import com.prof18.feedflow.desktop.utils.LocalComposeWindow
```

### Replace Navigator setup (lines 301-366):

```kotlin
// Create back stack
val backStack = rememberNavBackStack<NavKey>(Home)

// Provide ComposeWindow to descendants
CompositionLocalProvider(LocalComposeWindow provides window) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },

        // Forward navigation transitions
        transitionSpec = {
            if (reduceMotionEnabled) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith
                    (fadeOut() + scaleOut(targetScale = 0.9f))
            }
        },

        // Back navigation transitions
        popTransitionSpec = {
            if (reduceMotionEnabled) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                val rightEdgeOrigin = TransformOrigin(pivotFractionX = 1f, pivotFractionY = 0.5f)
                EnterTransition.None togetherWith scaleOut(
                    targetScale = 0.9f,
                    transformOrigin = rightEdgeOrigin,
                )
            }
        },

        // Map routes to composables
        entryProvider = entryProvider {
            // Entry definitions (see section 6)
        },
    )
}
```

### Update MenuBar call to pass backStack:
```kotlin
FeedFlowMenuBar(
    state = menuBarState,
    actions = menuBarActions,
    backStack = backStack,  // ADD THIS
)
```

---

## 6. Entry Provider Mappings

Add these inside the `entryProvider` block. Key pattern: **screens receive navigation callbacks as parameters, not direct backStack access**.

### Core screens:

```kotlin
entry<Home> {
    HomeScreen(
        homeViewModel = homeViewModel,
        snackbarHostState = snackbarHostState,
        listState = listState,
        onImportExportClick = { backStack.add(ImportExport) },
        onSearchClick = { backStack.add(Search) },
        navigateToReaderMode = { feedItemUrlInfo ->
            backStack.add(feedItemUrlInfo.toReaderMode())
        },
        onAccountsClick = { backStack.add(Accounts) },
        onFeedSuggestionsClick = { backStack.add(FeedSuggestions) },
    )
}

entry<Search> {
    SearchScreen(
        navigateBack = { backStack.removeLastOrNull() },
        navigateToReaderMode = { urlInfo ->
            backStack.add(urlInfo.toReaderMode())
        },
        navigateToEditFeed = { feedSource ->
            backStack.add(feedSource.toEditFeed())
        },
    )
}

entry<ReaderMode> { route ->
    val feedItemUrlInfo = FeedItemUrlInfo(
        id = route.id,
        url = route.url,
        title = route.title,
    )
    ReaderModeScreen(
        feedItemUrlInfo = feedItemUrlInfo,
        navigateBack = { backStack.removeLastOrNull() },
    )
}

entry<Accounts> {
    AccountsScreen(
        navigateBack = { backStack.removeLastOrNull() },
        navigateToDropboxSync = { backStack.add(DropboxSync) },
        navigateToGoogleDriveSync = { backStack.add(GoogleDriveSync) },
        navigateToICloudSync = { backStack.add(ICloudSync) },
        navigateToFreshRssSync = { backStack.add(FreshRssSync) },
        navigateToMinifluxSync = { backStack.add(MinifluxSync) },
        navigateToBazquxSync = { backStack.add(BazquxSync) },
        navigateToFeedbinSync = { backStack.add(FeedbinSync) },
    )
}

entry<FeedSuggestions> {
    FeedSuggestionsScreen(
        navigateBack = { backStack.removeLastOrNull() },
    )
}

entry<AddFeed> {
    AddFeedFullScreen(
        onFeedAdded = {
            homeViewModel.getNewFeeds()
            backStack.removeLastOrNull()
        },
        navigateBack = { backStack.removeLastOrNull() },
    )
}

entry<ImportExport> {
    val composeWindow = LocalComposeWindow.current
    ImportExportScreen(
        composeWindow = composeWindow,
        triggerFeedFetch = { homeViewModel.getNewFeeds() },
        navigateBack = { backStack.removeLastOrNull() },
    )
}

entry<EditFeed> { route ->
    val feedSource = route.toFeedSource()
    EditFeedScreen(
        feedSource = feedSource,
        navigateBack = { backStack.removeLastOrNull() },
    )
}
```

### Sync screens (simple pattern):

```kotlin
entry<DropboxSync> {
    DropboxSyncScreen(navigateBack = { backStack.removeLastOrNull() })
}
entry<GoogleDriveSync> {
    GoogleDriveSyncScreen(navigateBack = { backStack.removeLastOrNull() })
}
entry<ICloudSync> {
    ICloudSyncScreen(navigateBack = { backStack.removeLastOrNull() })
}
entry<FreshRssSync> {
    FreshRssSyncScreen(navigateBack = { backStack.removeLastOrNull() })
}
entry<MinifluxSync> {
    MinifluxSyncScreen(navigateBack = { backStack.removeLastOrNull() })
}
entry<BazquxSync> {
    BazquxSyncScreen(navigateBack = { backStack.removeLastOrNull() })
}
entry<FeedbinSync> {
    FeedbinSyncScreen(navigateBack = { backStack.removeLastOrNull() })
}
```

### Settings screens:

```kotlin
entry<BlockedWords> {
    BlockedWordsScreen(navigateBack = { backStack.removeLastOrNull() })
}

entry<FeedSourceList> {
    FeedSourceListScreen(
        onAddFeedClick = { backStack.add(AddFeed) },
        navigateBack = { backStack.removeLastOrNull() },
        onEditFeedClick = { feedSource ->
            backStack.add(feedSource.toEditFeed())
        },
    )
}
```

---

## 7. Convert Screen Files

For each screen file in `desktopApp/src/jvmMain/kotlin/com/prof18/feedflow/desktop/`:

### Remove:
```kotlin
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

internal class SomeScreen(...) : Screen {
    override val key: String = generateUniqueKey()

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        // ...
    }
}
```

### Replace with:
```kotlin
@Composable
internal fun SomeScreen(
    navigateBack: () -> Unit,
    // other navigation callbacks as needed
    // ... other parameters
) {
    // Screen content here - no Screen interface, no navigator
}
```

### Files to convert:
- `search/SearchScreen.desktop.kt`
- `accounts/AccountsScreen.desktop.kt`
- `feedsuggestions/FeedSuggestionsScreen.desktop.kt`
- `addfeed/AddFeedFullScreen.kt`
- `importexport/ImportExportScreen.desktop.kt`
- `editfeed/EditFeedScreen.desktop.kt`
- `reaadermode/ReaderModeScreen.desktop.kt`
- All sync screens in `accounts/` subdirectories
- `settings/blocked/BlockedWordsScreen.desktop.kt`
- `home/HomeScreen.desktop.kt` (HomeScreenContainer)
- `feedsourcelist/FeedSourceListScreen.desktop.kt`

---

## 8. Update MenuBar

### File: `desktopApp/src/jvmMain/kotlin/com/prof18/feedflow/desktop/home/menubar/MenuBar.kt`

**Remove:**
```kotlin
val navigator = LocalNavigator.currentOrThrow
```

**Update signature:**
```kotlin
@Composable
fun FrameWindowScope.FeedFlowMenuBar(
    state: MenuBarState,
    actions: MenuBarActions,
    backStack: NavBackStack<NavKey>,  // ADD THIS
) {
    // Replace all navigator.push() calls with:
    // backStack.add(...)
}
```

---

## 9. Cleanup

After migration is complete:

1. **Delete files:**
    - `desktopApp/src/jvmMain/kotlin/com/prof18/feedflow/desktop/DesktopViewModel.kt`
    - `desktopApp/src/jvmMain/kotlin/com/prof18/feedflow/desktop/utils/ScreenExtensions.kt` (if exists)

2. **Remove all Voyager imports** from codebase:
   ```bash
   grep -r "cafe.adriel.voyager" desktopApp/src/
   ```

3. **Remove Voyager dependencies** from `build.gradle.kts` and `libs.versions.toml`

---

## 10. Testing Checklist

### Navigation flows:
- [ ] Home → Search → back
- [ ] Home → Reader Mode → back
- [ ] Home → Accounts → (all sync screens) → back
- [ ] Home → Feed Suggestions → back
- [ ] Home → Add Feed → (success/cancel) → back
- [ ] Home → Import/Export (test file dialog) → back
- [ ] MenuBar → Feed List → Edit Feed → back
- [ ] Deep navigation stack (multiple screens) → pop all

### Transitions:
- [ ] Reduce motion OFF - verify scale animations
- [ ] Reduce motion ON - verify no animations
- [ ] Forward navigation transition
- [ ] Back navigation transition

### ViewModel lifecycle:
- [ ] Navigate to screen → back → verify ViewModel disposed
- [ ] Window resize → verify state preserved
- [ ] Multiple screens in stack → pop all → verify all ViewModels disposed

### Edge cases:
- [ ] Rapid navigation (quick clicks) - no crashes
- [ ] Back from Home (root) - nothing happens
- [ ] Multiple EditFeed instances with different parameters
- [ ] ImportExport file dialog cancel - no crash
- [ ] ComposeWindow access works in ImportExport

### Build verification:
```bash
./gradlew desktopApp:build -q --console=plain
./gradlew desktopApp:run
```

---

## Key Differences: Voyager vs Navigation 3

| Aspect | Voyager (Old) | Navigation 3 (New) |
|--------|--------------|-------------------|
| **Route Definition** | `Screen` interface implementations | `@Serializable` `NavKey` classes |
| **Navigation API** | `navigator.push()` / `pop()` | `backStack.add()` / `removeLastOrNull()` |
| **Navigator Access** | `LocalNavigator.currentOrThrow` | Pass via callbacks (no direct access in screens) |
| **Type Safety** | Instance-based (any object) | Compile-time type checking via serialization |
| **Transitions** | `ScaleTransition` wrapper | `transitionSpec` / `popTransitionSpec` on `NavDisplay` |
| **ViewModel Lifecycle** | `ScreenLifecycleStore` | `viewModelStoreOwner()` (automatic) |
| **Unique Keys** | Manual `generateUniqueKey()` | Automatic (different parameter values = different entries) |

---

## Critical Files

**To create:**
- `desktopApp/src/jvmMain/kotlin/com/prof18/feedflow/desktop/Screen.kt`
- `desktopApp/src/jvmMain/kotlin/com/prof18/feedflow/desktop/utils/CompositionLocals.kt`
- `desktopApp/src/jvmMain/kotlin/com/prof18/feedflow/desktop/utils/ViewModelUtils.kt`

**To modify:**
- `desktopApp/src/jvmMain/kotlin/com/prof18/feedflow/desktop/main/MainWindow.kt`
- `desktopApp/src/jvmMain/kotlin/com/prof18/feedflow/desktop/home/menubar/MenuBar.kt`
- `desktopApp/build.gradle.kts`
- `gradle/libs.versions.toml`
- All screen files in `desktopApp/src/jvmMain/kotlin/com/prof18/feedflow/desktop/` (convert from Screen interface to composable functions)

**To delete:**
- `desktopApp/src/jvmMain/kotlin/com/prof18/feedflow/desktop/DesktopViewModel.kt`
- `desktopApp/src/jvmMain/kotlin/com/prof18/feedflow/desktop/utils/ScreenExtensions.kt` (if exists)
