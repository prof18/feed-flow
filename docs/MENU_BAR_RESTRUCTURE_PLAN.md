# Menu Bar Restructure Implementation Plan

## Overview

This plan restructures the FeedFlow desktop menu bar for improved UX. The main changes are:
1. Create a new **View** menu for appearance/display settings
2. Rename **Behaviour** menu to **Settings** for clarity
3. Reorganize items into logical groups following platform conventions
4. Fix mnemonic conflicts
5. Move items to their conventional locations

## Files to Modify

### Primary File
- `desktopApp/src/jvmMain/kotlin/com/prof18/feedflow/desktop/home/MenuBar.kt`

### String Resources (for new menu name)
- `i18n/src/commonMain/composeResources/locale/values/strings.xml` (add "View" menu string)
- Run `.scripts/refresh-translations.sh` after adding the string

---

## Implementation Steps

### Step 1: Add New String Resource for "View" Menu

In `i18n/src/commonMain/composeResources/locale/values/strings.xml`, add:

```xml
<string name="menu_view">View</string>
```

Then run:
```bash
.scripts/refresh-translations.sh
```

This will generate the Kotlin accessor for the new string.

---

### Step 2: Restructure MenuBar.kt

Replace the entire `FeedFlowMenuBar` composable function with the new structure below.

#### 2.1 Update Data Classes

Keep `MenuBarActions` and `MenuBarSettings` as-is, but ensure all required callbacks are present.

#### 2.2 New Menu Structure

The new menu order should be:
1. **File** (mnemonic: 'F') - Data operations and sync
2. **Feed** (mnemonic: 'E') - Feed management (changed from no mnemonic)
3. **View** (mnemonic: 'V') - NEW - Appearance and display settings
4. **Settings** (mnemonic: 'S') - Preferences and configuration (renamed from Behaviour)
5. **Help** (mnemonic: 'H') - Support and about (fixed from 'B')

#### 2.3 Detailed Menu Implementation

```kotlin
@Composable
fun FrameWindowScope.FeedFlowMenuBar(
    state: MenuBarState,
    actions: MenuBarActions,
    settings: MenuBarSettings,
) {
    val isMacOS = getDesktopOS().isMacOs()
    val navigator = LocalNavigator.currentOrThrow

    MenuBar {
        // =====================
        // FILE MENU (mnemonic: F)
        // =====================
        Menu(LocalFeedFlowStrings.current.fileMenu, mnemonic = 'F') {
            // --- Refresh Actions ---
            Item(
                text = LocalFeedFlowStrings.current.refreshFeeds,
                onClick = { actions.onRefreshClick() },
                shortcut = if (isMacOS) {
                    KeyShortcut(Key.R, meta = true)
                } else {
                    KeyShortcut(Key.F5)
                },
            )

            Item(
                text = LocalFeedFlowStrings.current.forceFeedRefresh,
                onClick = { actions.onForceRefreshClick() },
                shortcut = if (isMacOS) {
                    KeyShortcut(Key.R, meta = true, shift = true)
                } else {
                    KeyShortcut(Key.F5, shift = true)
                },
            )

            // --- Sync (conditional) ---
            if (state.isSyncUploadRequired) {
                Separator()
                Item(
                    text = LocalFeedFlowStrings.current.triggerFeedSync,
                    onClick = { actions.onBackupClick() },
                    shortcut = if (isMacOS) {
                        KeyShortcut(Key.S, meta = true)
                    } else {
                        KeyShortcut(Key.S, ctrl = true)
                    },
                )
            }

            Separator()

            // --- Article Management ---
            Item(
                text = LocalFeedFlowStrings.current.markAllReadButton,
                onClick = { actions.onMarkAllReadClick() },
                shortcut = if (isMacOS) {
                    KeyShortcut(Key.A, meta = true, shift = true)
                } else {
                    KeyShortcut(Key.A, ctrl = true, shift = true)
                },
            )

            Item(
                text = LocalFeedFlowStrings.current.clearOldArticlesButton,
                onClick = { actions.onClearOldFeedClick() },
                shortcut = if (isMacOS) {
                    KeyShortcut(Key.D, meta = true, shift = true)
                } else {
                    KeyShortcut(Key.D, ctrl = true, shift = true)
                },
            )

            Separator()

            // --- Import/Export (moved from Feed menu) ---
            Item(
                text = LocalFeedFlowStrings.current.importExportOpml,
                onClick = actions.onImportExportClick,
                shortcut = if (isMacOS) {
                    KeyShortcut(Key.I, meta = true)
                } else {
                    KeyShortcut(Key.I, ctrl = true)
                },
            )

            // --- Debug Menu (conditional) ---
            DebugMenu(
                showDebugMenu = state.showDebugMenu,
                deleteFeeds = actions.deleteFeeds,
            )
        }

        // =====================
        // FEED MENU (mnemonic: E)
        // =====================
        Menu(LocalFeedFlowStrings.current.settingsTitleFeed, mnemonic = 'E') {
            // --- Edit Feed (conditional - only when source selected) ---
            if (state.feedFilter is FeedFilter.Source) {
                Item(
                    text = LocalFeedFlowStrings.current.editFeed,
                    onClick = {
                        navigator.push(EditFeedScreen(state.feedFilter.feedSource))
                    },
                    shortcut = if (isMacOS) {
                        KeyShortcut(Key.E, meta = true)
                    } else {
                        KeyShortcut(Key.E, ctrl = true)
                    },
                )
                Separator()
            }

            // --- Feed Management ---
            Item(
                text = LocalFeedFlowStrings.current.feedsTitle,
                onClick = { navigator.push(FeedSourceListScreen()) },
                shortcut = if (isMacOS) {
                    KeyShortcut(Key.L, meta = true)
                } else {
                    KeyShortcut(Key.L, ctrl = true)
                },
            )

            Separator()

            // --- Filtering ---
            Item(
                text = LocalFeedFlowStrings.current.settingsBlockedWords,
                onClick = { navigator.push(BlockedWordsScreen()) },
            )
        }

        // =====================
        // VIEW MENU (mnemonic: V) - NEW
        // =====================
        Menu(LocalFeedFlowStrings.current.menuView, mnemonic = 'V') {
            // --- Theme Submenu ---
            Menu(LocalFeedFlowStrings.current.settingsTheme) {
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsThemeSystem,
                    selected = state.settingsState.themeMode == ThemeMode.SYSTEM,
                    onClick = { settings.onThemeModeSelected(ThemeMode.SYSTEM) },
                )
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsThemeLight,
                    selected = state.settingsState.themeMode == ThemeMode.LIGHT,
                    onClick = { settings.onThemeModeSelected(ThemeMode.LIGHT) },
                )
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsThemeDark,
                    selected = state.settingsState.themeMode == ThemeMode.DARK,
                    onClick = { settings.onThemeModeSelected(ThemeMode.DARK) },
                )
            }

            // --- Feed List Appearance (moved from Feed menu) ---
            Item(
                text = LocalFeedFlowStrings.current.feedListAppearance,
                onClick = actions.onFeedFontScaleClick,
            )

            Separator()

            // --- Display Options ---
            CheckboxItem(
                text = LocalFeedFlowStrings.current.settingsToggleShowReadArticles,
                checked = state.settingsState.isShowReadItemsEnabled,
                onCheckedChange = settings.setShowReadItem,
            )

            // --- Feed Order Submenu ---
            Menu(LocalFeedFlowStrings.current.settingsFeedOrderTitle) {
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsFeedOrderNewestFirst,
                    selected = state.settingsState.feedOrder == FeedOrder.NEWEST_FIRST,
                    onClick = { settings.onFeedOrderSelected(FeedOrder.NEWEST_FIRST) },
                )
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsFeedOrderOldestFirst,
                    selected = state.settingsState.feedOrder == FeedOrder.OLDEST_FIRST,
                    onClick = { settings.onFeedOrderSelected(FeedOrder.OLDEST_FIRST) },
                )
            }
        }

        // =====================
        // SETTINGS MENU (mnemonic: S) - Renamed from Behaviour
        // =====================
        Menu(LocalFeedFlowStrings.current.settingsBehaviourTitle, mnemonic = 'S') {
            // --- Accounts/Sync (moved from Feed menu) ---
            Item(
                text = LocalFeedFlowStrings.current.settingsAccounts,
                onClick = { navigator.push(AccountsScreen()) },
                shortcut = if (isMacOS) {
                    KeyShortcut(Key.Comma, meta = true)
                } else {
                    KeyShortcut(Key.Comma, ctrl = true)
                },
            )

            Separator()

            // --- Reader Mode Settings (grouped) ---
            CheckboxItem(
                text = LocalFeedFlowStrings.current.settingsReaderMode,
                checked = state.settingsState.isReaderModeEnabled,
                onCheckedChange = settings.setReaderMode,
            )

            CheckboxItem(
                text = LocalFeedFlowStrings.current.settingsSaveReaderModeContent,
                checked = state.settingsState.isSaveReaderModeContentEnabled,
                onCheckedChange = settings.setSaveReaderModeContent,
            )

            CheckboxItem(
                text = LocalFeedFlowStrings.current.settingsPrefetchArticleContent,
                checked = state.settingsState.isPrefetchArticleContentEnabled,
                onCheckedChange = settings.setPrefetchArticleContent,
            )

            Separator()

            // --- Reading Behavior ---
            CheckboxItem(
                text = LocalFeedFlowStrings.current.toggleMarkReadWhenScrolling,
                checked = state.settingsState.isMarkReadWhenScrollingEnabled,
                onCheckedChange = settings.setMarkReadWhenScrolling,
            )

            // --- Auto Delete Submenu ---
            Menu(LocalFeedFlowStrings.current.settingsAutoDelete) {
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsAutoDeletePeriodDisabled,
                    selected = state.settingsState.autoDeletePeriod == AutoDeletePeriod.DISABLED,
                    onClick = { settings.onAutoDeletePeriodSelected(AutoDeletePeriod.DISABLED) },
                )
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsAutoDeletePeriodOneDay,
                    selected = state.settingsState.autoDeletePeriod == AutoDeletePeriod.ONE_DAY,
                    onClick = { settings.onAutoDeletePeriodSelected(AutoDeletePeriod.ONE_DAY) },
                )
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsAutoDeletePeriodOneWeek,
                    selected = state.settingsState.autoDeletePeriod == AutoDeletePeriod.ONE_WEEK,
                    onClick = { settings.onAutoDeletePeriodSelected(AutoDeletePeriod.ONE_WEEK) },
                )
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsAutoDeletePeriodTwoWeeks,
                    selected = state.settingsState.autoDeletePeriod == AutoDeletePeriod.TWO_WEEKS,
                    onClick = { settings.onAutoDeletePeriodSelected(AutoDeletePeriod.TWO_WEEKS) },
                )
                RadioButtonItem(
                    text = LocalFeedFlowStrings.current.settingsAutoDeletePeriodOneMonth,
                    selected = state.settingsState.autoDeletePeriod == AutoDeletePeriod.ONE_MONTH,
                    onClick = { settings.onAutoDeletePeriodSelected(AutoDeletePeriod.ONE_MONTH) },
                )
            }

            Separator()

            // --- Data Management ---
            Item(
                text = LocalFeedFlowStrings.current.settingsClearDownloadedArticles,
                onClick = settings.onClearDownloadedArticles,
            )

            Item(
                text = LocalFeedFlowStrings.current.settingsClearImageCache,
                onClick = settings.onClearImageCache,
            )
        }

        // =====================
        // HELP MENU (mnemonic: H) - Fixed from 'B'
        // =====================
        Menu(LocalFeedFlowStrings.current.settingsHelpTitle, mnemonic = 'H') {
            // --- FAQ (conditional) ---
            if (FeatureFlags.ENABLE_FAQ) {
                Item(
                    text = LocalFeedFlowStrings.current.aboutMenuFaq,
                    onClick = {
                        runCatching {
                            val languageCode = java.util.Locale.getDefault().language
                            val faqUrl = "https://feedflow.dev/$languageCode/faq"
                            Desktop.getDesktop().browse(URI(faqUrl))
                        }
                    },
                )
            }

            Item(
                text = LocalFeedFlowStrings.current.reportIssueButton,
                onClick = actions.onBugReportClick,
            )

            Separator()

            CheckboxItem(
                text = LocalFeedFlowStrings.current.settingsCrashReporting,
                checked = state.settingsState.isCrashReportingEnabled,
                onCheckedChange = settings.setCrashReportingEnabled,
            )

            Separator()

            // --- Support (now on all platforms) ---
            Item(
                text = LocalFeedFlowStrings.current.supportTheProject,
                onClick = {
                    runCatching {
                        Desktop.getDesktop().browse(URI("https://www.paypal.me/MarcoGomiero"))
                    }
                },
            )

            // --- About (Windows/Linux only; macOS uses app menu) ---
            if (!isMacOS) {
                Separator()
                Item(
                    text = LocalFeedFlowStrings.current.aboutButton,
                    onClick = actions.onAboutClick,
                )
            }
        }
    }
}
```

---

### Step 3: Move Navigator Declaration

Note that in the new structure, `navigator` is used across multiple menus (Feed and Settings), so it should be declared at the top of the `FeedFlowMenuBar` function, before the `MenuBar` block:

```kotlin
@Composable
fun FrameWindowScope.FeedFlowMenuBar(
    state: MenuBarState,
    actions: MenuBarActions,
    settings: MenuBarSettings,
) {
    val isMacOS = getDesktopOS().isMacOs()
    val navigator = LocalNavigator.currentOrThrow  // <-- Move here from inside Feed menu

    MenuBar {
        // ... menus
    }
}
```

---

### Step 4: Handle macOS About Menu

On macOS, the "About" menu item should appear in the application menu (the menu with the app name), not in Help. This is typically handled automatically by the Compose Desktop framework when you don't include it elsewhere on macOS.

The implementation above already handles this with:
```kotlin
if (!isMacOS) {
    Item(
        text = LocalFeedFlowStrings.current.aboutButton,
        onClick = actions.onAboutClick,
    )
}
```

If macOS doesn't automatically show About in the app menu, you may need to configure the native macOS menu separately in the window configuration.

---

## Summary of Changes

### Items Moved

| Item | From | To |
|------|------|-----|
| Import/Export OPML | Feed | File |
| Theme submenu | File | View (new) |
| Feed List Appearance | Feed | View (new) |
| Show Read Articles | Behaviour | View (new) |
| Feed Order submenu | Behaviour | View (new) |
| Accounts | Feed | Settings |
| Clear Downloaded Articles | File | Settings |
| Clear Image Cache | File | Settings |
| Support the Project | File (Linux only) | Help (all platforms) |
| About | File | Help (Windows/Linux only) |

### Mnemonics Fixed

| Menu | Old | New |
|------|-----|-----|
| Feed | (none) | E |
| Behaviour/Settings | B | S |
| Help | B | H |

### New Menu Created

- **View** menu (mnemonic: V) for all appearance and display settings

---

## Testing Checklist

After implementation, verify:

1. [ ] All keyboard shortcuts work correctly on macOS
2. [ ] All keyboard shortcuts work correctly on Windows/Linux
3. [ ] No mnemonic conflicts (each menu has unique mnemonic)
4. [ ] Theme switching works from View menu
5. [ ] Feed List Appearance dialog opens from View menu
6. [ ] Show Read Articles toggle works from View menu
7. [ ] Feed Order changes work from View menu
8. [ ] Import/Export OPML works from File menu
9. [ ] Accounts screen opens from Settings menu
10. [ ] Clear cache options work from Settings menu
11. [ ] Support the Project link works on all platforms
12. [ ] About dialog shows on Windows/Linux (from Help menu)
13. [ ] About dialog shows on macOS (from app menu, if configured)
14. [ ] FAQ link opens browser (if feature flag enabled)
15. [ ] Debug menu appears only in debug builds
16. [ ] Trigger Feed Sync appears only when sync is configured

---

## Optional Future Enhancements

1. **Add "Add Feed" item** to Feed menu with `Cmd+N`/`Ctrl+N` shortcut
2. **Add "Preferences" item** to Settings menu that opens a dedicated preferences window
3. **Group reader mode options** into a submenu called "Reader Mode Settings"
4. **Add "Keyboard Shortcuts" item** to Help menu showing all available shortcuts
5. **Add "Check for Updates" item** to Help menu (if auto-update is implemented)
