package com.prof18.feedflow.desktop.home.menubar

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.MenuBarScope
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.shared.presentation.model.MenuBarSettingsState
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun MenuBarScope.BehaviorMenu(
    isMacOS: Boolean,
    settingsState: MenuBarSettingsState,
    callbacks: BehaviorMenuCallbacks,
) {
    Menu(LocalFeedFlowStrings.current.settingsBehaviourTitle, mnemonic = 'S') {
        Item(
            text = LocalFeedFlowStrings.current.settingsAccounts,
            onClick = callbacks.onAccountsClick,
            shortcut = if (isMacOS) {
                KeyShortcut(Key.Comma, meta = true)
            } else {
                KeyShortcut(Key.Comma, ctrl = true)
            },
        )

        Separator()

        CheckboxItem(
            text = LocalFeedFlowStrings.current.settingsReaderMode,
            checked = settingsState.isReaderModeEnabled,
            onCheckedChange = callbacks.onReaderModeToggled,
        )

        CheckboxItem(
            text = LocalFeedFlowStrings.current.settingsSaveReaderModeContent,
            checked = settingsState.isSaveReaderModeContentEnabled,
            onCheckedChange = callbacks.onSaveReaderModeContentToggled,
        )

        CheckboxItem(
            text = LocalFeedFlowStrings.current.settingsPrefetchArticleContent,
            checked = settingsState.isPrefetchArticleContentEnabled,
            onCheckedChange = callbacks.onPrefetchToggle,
        )

        CheckboxItem(
            text = LocalFeedFlowStrings.current.settingsRefreshFeedsOnLaunch,
            checked = settingsState.isRefreshFeedsOnLaunchEnabled,
            onCheckedChange = callbacks.onRefreshFeedsOnLaunchToggled,
        )

        Separator()

        CheckboxItem(
            text = LocalFeedFlowStrings.current.toggleMarkReadWhenScrolling,
            checked = settingsState.isMarkReadWhenScrollingEnabled,
            onCheckedChange = callbacks.onMarkReadWhenScrollingToggled,
        )

        Menu(LocalFeedFlowStrings.current.settingsAutoDelete) {
            RadioButtonItem(
                text = LocalFeedFlowStrings.current.settingsAutoDeletePeriodDisabled,
                selected = settingsState.autoDeletePeriod == AutoDeletePeriod.DISABLED,
                onClick = { callbacks.onAutoDeletePeriodSelected(AutoDeletePeriod.DISABLED) },
            )
            RadioButtonItem(
                text = LocalFeedFlowStrings.current.settingsAutoDeletePeriodOneDay,
                selected = settingsState.autoDeletePeriod == AutoDeletePeriod.ONE_DAY,
                onClick = { callbacks.onAutoDeletePeriodSelected(AutoDeletePeriod.ONE_DAY) },
            )
            RadioButtonItem(
                text = LocalFeedFlowStrings.current.settingsAutoDeletePeriodOneWeek,
                selected = settingsState.autoDeletePeriod == AutoDeletePeriod.ONE_WEEK,
                onClick = { callbacks.onAutoDeletePeriodSelected(AutoDeletePeriod.ONE_WEEK) },
            )
            RadioButtonItem(
                text = LocalFeedFlowStrings.current.settingsAutoDeletePeriodTwoWeeks,
                selected = settingsState.autoDeletePeriod == AutoDeletePeriod.TWO_WEEKS,
                onClick = { callbacks.onAutoDeletePeriodSelected(AutoDeletePeriod.TWO_WEEKS) },
            )
            RadioButtonItem(
                text = LocalFeedFlowStrings.current.settingsAutoDeletePeriodOneMonth,
                selected = settingsState.autoDeletePeriod == AutoDeletePeriod.ONE_MONTH,
                onClick = { callbacks.onAutoDeletePeriodSelected(AutoDeletePeriod.ONE_MONTH) },
            )
        }

        Separator()

        Item(
            text = LocalFeedFlowStrings.current.settingsClearDownloadedArticles,
            onClick = callbacks.onClearDownloadedArticles,
        )

        Item(
            text = LocalFeedFlowStrings.current.settingsClearImageCache,
            onClick = callbacks.onClearImageCache,
        )
    }
}

internal data class BehaviorMenuCallbacks(
    val onAccountsClick: () -> Unit,
    val onReaderModeToggled: (Boolean) -> Unit,
    val onSaveReaderModeContentToggled: (Boolean) -> Unit,
    val onPrefetchToggle: (Boolean) -> Unit,
    val onRefreshFeedsOnLaunchToggled: (Boolean) -> Unit,
    val onMarkReadWhenScrollingToggled: (Boolean) -> Unit,
    val onAutoDeletePeriodSelected: (AutoDeletePeriod) -> Unit,
    val onClearDownloadedArticles: () -> Unit,
    val onClearImageCache: () -> Unit,
)
