package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.shared.ui.settings.ConfirmationDialogConfig
import com.prof18.feedflow.shared.ui.settings.SettingSwitchItem
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun ReadingPane(
    isReaderModeEnabled: Boolean,
    isSaveReaderModeContentEnabled: Boolean,
    isPrefetchArticleContentEnabled: Boolean,
    isMarkReadWhenScrollingEnabled: Boolean,
    isShowReadItemsEnabled: Boolean,
    isHideReadItemsEnabled: Boolean,
    onReaderModeToggled: (Boolean) -> Unit,
    onSaveReaderModeContentToggled: (Boolean) -> Unit,
    onPrefetchToggled: (Boolean) -> Unit,
    onMarkReadWhenScrollingToggled: (Boolean) -> Unit,
    onShowReadItemsToggled: (Boolean) -> Unit,
    onHideReadItemsToggled: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingSwitchItem(
            title = LocalFeedFlowStrings.current.settingsReaderMode,
            isChecked = isReaderModeEnabled,
            onCheckedChange = onReaderModeToggled,
        )

        SettingSwitchItem(
            title = LocalFeedFlowStrings.current.settingsSaveReaderModeContent,
            isChecked = isSaveReaderModeContentEnabled,
            onCheckedChange = onSaveReaderModeContentToggled,
        )

        SettingSwitchItem(
            title = LocalFeedFlowStrings.current.settingsPrefetchArticleContent,
            isChecked = isPrefetchArticleContentEnabled,
            onCheckedChange = onPrefetchToggled,
            confirmationDialog = ConfirmationDialogConfig(
                title = LocalFeedFlowStrings.current.settingsPrefetchArticleContent,
                message = LocalFeedFlowStrings.current.settingsPrefetchArticleContentWarning,
            ),
        )

        SettingSwitchItem(
            title = LocalFeedFlowStrings.current.toggleMarkReadWhenScrolling,
            isChecked = isMarkReadWhenScrollingEnabled,
            onCheckedChange = onMarkReadWhenScrollingToggled,
        )

        SettingSwitchItem(
            title = LocalFeedFlowStrings.current.settingsToggleShowReadArticles,
            isChecked = isShowReadItemsEnabled,
            onCheckedChange = onShowReadItemsToggled,
        )

        SettingSwitchItem(
            title = LocalFeedFlowStrings.current.settingsHideReadItems,
            isChecked = isHideReadItemsEnabled,
            onCheckedChange = onHideReadItemsToggled,
        )
    }
}

@Preview
@Composable
private fun ReadingPanePreview() {
    FeedFlowTheme {
        ReadingPane(
            isReaderModeEnabled = false,
            isSaveReaderModeContentEnabled = false,
            isPrefetchArticleContentEnabled = false,
            isMarkReadWhenScrollingEnabled = false,
            isShowReadItemsEnabled = false,
            isHideReadItemsEnabled = false,
            onReaderModeToggled = {},
            onSaveReaderModeContentToggled = {},
            onPrefetchToggled = {},
            onMarkReadWhenScrollingToggled = {},
            onShowReadItemsToggled = {},
            onHideReadItemsToggled = {},
        )
    }
}
